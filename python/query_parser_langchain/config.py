from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass
class QueryParserSettings:
    api_key: str | None = None
    base_url: str = "https://api.openai.com/v1"
    model: str = "gpt-4o-mini"
    temperature: float = 0.1
    timeout_seconds: float = 0.45
    structured_method: str = "json_schema"


@dataclass
class ResponseComposerSettings:
    api_key: str | None = None
    base_url: str = "https://api.openai.com/v1"
    model: str = "gpt-4o-mini"
    temperature: float = 0.2
    timeout_seconds: float = 0.55


def _first_non_empty(*values: str | None) -> str | None:
    for value in values:
        if value:
            return value
    return None


def _normalize_base_url(value: str | None) -> str:
    if not value:
        return "https://api.openai.com/v1"
    return value.rstrip("/")


def load_query_parser_settings() -> QueryParserSettings:
    return QueryParserSettings(
        api_key=_first_non_empty(os.getenv("OPENAI_API_KEY"), os.getenv("QUERY_PARSER_OPENAI_API_KEY")),
        base_url=_normalize_base_url(_first_non_empty(os.getenv("OPENAI_BASE_URL"), os.getenv("QUERY_PARSER_OPENAI_BASE_URL"))),
        model=_first_non_empty(os.getenv("QUERY_PARSER_OPENAI_MODEL"), os.getenv("OPENAI_MODEL"), "gpt-4o-mini") or "gpt-4o-mini",
        temperature=float(os.getenv("QUERY_PARSER_TEMPERATURE", "0.1")),
        timeout_seconds=float(os.getenv("QUERY_PARSER_TIMEOUT_SECONDS", "0.45")),
        structured_method=os.getenv("QUERY_PARSER_STRUCTURED_METHOD", "json_schema"),
    )


def load_response_composer_settings() -> ResponseComposerSettings:
    return ResponseComposerSettings(
        api_key=_first_non_empty(os.getenv("OPENAI_API_KEY"), os.getenv("RESPONSE_COMPOSER_OPENAI_API_KEY")),
        base_url=_normalize_base_url(_first_non_empty(os.getenv("OPENAI_BASE_URL"), os.getenv("RESPONSE_COMPOSER_OPENAI_BASE_URL"))),
        model=_first_non_empty(os.getenv("RESPONSE_COMPOSER_OPENAI_MODEL"), os.getenv("OPENAI_MODEL"), "gpt-4o-mini") or "gpt-4o-mini",
        temperature=float(os.getenv("RESPONSE_COMPOSER_TEMPERATURE", "0.2")),
        timeout_seconds=float(os.getenv("RESPONSE_COMPOSER_TIMEOUT_SECONDS", "0.55")),
    )
