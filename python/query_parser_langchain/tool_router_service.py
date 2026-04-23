from __future__ import annotations

import uuid

from .models import TaskType
from .tool_router_models import (
    ExecutionPolicy,
    NormalizedParams,
    PlanType,
    SessionContext,
    ToolName,
    ToolPlan,
    ToolRegistry,
    ToolRouterRequest,
    ToolStep,
)


class LangGraphToolRouter:
    def route_request(self, request: ToolRouterRequest) -> ToolPlan:
        if self._should_clarify(request):
            return self._clarification(request)
        task_type = request.parsed_intent.task_type
        if task_type == TaskType.FAQ_QUERY:
            return self._route_faq(request)
        if task_type == TaskType.MIXED_SEARCH_KNOWLEDGE or request.parsed_intent.need_explanation:
            return self._route_search_then_knowledge(request)
        if task_type in {TaskType.REALTIME_QUERY, TaskType.MIXED_SEARCH_REALTIME} or request.parsed_intent.need_realtime:
            return self._route_search_then_realtime(request)
        return self._route_search_only(request)

    def _should_clarify(self, request: ToolRouterRequest) -> bool:
        params = request.normalized_params
        policy = request.execution_policy
        if policy.clarify_on_missing_required_slots and (params.missing_required_slots or params.validation_errors):
            return True
        if policy.clarify_on_low_intent_confidence and request.parsed_intent.intent_confidence < policy.low_intent_confidence_threshold:
            if request.parsed_intent.task_type == TaskType.CHITCHAT:
                return False
            return True
        if request.parsed_intent.task_type == TaskType.STORE_SEARCH and not params.execution_ready and not params.entity_type:
            return True
        return False

    def _clarification(self, request: ToolRouterRequest) -> ToolPlan:
        params = request.normalized_params
        pieces = []
        if params.missing_required_slots:
            pieces.append("还缺少这些信息：" + "、".join(params.missing_required_slots))
        if params.validation_errors:
            pieces.append("这些条件还需要调整：" + "、".join(params.validation_errors))
        if not pieces:
            pieces.append("还需要补充一些信息后我才能继续执行。")
        return ToolPlan(
            plan_id=self._plan_id(),
            plan_type=PlanType.CLARIFICATION_REQUIRED,
            execution_mode="single",
            requires_clarification=True,
            clarification_prompt="；".join(pieces),
            routing_reason="clarification_required",
            max_parallel_tools=1,
        )

    def _route_faq(self, request: ToolRouterRequest) -> ToolPlan:
        policy = request.execution_policy
        return ToolPlan(
            plan_id=self._plan_id(),
            plan_type=PlanType.SINGLE_TOOL,
            execution_mode="single",
            steps=[self._step("knowledge", ToolName.KNOWLEDGE_RETRIEVAL, "faq_direct", "parsed_intent", [], "retrieved_docs", False, policy.knowledge_timeout_ms, request)],
            routing_reason="faq_query | knowledge_retrieval_only",
            max_parallel_tools=policy.max_parallel_tools,
        )

    def _route_search_only(self, request: ToolRouterRequest) -> ToolPlan:
        policy = request.execution_policy
        return ToolPlan(
            plan_id=self._plan_id(),
            plan_type=PlanType.SINGLE_TOOL,
            execution_mode="single",
            steps=[self._step("search", ToolName.STRUCTURED_SEARCH, "search_only", "normalized_params", [], "search_results", False, policy.structured_search_timeout_ms, request)],
            routing_reason="search_only | structured_search_only | need_recommendation=%s" % request.parsed_intent.need_recommendation,
            max_parallel_tools=policy.max_parallel_tools,
        )

    def _route_search_then_knowledge(self, request: ToolRouterRequest) -> ToolPlan:
        policy = request.execution_policy
        fallback = ToolPlan(
            plan_type=PlanType.SINGLE_TOOL,
            execution_mode="single",
            steps=[self._step("search", ToolName.STRUCTURED_SEARCH, "fallback_search_only", "normalized_params", [], "search_results", False, policy.structured_search_timeout_ms, request)],
            routing_reason="knowledge_unavailable_fallback_to_search",
            max_parallel_tools=1,
        )
        return ToolPlan(
            plan_id=self._plan_id(),
            plan_type=PlanType.SEARCH_THEN_KNOWLEDGE,
            execution_mode="serial",
            steps=[
                self._step("search", ToolName.STRUCTURED_SEARCH, "search_primary", "normalized_params", [], "search_results", False, policy.structured_search_timeout_ms, request),
                self._step("knowledge", ToolName.KNOWLEDGE_RETRIEVAL, "knowledge_enrichment", "search_results", ["search"], "retrieved_docs", True, policy.knowledge_timeout_ms, request),
            ],
            fallback_plan=fallback,
            routing_reason="search_then_knowledge | need_explanation=true | knowledge_optional=%s" % policy.knowledge_optional_for_search,
            max_parallel_tools=policy.max_parallel_tools,
        )

    def _route_search_then_realtime(self, request: ToolRouterRequest) -> ToolPlan:
        policy = request.execution_policy
        fallback = ToolPlan(
            plan_type=PlanType.SINGLE_TOOL,
            execution_mode="single",
            steps=[self._step("search", ToolName.STRUCTURED_SEARCH, "fallback_search_only", "normalized_params", [], "search_results", False, policy.structured_search_timeout_ms, request)],
            routing_reason="realtime_timeout_or_unavailable",
            max_parallel_tools=1,
        )
        return ToolPlan(
            plan_id=self._plan_id(),
            plan_type=PlanType.SEARCH_THEN_REALTIME,
            execution_mode="serial",
            steps=[
                self._step("search", ToolName.STRUCTURED_SEARCH, "search_primary", "normalized_params", [], "search_results", False, policy.structured_search_timeout_ms, request),
                self._step("realtime", ToolName.REALTIME_QUERY, "realtime_confirmation", "search_results", ["search"], "realtime_results", True, policy.realtime_timeout_ms, request),
            ],
            fallback_plan=fallback,
            routing_reason="search_then_realtime | need_realtime=true | allow_partial_answer=%s" % policy.allow_partial_answer,
            max_parallel_tools=policy.max_parallel_tools,
        )

    def _step(self, step_id: str, tool_name: ToolName, purpose: str, input_ref: str, depends_on: list[str], output_key: str, optional: bool, timeout_ms: int, request: ToolRouterRequest) -> ToolStep:
        params = self._tool_params(request)
        params["purpose"] = purpose
        return ToolStep(
            step_id=step_id,
            tool_name=tool_name,
            purpose=tool_name.value,
            input_ref=input_ref,
            depends_on=depends_on,
            output_key=output_key,
            optional=optional,
            timeout_ms=timeout_ms,
            params=params,
        )

    def _tool_params(self, request: ToolRouterRequest) -> dict[str, object]:
        params = request.normalized_params
        policy = request.execution_policy
        registry = request.tool_registry
        return {
            "task_type": request.parsed_intent.task_type.value,
            "need_realtime": request.parsed_intent.need_realtime,
            "need_explanation": request.parsed_intent.need_explanation,
            "need_recommendation": request.parsed_intent.need_recommendation,
            "entity_type": params.entity_type or request.parsed_intent.candidate_slots.entity_type or "product",
            "keywords": params.keywords,
            "execution_ready": params.execution_ready,
            "max_realtime_candidates": policy.max_realtime_candidates,
            "allow_partial_answer": policy.allow_partial_answer,
            "prefer_snapshot_when_realtime_timeout_ms": policy.prefer_snapshot_when_realtime_timeout_ms,
            "knowledge_optional_for_search": policy.knowledge_optional_for_search,
            "knowledge_retrieval_enabled": registry.knowledge_retrieval_enabled,
            "realtime_enabled": registry.product_realtime_enabled,
            "structured_search_enabled": registry.product_structured_search_enabled,
        }

    def _plan_id(self) -> str:
        return uuid.uuid4().hex
