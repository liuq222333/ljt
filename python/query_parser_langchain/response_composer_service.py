from __future__ import annotations

from typing import Any, Protocol

from .response_composer_models import (
    AnswerType,
    Citation,
    EntityCard,
    FinalAnswer,
    ResponseComposerRequest,
)


NEGATIVE_REALTIME_MARKERS = {"sold_out", "closed", "unavailable", "degraded", "售罄", "下架", "不可用"}
POSITIVE_REALTIME_MARKERS = {"available", "bookable", "on_sale", "在售", "有货", "可预订", "可下单"}
DETAIL_MARKERS = ("\u8be6\u60c5", "\u8be6\u7ec6\u4fe1\u606f", "\u5546\u54c1\u8be6\u60c5", "\u4ecb\u7ecd", "\u5177\u4f53\u4fe1\u606f", "\u66f4\u591a\u4fe1\u606f")


class Invokable(Protocol):
    def invoke(self, payload: dict[str, Any]) -> str:
        ...


class LangChainResponseComposer:
    def __init__(self, renderer: Invokable | None = None, chain_timeout_seconds: float = 0.55) -> None:
        self._renderer = renderer
        self._timeout = chain_timeout_seconds

    def compose_request(self, request: ResponseComposerRequest) -> FinalAnswer:
        answer = self._build_pre_answer(request)
        answer.disclaimers = self._deduplicate(answer.disclaimers)
        answer.next_actions = self._deduplicate(answer.next_actions)
        answer.citations = self._deduplicate_citations(answer.citations)
        answer.debug_trace = self._build_debug_trace(request, answer)
        answer.composer_meta = {
            "used_sources": self._used_sources(request),
            "degraded": request.execution_meta.degraded or request.error_state.has_error,
        }
        return answer

    def _build_pre_answer(self, request: ResponseComposerRequest) -> FinalAnswer:
        if request.tool_plan and request.tool_plan.requires_clarification:
            return FinalAnswer(
                answer_type=AnswerType.CLARIFICATION,
                summary="还需要补充一些信息",
                answer_text=request.tool_plan.clarification_prompt or "请补充必要信息后，我再继续帮你查询。",
                next_actions=["补充缺失条件后再试一次"],
            )

        if request.search_results.items:
            return self._build_search_answer(request)
        if request.realtime_results.items:
            return self._build_realtime_only_answer(request)
        if request.retrieved_docs.items:
            doc = request.retrieved_docs.items[0]
            return FinalAnswer(
                answer_type=AnswerType.FAQ_ANSWER,
                summary=doc.title or "已找到相关说明",
                answer_text=(doc.content or doc.title or "我已经找到相关说明。")[:180],
                citations=[Citation(doc_id=doc.id, doc_title=doc.title, snippet=(doc.content or "")[:80], confidence=0.72)],
                next_actions=["如果你还想继续追问细节，可以直接继续问我"],
            )
        if request.error_state.has_error:
            return FinalAnswer(
                answer_type=AnswerType.PARTIAL_RESULT,
                summary="本次查询部分完成",
                answer_text="我已经拿到一部分结果，但部分链路暂时不可用。",
                disclaimers=[request.error_state.error_message or "当前服务暂时不稳定，请稍后重试。"],
                next_actions=["你可以稍后重试，或换一个更具体的问法"],
            )
        return FinalAnswer(
            answer_type=AnswerType.NO_RESULT,
            summary="暂时没有找到匹配结果",
            answer_text="我理解了你的需求，但当前没有命中可直接返回的结果。",
            next_actions=["可以换一个更具体的关键词再试试", "如果要查实时状态，可以直接提供商品 ID"],
        )

    def _build_search_answer(self, request: ResponseComposerRequest) -> FinalAnswer:
        cards = []
        for item in request.search_results.items[:5]:
            cards.append(
                EntityCard(
                    entity_id=item.product_id,
                    entity_type=request.search_results.entity_type or request.normalized_params.entity_type or "product",
                    title=item.title,
                    subtitle=item.subtitle,
                    image_url=item.cover_image,
                    price_text=item.display_price,
                    tags=item.tag_names[:4],
                    location_text=self._build_location_text(item),
                    realtime_status_text=self._realtime_status_for(item.product_id, request),
                    recommend_reason=self._build_recommend_reason(item),
                )
            )
        if self._is_detail_request(request):
            return self._build_search_detail_answer(request, cards)
        keyword = request.normalized_params.keywords or request.parsed_intent.candidate_slots.keyword or "相关商品"
        answer_text = f"我先为你找到 {request.search_results.returned or len(cards)} 个和“{keyword}”相关的候选结果，你可以先看看这些商品。"
        if self._renderer is not None:
            try:
                answer_text = self._renderer.invoke({"draft_answer_text": answer_text})
            except Exception:
                pass
        disclaimers = self._build_disclaimers(request)
        return FinalAnswer(
            answer_type=self._resolve_search_answer_type(request),
            summary=f"共找到 {request.search_results.total or len(cards)} 个候选结果",
            answer_text=answer_text,
            cards=cards,
            disclaimers=disclaimers,
            citations=self._build_citations(request),
            next_actions=self._build_search_next_actions(request),
        )

    def _build_realtime_only_answer(self, request: ResponseComposerRequest) -> FinalAnswer:
        positive = [item for item in request.realtime_results.items if (item.status_text or "") in POSITIVE_REALTIME_MARKERS]
        answer_type = AnswerType.REALTIME_CONFIRMATION if positive else AnswerType.PARTIAL_RESULT
        snippets = [f"{item.entity_id or item.product_id}：{item.status_text or '状态未知'}" for item in request.realtime_results.items[:3]]
        return FinalAnswer(
            answer_type=answer_type,
            summary="已获取实时状态",
            answer_text="；".join(snippets),
            disclaimers=self._build_disclaimers(request),
            next_actions=["如果你想继续看更多候选商品，我也可以继续帮你查"],
        )

    def _resolve_search_answer_type(self, request: ResponseComposerRequest) -> AnswerType:
        if request.realtime_results.items:
            return AnswerType.RECOMMENDATION
        if request.retrieved_docs.items:
            return AnswerType.RECOMMENDATION
        return AnswerType.RECOMMENDATION

    def _build_search_next_actions(self, request: ResponseComposerRequest) -> list[str]:
        actions = ["如果你想缩小范围，可以补充价格、地点或类目条件"]
        if request.realtime_results.items:
            actions.append("如果你想继续确认某个商品，我可以继续帮你看实时状态")
        return actions

    def _build_search_detail_answer(self, request: ResponseComposerRequest, cards: list[EntityCard]) -> FinalAnswer:
        items = request.search_results.items[:3]
        if not items:
            return FinalAnswer(
                answer_type=AnswerType.NO_RESULT,
                summary="\u6682\u65f6\u6ca1\u6709\u53ef\u5c55\u5f00\u7684\u8be6\u60c5",
                answer_text="\u6211\u7406\u89e3\u4f60\u60f3\u770b\u66f4\u8be6\u7ec6\u7684\u4fe1\u606f\uff0c\u4f46\u5f53\u524d\u6ca1\u6709\u547d\u4e2d\u53ef\u5c55\u5f00\u7684\u5546\u54c1\u7ed3\u679c\u3002",
                next_actions=["\u4f60\u53ef\u4ee5\u5148\u544a\u8bc9\u6211\u60f3\u770b\u7684\u5546\u54c1\u5173\u952e\u8bcd\uff0c\u6216\u8005\u76f4\u63a5\u70b9\u5f00\u4e0a\u4e00\u6761\u7ed3\u679c\u5361\u7247"],
            )

        specific_entity = bool(request.parsed_intent.candidate_slots.entity_ref) or len(request.search_results.items) == 1
        if specific_entity:
            item = items[0]
            title = item.title or "\u5f53\u524d\u5546\u54c1"
            answer_text = f"\u8fd9\u662f {title} \u7684\u8be6\u7ec6\u4fe1\u606f\uff1a\n" + "\n".join(self._format_item_detail_lines(item, request))
            summary = f"\u5df2\u5c55\u5f00 {title} \u7684\u8be6\u7ec6\u4fe1\u606f"
            next_actions = ["\u5982\u679c\u4f60\u8fd8\u60f3\u7ee7\u7eed\u770b\u5b9e\u65f6\u5e93\u5b58\u3001\u4ef7\u683c\u53d8\u5316\u6216\u8d2d\u4e70\u65b9\u5f0f\uff0c\u53ef\u4ee5\u7ee7\u7eed\u8ffd\u95ee\u6211"]
        else:
            summary_lines = []
            for idx, item in enumerate(items, start=1):
                detail_summary = "\uff1b".join(self._format_item_detail_lines(item, request, concise=True))
                summary_lines.append(f"{idx}. {(item.title or '\u672a\u547d\u540d\u5546\u54c1')}\uff1a{detail_summary}")
            answer_text = "\u6211\u5148\u628a\u5f53\u524d\u8fd9\u6279\u7ed3\u679c\u91cc\u66f4\u503c\u5f97\u770b\u7684\u5546\u54c1\u8be6\u60c5\u5c55\u5f00\u7ed9\u4f60\uff1a\n" + "\n".join(summary_lines)
            summary = f"\u5df2\u5c55\u5f00\u524d {len(items)} \u4e2a\u5019\u9009\u5546\u54c1\u7684\u8be6\u7ec6\u4fe1\u606f"
            next_actions = ["\u5982\u679c\u4f60\u60f3\u770b\u67d0\u4e00\u4e2a\u5546\u54c1\u7684\u5b8c\u6574\u8be6\u60c5\uff0c\u53ef\u4ee5\u7ee7\u7eed\u8bf4\u201c\u7b2c\u4e00\u4e2a\u5546\u54c1\u7684\u8be6\u60c5\u201d"]
        if self._renderer is not None:
            try:
                answer_text = self._renderer.invoke({"draft_answer_text": answer_text})
            except Exception:
                pass
        return FinalAnswer(
            answer_type=AnswerType.RECOMMENDATION,
            summary=summary,
            answer_text=answer_text,
            cards=cards,
            disclaimers=self._build_disclaimers(request),
            citations=self._build_citations(request),
            next_actions=next_actions,
        )

    def _is_detail_request(self, request: ResponseComposerRequest) -> bool:
        if request.parsed_intent.need_explanation:
            return True
        query = request.parsed_intent.query_text or ""
        return any(marker in query for marker in DETAIL_MARKERS)

    def _format_item_detail_lines(self, item, request: ResponseComposerRequest, concise: bool = False) -> list[str]:
        details = []
        if item.subtitle:
            details.append(f"\u7b80\u4ecb\uff1a{item.subtitle}")
        if item.display_price:
            details.append(f"\u4ef7\u683c\uff1a{item.display_price}")
        location = self._build_location_text(item)
        if location:
            details.append(f"\u4f4d\u7f6e\uff1a{location}")
        realtime = self._realtime_status_for(item.product_id, request)
        if realtime:
            label = "\u5b9e\u65f6\u72b6\u6001" if not concise else "\u72b6\u6001"
            details.append(f"{label}\uff1a{realtime}")
        if item.tag_names:
            details.append("\u6807\u7b7e\uff1a" + "\u3001".join(item.tag_names[:4]))
        if item.sales_count is not None and not concise:
            details.append(f"\u9500\u91cf\uff1a{item.sales_count}")
        if item.rating is not None and not concise:
            details.append(f"\u8bc4\u5206\uff1a{item.rating}")
        if not details:
            details.append("\u5f53\u524d\u53ea\u6709\u57fa\u7840\u4fe1\u606f\uff0c\u5efa\u8bae\u70b9\u51fb\u8be6\u60c5\u9875\u67e5\u770b\u66f4\u591a")
        return details

    def _build_disclaimers(self, request: ResponseComposerRequest) -> list[str]:
        disclaimers = []
        for item in request.realtime_results.items:
            status = (item.status_text or "").lower()
            if status in NEGATIVE_REALTIME_MARKERS:
                disclaimers.append("部分商品当前实时状态不佳，建议下单前再次确认。")
                break
        if request.error_state.has_error:
            disclaimers.append(request.error_state.error_message or "当前服务暂时不稳定，请稍后重试。")
        return disclaimers

    def _build_citations(self, request: ResponseComposerRequest) -> list[Citation]:
        citations = []
        for doc in request.retrieved_docs.items[:3]:
            citations.append(Citation(doc_id=doc.id, doc_title=doc.title, snippet=(doc.content or "")[:80], confidence=0.72))
        return citations

    def _build_debug_trace(self, request: ResponseComposerRequest, answer: FinalAnswer) -> dict[str, Any]:
        return {
            "pre_answer": {
                "answer_type": answer.answer_type.value,
                "card_count": len(answer.cards),
                "citation_count": len(answer.citations),
                "disclaimer_count": len(answer.disclaimers),
                "next_action_count": len(answer.next_actions),
            },
            "runtime": {
                "request_id": request.execution_meta.request_id,
                "trace_id": request.execution_meta.trace_id,
                "degraded": request.execution_meta.degraded,
                "fallback_applied": request.execution_meta.fallback_applied,
                "realtime_status": request.realtime_results.realtime_status,
                "search_status": request.search_results.search_status,
                "retrieval_status": request.retrieved_docs.retrieval_status,
            },
        }

    def _build_location_text(self, item) -> str | None:
        pieces = [value for value in [item.city_name, item.district_name, item.business_area_name] if value]
        return " / ".join(pieces) if pieces else None

    def _build_recommend_reason(self, item) -> str | None:
        reasons = []
        if item.title:
            reasons.append("标题匹配")
        if item.tag_names:
            reasons.append(f"命中标签：{item.tag_names[0]}")
        return "；".join(reasons) if reasons else None

    def _realtime_status_for(self, product_id: str | None, request: ResponseComposerRequest) -> str | None:
        for item in request.realtime_results.items:
            if item.product_id == product_id or item.entity_id == product_id:
                return item.status_text
        return None

    def _used_sources(self, request: ResponseComposerRequest) -> list[str]:
        used = []
        if request.search_results.items:
            used.append(request.search_results.source or "search_results")
        if request.retrieved_docs.items:
            used.append("knowledge_retrieval")
        if request.realtime_results.items:
            used.append("realtime_query")
        return used

    def _deduplicate(self, values: list[str]) -> list[str]:
        result = []
        for value in values:
            if value and value not in result:
                result.append(value)
        return result

    def _deduplicate_citations(self, values: list[Citation]) -> list[Citation]:
        result = []
        seen = set()
        for citation in values:
            key = (citation.doc_id, citation.doc_title)
            if key not in seen:
                seen.add(key)
                result.append(citation)
        return result
