from __future__ import annotations

from enum import Enum
from typing import Any

from pydantic import BaseModel, Field


class TaskType(str, Enum):
    PRODUCT_SEARCH = "product_search"
    EVENT_SEARCH = "event_search"
    STORE_SEARCH = "store_search"
    FAQ_QUERY = "faq_query"
    REALTIME_QUERY = "realtime_query"
    MIXED_SEARCH_KNOWLEDGE = "mixed_search_knowledge"
    MIXED_SEARCH_REALTIME = "mixed_search_realtime"
    FOLLOW_UP = "follow_up"
    CLARIFICATION_RESPONSE = "clarification_response"
    CHITCHAT = "chitchat"


class FollowUpType(str, Enum):
    ADD_CONSTRAINT = "add_constraint"
    CHANGE_CONSTRAINT = "change_constraint"
    NEGATE_RESULT = "negate_result"
    SELECT_ENTITY = "select_entity"
    ASK_DETAIL = "ask_detail"
    SWITCH_TOPIC = "switch_topic"


class AgentChatMessage(BaseModel):
    role: str = "user"
    content: str = ""


class SessionContextSummary(BaseModel):
    confirmed_constraints: dict[str, Any] = Field(default_factory=dict)
    candidate_entities: list[dict[str, Any]] = Field(default_factory=list)
    focused_entity_id: str | None = None
    last_selected_entity_ids: list[str] = Field(default_factory=list)
    current_stage: str | None = None


class CandidateSlots(BaseModel):
    keyword: str | None = None
    category_text: str | None = None
    crowd_tag_text: str | None = None
    scene_tag_text: str | None = None
    city_text: str | None = None
    district_text: str | None = None
    location_text: str | None = None
    price_text: str | None = None
    date_text: str | None = None
    sort_text: str | None = None
    entity_type: str | None = None
    entity_ref: str | None = None


class ParsedIntent(BaseModel):
    task_type: TaskType = TaskType.CHITCHAT
    intent_confidence: float = 0.0
    query_text: str = ""
    candidate_slots: CandidateSlots = Field(default_factory=CandidateSlots)
    need_realtime: bool = False
    need_explanation: bool = False
    need_recommendation: bool = False
    is_follow_up: bool = False
    is_negation: bool = False
    follow_up_type: FollowUpType | None = None
    negated_entities: list[str] = Field(default_factory=list)


class QueryParserRequest(BaseModel):
    current_message: str = ""
    recent_messages: list[AgentChatMessage] = Field(default_factory=list)
    session_context_summary: SessionContextSummary | None = None
    user_profile: dict[str, Any] = Field(default_factory=dict)
