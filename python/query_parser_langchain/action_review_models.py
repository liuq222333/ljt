from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field

from .models import ParsedIntent


class ActionRouteSummary(BaseModel):
    resource: str | None = None
    action: str | None = None
    operation_type: str | None = None
    route_id: int | None = None
    route_description: str | None = None


class PendingActionSnapshot(BaseModel):
    session_id: str | None = None
    resource: str | None = None
    action: str | None = None
    operation_type: str | None = None
    display_name: str | None = None
    route_id: int | None = None
    route_description: str | None = None
    params: dict[str, Any] = Field(default_factory=dict)
    payload: dict[str, Any] = Field(default_factory=dict)
    missing_fields: list[str] = Field(default_factory=list)
    route_keywords: list[str] = Field(default_factory=list)
    awaiting_confirmation: bool = False
    original_text: str | None = None


class ActionReviewRequest(BaseModel):
    current_message: str = ""
    parsed_intent: ParsedIntent | None = None
    pending_action: PendingActionSnapshot | None = None
    available_routes: list[ActionRouteSummary] = Field(default_factory=list)


class ActionReviewResponse(BaseModel):
    handled: bool = False
    outcome: str = "none"
    pending_action: PendingActionSnapshot | None = None
    missing_fields: list[str] = Field(default_factory=list)
    executable_payload: dict[str, Any] = Field(default_factory=dict)
    confirmation_message: str | None = None
