from __future__ import annotations

import re
from decimal import Decimal, InvalidOperation

from .action_review_models import ActionReviewRequest, ActionReviewResponse, PendingActionSnapshot


_PRODUCT_ID_PATTERN = re.compile(r'(?i)(?:product(?:\s*id)?|item(?:\s*id)?|\u5546\u54c1(?:\s*(?:id|\u7f16\u53f7))?|id|#)\s*[:\uff1a#]?\s*(\d{1,18})')
_TITLE_PATTERN = re.compile(r'(?:\u5546\u54c1\u540d(?:\u79f0|\u5b57)?|\u6807\u9898|\u540d\u4e3a|\u53eb)\s*(?:\u662f|\u4e3a|\uff1a|:)?\s*([^,\uff0c.\u3002;\uff1b\n]+)')
_INLINE_CREATE_TITLE_PATTERN = re.compile(r'(?:\u53d1\u5e03|\u4e0a\u67b6|\u521b\u5efa|\u65b0\u589e)(?:\u4e00\u4e2a|\u4e00\u4ef6|\u4e2a)?([^,\uff0c.\u3002;\uff1b\n]+?)(?:\u4ef7\u683c|\u5e93\u5b58|\u5730\u70b9|\u56fe\u7247|\u63cf\u8ff0|$)')
_PRICE_PATTERN = re.compile(r'(?:\u4ef7\u683c|\u552e\u4ef7)?\s*(?:\u6539\u6210|\u6539\u4e3a|\u8c03\u6574\u4e3a|\u8bbe\u4e3a|\u8bbe\u7f6e\u4e3a|\u662f|\u4e3a|\uff1a|:)?\s*(\d+(?:\.\d+)?)\s*(?:\u5143|\u5757|\uffe5)?')
_STOCK_PATTERN = re.compile(r'(?:\u5e93\u5b58|\u6570\u91cf|\u8865\u8d27|\u589e\u52a0\u5e93\u5b58|\u52a0\u5e93\u5b58)\s*(?:\u662f|\u4e3a|\u589e\u52a0|\u8865|\u5230|\uff1a|:)?\s*(\d+)')
_PIECE_COUNT_PATTERN = re.compile(r'(\d+)\s*(?:\u4ef6|\u4e2a)')
_LOCATION_PATTERN = re.compile(r'(?:\u5730\u70b9|\u4f4d\u7f6e)\s*(?:\u6539\u6210|\u6539\u4e3a|\u8c03\u6574\u4e3a|\u662f|\u4e3a|\uff1a|:)?\s*([^,\uff0c.\u3002;\uff1b\n]+)')
_CATEGORY_PATTERN = re.compile(r'(?:\u5206\u7c7b|category)\s*(?:id)?\s*(?:\u662f|\u4e3a|\uff1a|:)?\s*(\d+)')
_URL_PATTERN = re.compile(r'https?://[^\s,\uff0c]+')

_CREATE_KEYWORDS = ['\u53d1\u5e03', '\u4e0a\u67b6', '\u521b\u5efa', '\u65b0\u589e']
_TAKE_DOWN_KEYWORDS = ['\u4e0b\u67b6', '\u64a4\u4e0b', '\u505c\u552e', '\u4e0b\u6389']
_UPDATE_PRICE_KEYWORDS = ['\u6539\u4ef7', '\u4fee\u6539\u4ef7\u683c', '\u8c03\u6574\u4ef7\u683c']
_UPDATE_LOCATION_KEYWORDS = ['\u4fee\u6539\u5730\u70b9', '\u8c03\u6574\u5730\u70b9', '\u6539\u5730\u70b9', '\u6539\u4f4d\u7f6e', '\u653e\u5230', '\u79fb\u5230', '\u6539\u5230']
_INCREASE_STOCK_KEYWORDS = ['\u589e\u52a0\u5e93\u5b58', '\u52a0\u5e93\u5b58', '\u8865\u8d27', '\u5e93\u5b58\u589e\u52a0']
_CONFIRM_KEYWORDS = ['\u786e\u8ba4', '\u786e\u8ba4\u6267\u884c', '\u6267\u884c', '\u63d0\u4ea4', '\u5c31\u8fd9\u6837', '\u53ef\u4ee5\u53d1\u5e03', '\u53ef\u4ee5\u6267\u884c']
_CANCEL_KEYWORDS = ['\u53d6\u6d88', '\u4e0d\u7528\u4e86', '\u7b97\u4e86', '\u5148\u522b', '\u4e0d\u6267\u884c']
_PRODUCT_HINT_KEYWORDS = ['\u5546\u54c1', '\u8d27', '\u5356', '\u53d1\u5e03', '\u4e0a\u67b6', '\u4e0b\u67b6']


class ActionReviewService:
    def review(self, request: ActionReviewRequest) -> ActionReviewResponse:
        text = (request.current_message or '').strip()
        pending = request.pending_action.model_copy(deep=True) if request.pending_action else None

        if pending and self._contains_any(text, _CANCEL_KEYWORDS):
            return ActionReviewResponse(handled=True, outcome='cancelled', pending_action=pending)

        signal = self._detect_action_signal(text, request)
        if pending is None and signal is None:
            return ActionReviewResponse()

        if signal is not None:
            pending = self._create_pending(signal, text)

        if pending is None:
            return ActionReviewResponse()

        self._merge_params(pending, text)
        missing = self._missing_fields(pending)
        pending.missing_fields = missing

        if missing:
            pending.awaiting_confirmation = False
            return ActionReviewResponse(handled=True, outcome='need_clarification', pending_action=pending, missing_fields=missing)

        if self._contains_any(text, _CANCEL_KEYWORDS):
            return ActionReviewResponse(handled=True, outcome='cancelled', pending_action=pending)

        if self._contains_any(text, _CONFIRM_KEYWORDS):
            return ActionReviewResponse(
                handled=True,
                outcome='ready_to_execute',
                pending_action=pending,
                executable_payload={
                    'resource': pending.resource,
                    'action': pending.action,
                    'operation_type': pending.operation_type,
                    'params': pending.params,
                    'payload': pending.payload,
                },
            )

        pending.awaiting_confirmation = True
        return ActionReviewResponse(
            handled=True,
            outcome='need_confirmation',
            pending_action=pending,
            confirmation_message=self._build_confirmation_message(pending),
        )

    def _detect_action_signal(self, text: str, request: ActionReviewRequest) -> tuple[str, str, str] | None:
        if not text:
            return None
        if self._contains_any(text, _CREATE_KEYWORDS) and self._looks_like_product_context(request):
            return ('create', 'CREATE', '\u53d1\u5e03\u5546\u54c1')
        if self._contains_any(text, _TAKE_DOWN_KEYWORDS) and self._looks_like_product_context(request):
            return ('take_down', 'UPDATE', '\u4e0b\u67b6\u5546\u54c1')
        if self._contains_any(text, _UPDATE_PRICE_KEYWORDS) or ('\u4ef7\u683c' in text and '\u6539' in text):
            return ('update_price', 'UPDATE', '\u4fee\u6539\u5546\u54c1\u4ef7\u683c')
        if self._contains_any(text, _UPDATE_LOCATION_KEYWORDS) or ('\u5730\u70b9' in text and '\u6539' in text):
            return ('update_location', 'UPDATE', '\u4fee\u6539\u5546\u54c1\u5730\u70b9')
        if self._contains_any(text, _INCREASE_STOCK_KEYWORDS):
            return ('increase_stock', 'UPDATE', '\u589e\u52a0\u5546\u54c1\u5e93\u5b58')
        return None

    def _create_pending(self, signal: tuple[str, str, str], text: str) -> PendingActionSnapshot:
        action, operation_type, display_name = signal
        return PendingActionSnapshot(
            resource='product',
            action=action,
            operation_type=operation_type,
            display_name=display_name,
            original_text=text,
            route_keywords=[action],
        )

    def _looks_like_product_context(self, request: ActionReviewRequest) -> bool:
        text = request.current_message or ''
        if any(keyword in text for keyword in _PRODUCT_HINT_KEYWORDS):
            return True
        intent = request.parsed_intent
        if intent is None or intent.candidate_slots is None:
            return True
        entity_type = intent.candidate_slots.entity_type
        return entity_type in (None, '', 'product')

    def _merge_params(self, pending: PendingActionSnapshot, text: str) -> None:
        if pending.action == 'create':
            title = self._extract_title(text)
            if title:
                pending.payload['title'] = title
            price = self._extract_price(text)
            if price is not None:
                pending.payload['price'] = price
            stock = self._extract_stock(text)
            if stock is not None:
                pending.payload['stockQuantity'] = stock
            location = self._extract_location(text)
            if location:
                pending.payload['location'] = location
            category_id = self._extract_category_id(text)
            if category_id is not None:
                pending.payload['categoryId'] = category_id
            image_urls = self._extract_image_urls(text)
            if image_urls:
                pending.payload['imageUrls'] = image_urls
            return

        product_id = self._extract_product_id(text)
        if product_id is not None:
            pending.params['productId'] = product_id
        if pending.action == 'update_price':
            price = self._extract_price(text)
            if price is not None:
                pending.params['price'] = price
        elif pending.action == 'update_location':
            location = self._extract_location(text)
            if location:
                pending.params['location'] = location
        elif pending.action == 'increase_stock':
            delta = self._extract_stock(text)
            if delta is not None:
                pending.params['delta'] = delta

    def _missing_fields(self, pending: PendingActionSnapshot) -> list[str]:
        if pending.action == 'create':
            required = ['title', 'price', 'stockQuantity', 'location', 'categoryId', 'imageUrls']
            return [name for name in required if not pending.payload.get(name)]
        required = []
        if pending.action in {'take_down', 'update_price', 'update_location', 'increase_stock'}:
            required.append('productId')
        if pending.action == 'update_price':
            required.append('price')
        elif pending.action == 'update_location':
            required.append('location')
        elif pending.action == 'increase_stock':
            required.append('delta')
        return [name for name in required if not pending.params.get(name)]

    def _extract_product_id(self, text: str) -> int | None:
        match = _PRODUCT_ID_PATTERN.search(text)
        return int(match.group(1)) if match else None

    def _extract_title(self, text: str) -> str | None:
        match = _TITLE_PATTERN.search(text)
        if match:
            return self._clean_field(match.group(1))
        inline_match = _INLINE_CREATE_TITLE_PATTERN.search(text)
        if inline_match:
            candidate = self._clean_field(inline_match.group(1))
            if candidate and candidate not in {'\u5546\u54c1', '\u4e1c\u897f', '\u7269\u54c1'}:
                return candidate
        return None

    def _extract_price(self, text: str) -> str | None:
        last = None
        for match in _PRICE_PATTERN.finditer(text):
            last = match.group(1)
        if last is None:
            return None
        try:
            return str(Decimal(last).normalize())
        except InvalidOperation:
            return None

    def _extract_stock(self, text: str) -> int | None:
        match = _STOCK_PATTERN.search(text)
        if match:
            return int(match.group(1))
        piece = _PIECE_COUNT_PATTERN.search(text)
        if piece:
            return int(piece.group(1))
        return None

    def _extract_location(self, text: str) -> str | None:
        match = _LOCATION_PATTERN.search(text)
        return self._clean_field(match.group(1)) if match else None

    def _extract_category_id(self, text: str) -> int | None:
        match = _CATEGORY_PATTERN.search(text)
        return int(match.group(1)) if match else None

    def _extract_image_urls(self, text: str) -> list[str]:
        return [match.group(0) for match in _URL_PATTERN.finditer(text)]

    def _clean_field(self, value: str | None) -> str | None:
        if value is None:
            return None
        cleaned = value.strip()
        cleaned = re.sub(r'^[\uFF1A:\s]+', '', cleaned)
        cleaned = re.sub(r'[,\uff0c.\u3002;\uff1b\s]+$', '', cleaned)
        return cleaned or None

    def _build_confirmation_message(self, pending: PendingActionSnapshot) -> str:
        if pending.action == 'create':
            title = pending.payload.get('title', '')
            price = pending.payload.get('price', '')
            return f'\u8bf7\u786e\u8ba4\u662f\u5426\u6267\u884c{pending.display_name or "\u5199\u64cd\u4f5c"}\uff1a\u540d\u79f0={title}\uff0c\u4ef7\u683c={price}'
        return f'\u8bf7\u786e\u8ba4\u662f\u5426\u6267\u884c{pending.display_name or "\u5199\u64cd\u4f5c"}'

    def _contains_any(self, text: str, keywords: list[str]) -> bool:
        return bool(text) and any(keyword in text for keyword in keywords)
