import unittest

from query_parser_langchain.models import FollowUpType, QueryParserRequest, SessionContextSummary, TaskType
from query_parser_langchain.rule_fallback import RuleFallbackParser


class RuleFallbackParserTest(unittest.TestCase):
    def test_should_return_chitchat_for_empty_message(self):
        parser = RuleFallbackParser()
        result = parser.parse(QueryParserRequest(current_message=""))
        self.assertEqual(TaskType.CHITCHAT, result.task_type)

    def test_should_parse_mixed_search_realtime(self):
        parser = RuleFallbackParser()
        result = parser.parse(QueryParserRequest(current_message="我想买点水果，现在有什么水果在卖"))
        self.assertEqual(TaskType.MIXED_SEARCH_REALTIME, result.task_type)
        self.assertEqual("水果", result.candidate_slots.keyword)

    def test_should_mark_follow_up_negation(self):
        parser = RuleFallbackParser()
        result = parser.parse(QueryParserRequest(current_message="不要这个，换一个"))
        self.assertTrue(result.is_follow_up)
        self.assertTrue(result.is_negation)
        self.assertEqual(FollowUpType.NEGATE_RESULT, result.follow_up_type)

    def test_should_normalize_nearby_typo_and_sale_phrase(self):
        parser = RuleFallbackParser()
        result = parser.parse(QueryParserRequest(current_message="附件有什么水果在售卖"))
        self.assertEqual(TaskType.MIXED_SEARCH_REALTIME, result.task_type)
        self.assertEqual("附近", result.candidate_slots.location_text)
        self.assertEqual("水果", result.candidate_slots.keyword)

    def test_should_reuse_recent_entity_for_short_realtime_follow_up(self):
        parser = RuleFallbackParser()
        result = parser.parse(
            QueryParserRequest(
                current_message="有货吗",
                session_context_summary=SessionContextSummary(focused_entity_id="250"),
            )
        )
        self.assertEqual(TaskType.REALTIME_QUERY, result.task_type)
        self.assertEqual("recent", result.candidate_slots.entity_ref)

    def test_should_treat_generic_search_follow_up_as_search_without_keyword(self):
        parser = RuleFallbackParser()
        result = parser.parse(QueryParserRequest(current_message="都有什么"))
        self.assertEqual(TaskType.PRODUCT_SEARCH, result.task_type)
        self.assertTrue(result.is_follow_up)
        self.assertIsNone(result.candidate_slots.keyword)

    def test_should_parse_detail_follow_up_as_mixed_search_knowledge(self):
        parser = RuleFallbackParser()
        result = parser.parse(
            QueryParserRequest(
                current_message="水果的详细信息",
                session_context_summary=SessionContextSummary(focused_entity_id="250"),
            )
        )
        self.assertEqual(TaskType.MIXED_SEARCH_KNOWLEDGE, result.task_type)
        self.assertTrue(result.need_explanation)
        self.assertEqual("recent", result.candidate_slots.entity_ref)
        self.assertEqual("水果", result.candidate_slots.keyword)

    def test_should_resolve_recent_entity_for_plain_detail_follow_up(self):
        parser = RuleFallbackParser()
        result = parser.parse(
            QueryParserRequest(
                current_message="详细信息",
                session_context_summary=SessionContextSummary(focused_entity_id="250"),
            )
        )
        self.assertEqual(TaskType.MIXED_SEARCH_KNOWLEDGE, result.task_type)
        self.assertEqual("recent", result.candidate_slots.entity_ref)
        self.assertIsNone(result.candidate_slots.keyword)


if __name__ == '__main__':
    unittest.main()
