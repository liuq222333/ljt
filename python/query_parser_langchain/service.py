from __future__ import annotations

import json
import logging
import os
import re
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeoutError
from typing import Any, Protocol

from .config import load_query_parser_settings
from .models import CandidateSlots, ParsedIntent, QueryParserRequest, TaskType
from .prompt import HUMAN_PROMPT, SYSTEM_PROMPT
from .rule_fallback import RuleFallbackParser

logger = logging.getLogger(__name__)

_PURE_CHITCHAT_WORDS = {
    "hello",
    "hi",
    "你好",
    "您好",
    "你好啊",
    "你好呀",
    "在吗",
    "嗨",
    "谢谢",
    "感谢",
    "再见",
    "拜拜",
    "早",
    "早上好",
    "晚上好",
    "晚安",
    "ok",
    "好的",
    "嗯",
    "嗯嗯",
}


class Invokable(Protocol):
    def invoke(self, payload: dict[str, Any]) -> Any:
        ...


class LangChainQueryParser:
    def __init__(self, chain: Invokable | None = None, fallback_parser: RuleFallbackParser | None = None, chain_timeout_seconds: float = 0.45) -> None:
        settings = load_query_parser_settings()
        self._chain = chain or self._build_default_chain(settings)
        self._fallback = fallback_parser or RuleFallbackParser()
        self._timeout = chain_timeout_seconds or settings.timeout_seconds

    def parse_request(self, request: QueryParserRequest) -> ParsedIntent:
        if not (request.current_message or "").strip():
            return ParsedIntent(task_type=TaskType.CHITCHAT, query_text="")
        if self._is_pure_chitchat(request.current_message):
            return ParsedIntent(
                task_type=TaskType.CHITCHAT,
                intent_confidence=0.95,
                query_text=request.current_message,
                candidate_slots=CandidateSlots(),
            )
        if self._chain is None:
            return self._fallback.parse(request)
        try:
            with ThreadPoolExecutor(max_workers=1) as executor:
                future = executor.submit(self._invoke_chain, request)
                payload = future.result(timeout=self._timeout)
            return self._coerce_result(payload, request.current_message)
        except FuturesTimeoutError:
            logger.warning("query parser chain timed out; fallback to rules")
        except Exception as exc:
            logger.warning("query parser chain failed; fallback to rules: %s", exc)
        return self._fallback.parse(request)

    def _is_pure_chitchat(self, text: str) -> bool:
        normalized = re.sub(r"[，,。！？.!?、；;：:\s]+", "", (text or "").strip().lower())
        if not normalized:
            return True
        return normalized in _PURE_CHITCHAT_WORDS

    def _invoke_chain(self, request: QueryParserRequest) -> Any:
        assert self._chain is not None
        payload = {
            "current_message": request.current_message,
            "recent_messages_json": json.dumps([message.model_dump() for message in request.recent_messages], ensure_ascii=False),
            "session_context_summary_json": json.dumps((request.session_context_summary.model_dump() if request.session_context_summary else {}), ensure_ascii=False),
            "user_profile_json": json.dumps(request.user_profile or {}, ensure_ascii=False),
        }
        return self._chain.invoke(payload)

    def _coerce_result(self, payload: Any, current_message: str) -> ParsedIntent:
        if isinstance(payload, ParsedIntent):
            result = payload
        elif isinstance(payload, dict):
            slots_payload = payload.get("candidate_slots") or {}
            result = ParsedIntent(
                task_type=TaskType(payload.get("task_type", TaskType.CHITCHAT.value)),
                intent_confidence=float(payload.get("intent_confidence", 0.0) or 0.0),
                query_text=payload.get("query_text") or current_message,
                candidate_slots=CandidateSlots(**slots_payload),
                need_realtime=bool(payload.get("need_realtime", False)),
                need_explanation=bool(payload.get("need_explanation", False)),
                need_recommendation=bool(payload.get("need_recommendation", False)),
                is_follow_up=bool(payload.get("is_follow_up", False)),
                is_negation=bool(payload.get("is_negation", False)),
                follow_up_type=payload.get("follow_up_type"),
                negated_entities=list(payload.get("negated_entities") or []),
            )
        else:
            raise TypeError("unsupported parser payload")
        if not result.query_text:
            result.query_text = current_message
        return result

    def _build_default_chain(self, settings):
        if not settings.api_key or os.getenv("QUERY_PARSER_ENABLE_OPENAI", "").lower() not in {"1", "true", "yes"}:
            return None
        try:
            from langchain_core.prompts import ChatPromptTemplate
            from langchain_openai import ChatOpenAI
        except Exception as exc:
            logger.warning("langchain openai unavailable: %s", exc)
            return None

        llm = ChatOpenAI(
            api_key=settings.api_key,
            base_url=settings.base_url,
            model=settings.model,
            temperature=settings.temperature,
            timeout=settings.timeout_seconds,
        )
        prompt = ChatPromptTemplate.from_messages([
            ("system", SYSTEM_PROMPT),
            ("human", HUMAN_PROMPT),
        ])
        return prompt | llm.with_structured_output(ParsedIntent)
