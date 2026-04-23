import unittest

from query_parser_langchain.models import CandidateSlots, ParsedIntent, TaskType
from query_parser_langchain.response_composer_models import AnswerType, ExecutionMeta, ResponseComposerRequest, SearchItem, SearchResults
from query_parser_langchain.response_composer_service import LangChainResponseComposer
from query_parser_langchain.tool_router_models import PlanType, ToolPlan


class LangChainResponseComposerTest(unittest.TestCase):
    def test_should_build_search_answer_with_cards_and_disclaimers(self):
        composer = LangChainResponseComposer(renderer=None)
        intent = ParsedIntent(task_type=TaskType.PRODUCT_SEARCH, query_text="有什么水果在卖", need_recommendation=True)
        request = ResponseComposerRequest(
            parsed_intent=intent,
            tool_plan=ToolPlan(plan_type=PlanType.SINGLE_TOOL, execution_mode="single"),
            search_results=SearchResults(
                entity_type="product",
                total=1,
                returned=1,
                search_status="success",
                items=[SearchItem(product_id="1", title="苹果", display_price="5元/斤", tag_names=["水果"])],
            ),
            execution_meta=ExecutionMeta(request_id="req-1", trace_id="trace-1", session_id="s-1"),
        )
        answer = composer.compose_request(request)
        self.assertEqual(AnswerType.RECOMMENDATION, answer.answer_type)
        self.assertEqual(1, len(answer.cards))
        self.assertTrue(answer.summary)
        self.assertIn("pre_answer", answer.debug_trace)
        self.assertIn("苹果", answer.cards[0].title)
        self.assertIn("候选结果", answer.summary)
        self.assertNotIn("???", answer.answer_text)

    def test_should_build_clarification_answer(self):
        composer = LangChainResponseComposer(renderer=None)
        intent = ParsedIntent(task_type=TaskType.STORE_SEARCH, query_text="帮我找门店")
        request = ResponseComposerRequest(
            parsed_intent=intent,
            tool_plan=ToolPlan(
                plan_type=PlanType.CLARIFICATION_REQUIRED,
                execution_mode="single",
                requires_clarification=True,
                clarification_prompt="还缺少这些信息：location",
            ),
        )
        answer = composer.compose_request(request)
        self.assertEqual(AnswerType.CLARIFICATION, answer.answer_type)
        self.assertIn("还缺少这些信息", answer.answer_text)

    def test_should_build_detail_answer_for_recent_entity(self):
        composer = LangChainResponseComposer(renderer=None)
        intent = ParsedIntent(
            task_type=TaskType.MIXED_SEARCH_KNOWLEDGE,
            query_text="水果的详细信息",
            need_explanation=True,
            candidate_slots=CandidateSlots(keyword="水果", entity_ref="recent"),
        )
        request = ResponseComposerRequest(
            parsed_intent=intent,
            tool_plan=ToolPlan(plan_type=PlanType.SEARCH_THEN_KNOWLEDGE, execution_mode="serial"),
            search_results=SearchResults(
                entity_type="product",
                total=1,
                returned=1,
                search_status="success",
                items=[
                    SearchItem(
                        product_id="250",
                        title="草莓",
                        subtitle="当天新到，适合直接食用",
                        display_price="18元/盒",
                        city_name="上海",
                        district_name="浦东新区",
                        tag_names=["水果", "新鲜直达"],
                    )
                ],
            ),
            execution_meta=ExecutionMeta(request_id="req-2", trace_id="trace-2", session_id="s-2"),
        )

        answer = composer.compose_request(request)

        self.assertEqual(AnswerType.RECOMMENDATION, answer.answer_type)
        self.assertEqual(1, len(answer.cards))
        self.assertIn("详细信息", answer.summary)
        self.assertIn("草莓", answer.answer_text)
        self.assertIn("价格", answer.answer_text)
        self.assertIn("简介", answer.answer_text)

    def test_should_expand_multiple_items_for_detail_request(self):
        composer = LangChainResponseComposer(renderer=None)
        intent = ParsedIntent(
            task_type=TaskType.MIXED_SEARCH_KNOWLEDGE,
            query_text="水果详情",
            need_explanation=True,
            candidate_slots=CandidateSlots(keyword="水果"),
        )
        request = ResponseComposerRequest(
            parsed_intent=intent,
            tool_plan=ToolPlan(plan_type=PlanType.SEARCH_THEN_KNOWLEDGE, execution_mode="serial"),
            search_results=SearchResults(
                entity_type="product",
                total=2,
                returned=2,
                search_status="success",
                items=[
                    SearchItem(product_id="1", title="苹果", display_price="5元/斤", tag_names=["水果"]),
                    SearchItem(product_id="2", title="香蕉", display_price="6元/斤", tag_names=["水果"]),
                ],
            ),
        )

        answer = composer.compose_request(request)

        self.assertEqual(AnswerType.RECOMMENDATION, answer.answer_type)
        self.assertEqual(2, len(answer.cards))
        self.assertIn("1. 苹果", answer.answer_text)
        self.assertIn("2. 香蕉", answer.answer_text)
        self.assertIn("详细信息", answer.summary)


if __name__ == '__main__':
    unittest.main()
