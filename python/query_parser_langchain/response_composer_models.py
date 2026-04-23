from __future__ import annotations

from enum import Enum
from typing import Any

from pydantic import BaseModel, Field

from .models import ParsedIntent
from .tool_router_models import NormalizedParams, SessionContext, ToolPlan


class AnswerType(str, Enum):
    RECOMMENDATION = "recommendation"
    FAQ_ANSWER = "faq_answer"
    REALTIME_CONFIRMATION = "realtime_confirmation"
    CLARIFICATION = "clarification"
    NO_RESULT = "no_result"
    PARTIAL_RESULT = "partial_result"


class SearchItem(BaseModel):
    product_id: str | None = None
    title: str | None = None
    subtitle: str | None = None
    display_price: str | None = None
    store_id: str | None = None
    store_name: str | None = None
    cover_image: str | None = None
    rating: float | None = None
    sales_count: int | None = None
    city_name: str | None = None
    district_name: str | None = None
    business_area_name: str | None = None
    distance_m: int | None = None
    tag_names: list[str] = Field(default_factory=list)
    recommend_score: float | None = None


class SearchResults(BaseModel):
    entity_type: str | None = None
    items: list[SearchItem] = Field(default_factory=list)
    total: int = 0
    returned: int = 0
    source: str | None = None
    query_snapshot_version: str | None = None
    search_status: str = "not_started"


class KnowledgeDoc(BaseModel):
    id: str | None = None
    category: str | None = None
    title: str | None = None
    content: str | None = None


class RetrievedDocs(BaseModel):
    items: list[KnowledgeDoc] = Field(default_factory=list)
    doc_count: int = 0
    retrieval_status: str = "not_started"
    query_version: str | None = None


class RealtimeEntity(BaseModel):
    entity_id: str | None = None
    product_id: str | None = None
    status_text: str | None = None
    confirm_level: str | None = None
    raw_payload: dict[str, Any] = Field(default_factory=dict)


class RealtimeResults(BaseModel):
    items: list[RealtimeEntity] = Field(default_factory=list)
    checked_entity_count: int = 0
    realtime_status: str = "not_started"
    query_ts: str | None = None


class ErrorState(BaseModel):
    has_error: bool = False
    error_code: str | None = None
    error_message: str | None = None
    failed_node: str | None = None
    recoverable: bool = True


class ExecutionMeta(BaseModel):
    request_id: str | None = None
    trace_id: str | None = None
    session_id: str | None = None
    degraded: bool = False
    restored_from_checkpoint: bool = False
    fallback_applied: bool = False
    fallback_reason: str | None = None
    fallback_source_step: str | None = None
    skipped_optional_steps: list[str] = Field(default_factory=list)


class EntityCard(BaseModel):
    entity_id: str | None = None
    entity_type: str | None = None
    title: str | None = None
    subtitle: str | None = None
    image_url: str | None = None
    price_text: str | None = None
    tags: list[str] = Field(default_factory=list)
    location_text: str | None = None
    realtime_status_text: str | None = None
    recommend_reason: str | None = None


class Citation(BaseModel):
    doc_id: str | None = None
    doc_title: str | None = None
    snippet: str | None = None
    confidence: float = 0.0


class FinalAnswer(BaseModel):
    answer_type: AnswerType
    answer_text: str
    cards: list[EntityCard] = Field(default_factory=list)
    disclaimers: list[str] = Field(default_factory=list)
    citations: list[Citation] = Field(default_factory=list)
    next_actions: list[str] = Field(default_factory=list)
    debug_trace: dict[str, Any] = Field(default_factory=dict)
    summary: str | None = None
    composer_meta: dict[str, Any] = Field(default_factory=dict)


class ResponseComposerRequest(BaseModel):
    parsed_intent: ParsedIntent
    normalized_params: NormalizedParams = Field(default_factory=NormalizedParams)
    search_results: SearchResults = Field(default_factory=SearchResults)
    retrieved_docs: RetrievedDocs = Field(default_factory=RetrievedDocs)
    realtime_results: RealtimeResults = Field(default_factory=RealtimeResults)
    tool_plan: ToolPlan | None = None
    session_context: SessionContext = Field(default_factory=SessionContext)
    execution_meta: ExecutionMeta = Field(default_factory=ExecutionMeta)
    error_state: ErrorState = Field(default_factory=ErrorState)
