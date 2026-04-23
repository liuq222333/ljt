import unittest

from query_parser_langchain.models import ParsedIntent, TaskType
from query_parser_langchain.tool_router_models import ExecutionPolicy, NormalizedParams, SessionContext, ToolRegistry, ToolRouterRequest
from query_parser_langchain.tool_router_service import LangGraphToolRouter


class LangGraphToolRouterTest(unittest.TestCase):
    def test_should_route_focused_entity_realtime_directly(self):
        router = LangGraphToolRouter()
        intent = ParsedIntent(task_type=TaskType.REALTIME_QUERY, query_text="\u8fd9\u4e2a\u73b0\u5728\u8fd8\u80fd\u4e70\u5417", need_realtime=True, intent_confidence=0.8)
        request = ToolRouterRequest(
            parsed_intent=intent,
            normalized_params=NormalizedParams(task_type=intent.task_type, keywords="\u82f9\u679c", entity_type="product", execution_ready=True),
            session_context=SessionContext(focused_entity_id="123", focused_entity_type="product"),
            execution_policy=ExecutionPolicy(),
            tool_registry=ToolRegistry(),
        )
        plan = router.route_request(request)
        self.assertEqual("search_then_realtime", plan.plan_type.value)
        self.assertEqual("realtime_query", plan.steps[-1].tool_name.value)

    def test_should_clarify_when_missing_location(self):
        router = LangGraphToolRouter()
        intent = ParsedIntent(task_type=TaskType.STORE_SEARCH, query_text="\u5e2e\u6211\u627e\u95e8\u5e97", intent_confidence=0.8)
        request = ToolRouterRequest(
            parsed_intent=intent,
            normalized_params=NormalizedParams(task_type=intent.task_type, keywords="\u95e8\u5e97", execution_ready=False, missing_required_slots=["location"]),
            session_context=SessionContext(),
            execution_policy=ExecutionPolicy(),
            tool_registry=ToolRegistry(),
        )
        plan = router.route_request(request)
        self.assertTrue(plan.requires_clarification)


if __name__ == '__main__':
    unittest.main()
