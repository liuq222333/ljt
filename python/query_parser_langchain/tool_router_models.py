from __future__ import annotations

from enum import Enum
from typing import Any

from pydantic import BaseModel, Field

from .models import ParsedIntent, TaskType


class PlanType(str, Enum):
    SINGLE_TOOL = "single_tool"
    CLARIFICATION_REQUIRED = "clarification_required"
    SEARCH_THEN_KNOWLEDGE = "search_then_knowledge"
    SEARCH_THEN_REALTIME = "search_then_realtime"
    SEARCH_THEN_PARALLEL = "search_then_parallel"
    PARALLEL = "parallel"


class ToolName(str, Enum):
    STRUCTURED_SEARCH = "structured_search"
    KNOWLEDGE_RETRIEVAL = "knowledge_retrieval"
    REALTIME_QUERY = "realtime_query"


class NormalizedParams(BaseModel):
    task_type: TaskType | None = None
    keywords: str | None = None
    entity_type: str | None = None
    execution_ready: bool = False
    missing_required_slots: list[str] = Field(default_factory=list)
    validation_errors: list[str] = Field(default_factory=list)


class SessionContext(BaseModel):
    confirmed_constraints: dict[str, Any] = Field(default_factory=dict)
    pending_slots: list[str] = Field(default_factory=list)
    candidate_entities: list[dict[str, Any]] = Field(default_factory=list)
    focused_entity_id: str | None = None
    focused_entity_type: str | None = None
    last_selected_entity_ids: list[str] = Field(default_factory=list)
    dialogue_turn_index: int = 0
    follow_up_context: dict[str, Any] = Field(default_factory=dict)
    current_stage: str = "initialized"
    clarification_history: list[str] = Field(default_factory=list)


class ExecutionPolicy(BaseModel):
    max_parallel_tools: int = 2
    max_realtime_candidates: int = 10
    allow_partial_answer: bool = True
    clarify_on_missing_required_slots: bool = True
    knowledge_optional_for_search: bool = True
    clarify_on_low_intent_confidence: bool = True
    low_intent_confidence_threshold: float = 0.45
    structured_search_timeout_ms: int = 1200
    knowledge_timeout_ms: int = 800
    realtime_timeout_ms: int = 900
    prefer_snapshot_when_realtime_timeout_ms: int = 1200


class ToolRegistry(BaseModel):
    knowledge_retrieval_enabled: bool = True
    product_structured_search_enabled: bool = True
    event_structured_search_enabled: bool = False
    store_structured_search_enabled: bool = False
    product_realtime_enabled: bool = True
    event_realtime_enabled: bool = False
    store_realtime_enabled: bool = False


class ToolStep(BaseModel):
    step_id: str
    tool_name: ToolName
    purpose: str | None = None
    input_ref: str | None = None
    depends_on: list[str] = Field(default_factory=list)
    output_key: str | None = None
    optional: bool = False
    params: dict[str, Any] = Field(default_factory=dict)
    timeout_ms: int = 0


class ToolPlan(BaseModel):
    plan_id: str | None = None
    plan_type: PlanType
    execution_mode: str = "single"
    requires_clarification: bool = False
    clarification_prompt: str | None = None
    steps: list[ToolStep] = Field(default_factory=list)
    fallback_plan: "ToolPlan | None" = None
    routing_reason: str | None = None
    plan_confidence: float = 0.0
    max_parallel_tools: int = 1


class ToolRouterRequest(BaseModel):
    parsed_intent: ParsedIntent
    normalized_params: NormalizedParams
    session_context: SessionContext = Field(default_factory=SessionContext)
    execution_policy: ExecutionPolicy = Field(default_factory=ExecutionPolicy)
    tool_registry: ToolRegistry = Field(default_factory=ToolRegistry)


ToolPlan.model_rebuild()
