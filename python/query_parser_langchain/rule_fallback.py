from __future__ import annotations

import re

from .models import CandidateSlots, FollowUpType, ParsedIntent, QueryParserRequest, TaskType


_SEARCH_WORDS = [
    "buy",
    "find",
    "search",
    "recommend",
    "look for",
    "买",
    "想买",
    "找",
    "搜索",
    "推荐",
    "看看",
    "有什么",
    "都有什么",
    "都有哪些",
    "还有什么",
    "还有哪些",
    "还有啥",
    "在卖",
]
_EVENT_WORDS = ["活动", "演出", "展览", "讲座", "报名"]
_STORE_WORDS = ["门店", "店铺", "店里", "store", "shop"]
_EXPLAIN_WORDS = ["规则", "退款", "预约", "怎么", "说明", "注意事项", "faq", "详情", "详细信息", "商品详情", "介绍", "具体信息"]
_REALTIME_WORDS = ["now", "live", "realtime", "available", "现在", "今天", "实时", "还能", "在卖", "有货"]
_RECOMMEND_WORDS = ["recommend", "推荐", "找几个", "有什么", "都有什么", "都有哪些", "看看"]
_FOLLOW_UP_WORDS = [
    "again",
    "change",
    "this",
    "that",
    "这个",
    "那个",
    "第一个",
    "刚才",
    "再",
    "换一个",
    "都有什么",
    "都有哪些",
    "还有什么",
    "还有哪些",
    "还有啥",
    "详情",
    "详细信息",
    "商品详情",
    "介绍",
]
_NEGATION_WORDS = ["not", "exclude", "不要", "不想", "排除", "别", "去掉"]
_DETAIL_WORDS = ["详情", "详细信息", "商品详情", "介绍", "具体信息", "更多信息"]
_BUY_PATTERN = re.compile(r"(?:我想要|我想买|我想|我要|想买|买点|买个|买一些|买)(.+?)(?:现在|目前|有什么|还有什么|哪些|在卖|能买|可买|吗|呢|吧|$)")
_SEARCH_PATTERN = re.compile(r"(?:找|搜索|推荐|看看|查一下|查查|帮我找|给我找)(.+?)(?:现在|目前|有什么|还有什么|哪些|在卖|能买|可买|吗|呢|吧|$)")
_ORDINAL_PATTERN = re.compile(r"第([一二三四五六七八九十\d]+)个")
_SHORT_REALTIME_PATTERN = re.compile(r"^(有货吗|还有吗|还在卖吗|能买吗|现在能买吗|现在有货吗)$")


class RuleFallbackParser:
    def parse(self, request: QueryParserRequest) -> ParsedIntent:
        raw_text = (request.current_message or "").strip()
        if not raw_text:
            return ParsedIntent(task_type=TaskType.CHITCHAT, query_text="")

        text = self.normalize_text(raw_text)
        slots = CandidateSlots(
            keyword=self._extract_keyword(text),
            category_text=self._extract_category_text(text),
            entity_ref=self._extract_entity_ref(text, request),
            entity_type=self._extract_entity_type(text),
            location_text=self._extract_location(text),
        )
        need_realtime = any(word in text.lower() for word in [w.lower() for w in _REALTIME_WORDS])
        need_explanation = any(word in text.lower() for word in [w.lower() for w in _EXPLAIN_WORDS])
        need_recommendation = any(word in text.lower() for word in [w.lower() for w in _RECOMMEND_WORDS])
        is_follow_up = any(word in text.lower() for word in [w.lower() for w in _FOLLOW_UP_WORDS]) or bool(slots.entity_ref)
        is_negation = any(word in text.lower() for word in [w.lower() for w in _NEGATION_WORDS])

        task_type = self._resolve_task_type(text, slots, need_realtime, need_explanation)
        follow_up_type = self._resolve_follow_up_type(text, is_follow_up, is_negation)
        negated_entities = [slots.entity_ref] if is_negation and slots.entity_ref else []
        confidence = 0.55
        if task_type in {TaskType.PRODUCT_SEARCH, TaskType.MIXED_SEARCH_REALTIME, TaskType.REALTIME_QUERY}:
            confidence = 0.78
        elif task_type == TaskType.FAQ_QUERY:
            confidence = 0.72
        elif task_type == TaskType.CHITCHAT:
            confidence = 0.4
        return ParsedIntent(
            task_type=task_type,
            intent_confidence=confidence,
            query_text=raw_text,
            candidate_slots=slots,
            need_realtime=need_realtime,
            need_explanation=need_explanation,
            need_recommendation=need_recommendation,
            is_follow_up=is_follow_up,
            is_negation=is_negation,
            follow_up_type=follow_up_type,
            negated_entities=negated_entities,
        )

    def normalize_text(self, text: str) -> str:
        normalized = (text or "").strip()
        if not normalized:
            return normalized
        shopping_context = any(token in normalized for token in ["卖", "售", "买", "商品", "水果", "推荐", "找"])
        if shopping_context:
            normalized = normalized.replace("附件", "附近")
        normalized = normalized.replace("在售卖", "在卖")
        normalized = normalized.replace("正在售卖", "在卖")
        normalized = normalized.replace("售卖", "卖")
        return normalized

    def _resolve_task_type(self, text: str, slots: CandidateSlots, need_realtime: bool, need_explanation: bool) -> TaskType:
        lowered = text.lower()
        has_search = any(word.lower() in lowered for word in _SEARCH_WORDS)
        if any(word.lower() in lowered for word in _STORE_WORDS):
            return TaskType.STORE_SEARCH
        if any(word.lower() in lowered for word in _EVENT_WORDS):
            return TaskType.EVENT_SEARCH
        if need_explanation and (has_search or bool(slots.keyword) or bool(slots.entity_ref) or bool(slots.category_text)):
            return TaskType.MIXED_SEARCH_KNOWLEDGE
        if need_explanation:
            return TaskType.FAQ_QUERY
        if need_realtime and has_search:
            return TaskType.MIXED_SEARCH_REALTIME
        if need_realtime:
            return TaskType.REALTIME_QUERY
        if has_search or slots.keyword:
            return TaskType.PRODUCT_SEARCH
        if any(word in lowered for word in ["hello", "hi", "你好", "您好", "谢谢"]):
            return TaskType.CHITCHAT
        if any(word.lower() in lowered for word in _FOLLOW_UP_WORDS):
            return TaskType.FOLLOW_UP
        return TaskType.CHITCHAT

    def _resolve_follow_up_type(self, text: str, is_follow_up: bool, is_negation: bool) -> FollowUpType | None:
        lowered = text.lower()
        if not is_follow_up and not is_negation:
            return None
        if is_negation:
            return FollowUpType.NEGATE_RESULT
        if "change" in lowered or "换" in text:
            return FollowUpType.CHANGE_CONSTRAINT
        if any(word in lowered for word in ["this", "that"]) or any(word in text for word in ["这个", "那个", "第一个", "刚才"]):
            return FollowUpType.SELECT_ENTITY
        if any(word in lowered for word in ["again", "more"]) or any(word in text for word in ["再", "附近", "便宜"]):
            return FollowUpType.ADD_CONSTRAINT
        return FollowUpType.ASK_DETAIL

    def _extract_keyword(self, text: str) -> str | None:
        for pattern in (_BUY_PATTERN, _SEARCH_PATTERN):
            match = pattern.search(text)
            if match:
                return self._clean_keyword(match.group(1))
        if _SHORT_REALTIME_PATTERN.match(text):
            return None
        return self._clean_keyword(text)

    def _extract_category_text(self, text: str) -> str | None:
        for marker in ["水果", "果蔬", "蔬菜", "苹果", "草莓", "樱桃"]:
            if marker in text:
                return marker
        return None

    def _clean_keyword(self, value: str | None) -> str | None:
        if not value:
            return None
        cleaned = value
        for token in [
            "点",
            "现在",
            "目前",
            "附近",
            "附件",
            "都有什么",
            "都有哪些",
            "有什么",
            "还有什么",
            "还有哪些",
            "还有啥",
            "详细信息",
            "商品详情",
            "具体信息",
            "更多信息",
            "详情",
            "介绍",
            "的信息",
            "的详细信息",
            "的详情",
            "的信息介绍",
            "哪些",
            "在卖",
            "能买",
            "可买",
            "帮我",
            "给我",
            "吗",
            "呢",
            "吧",
            "这个商品",
            "这个",
            "那个",
            "有货",
            "库存",
        ]:
            cleaned = cleaned.replace(token, "")
        cleaned = re.sub(r"^[\uFF0C\u3002\uFF01\uFF1F\u3001\s]+", "", cleaned)
        cleaned = re.sub(r"[\uFF0C\u3002\uFF01\uFF1F\u3001\s]+$", "", cleaned)
        cleaned = re.sub(r"的+$", "", cleaned)
        return cleaned or None

    def _extract_entity_ref(self, text: str, request: QueryParserRequest) -> str | None:
        if any(word in text for word in ["这个", "那个", "刚才"]):
            return "recent"
        match = _ORDINAL_PATTERN.search(text)
        if match:
            return match.group(0)
        if request.session_context_summary and request.session_context_summary.focused_entity_id and any(word in text for word in _DETAIL_WORDS):
            return "recent"
        if request.session_context_summary and request.session_context_summary.focused_entity_id and _SHORT_REALTIME_PATTERN.match(text):
            return "recent"
        return None

    def _extract_entity_type(self, text: str) -> str | None:
        lowered = text.lower()
        if any(word.lower() in lowered for word in _STORE_WORDS):
            return "store"
        if any(word.lower() in lowered for word in _EVENT_WORDS):
            return "event"
        if any(word in lowered for word in ["product", "apple", "fruit"]) or any(word in text for word in ["商品", "水果", "苹果"]):
            return "product"
        return None

    def _extract_location(self, text: str) -> str | None:
        for marker in ["附近", "一食堂", "二食堂", "校门口"]:
            if marker in text:
                return marker
        return None
