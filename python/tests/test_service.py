import unittest

from query_parser_langchain.models import QueryParserRequest, SessionContextSummary, TaskType
from query_parser_langchain.service import LangChainQueryParser


class StubChain:
    def __init__(self, result=None, error=None):
        self.result = result
        self.error = error
        self.last_payload = None

    def invoke(self, payload):
        self.last_payload = payload
        if self.error:
            raise self.error
        return self.result


class LangChainQueryParserTest(unittest.TestCase):
    def test_should_use_chain_result_when_available(self):
        chain = StubChain(
            {
                "task_type": "product_search",
                "intent_confidence": 0.91,
                "query_text": "买苹果",
                "candidate_slots": {"keyword": "苹果"},
            }
        )
        parser = LangChainQueryParser(chain=chain)
        result = parser.parse_request(QueryParserRequest(current_message="买苹果"))
        self.assertEqual(TaskType.PRODUCT_SEARCH, result.task_type)
        self.assertEqual("苹果", result.candidate_slots.keyword)

    def test_should_fallback_when_chain_fails(self):
        parser = LangChainQueryParser(chain=StubChain(error=RuntimeError("boom")))
        result = parser.parse_request(QueryParserRequest(current_message="现在还有苹果吗"))
        self.assertTrue(result.need_realtime)

    def test_should_forward_session_context_summary_to_chain(self):
        chain = StubChain({"task_type": "realtime_query", "candidate_slots": {"entity_ref": "recent"}})
        parser = LangChainQueryParser(chain=chain)
        parser.parse_request(
            QueryParserRequest(
                current_message="这个商品有货吗",
                session_context_summary=SessionContextSummary(focused_entity_id="250"),
            )
        )
        self.assertIn('"focused_entity_id": "250"', chain.last_payload["session_context_summary_json"])


if __name__ == '__main__':
    unittest.main()
