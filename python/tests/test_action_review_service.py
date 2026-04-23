import unittest

from query_parser_langchain.action_review_models import ActionReviewRequest, PendingActionSnapshot
from query_parser_langchain.action_review_service import ActionReviewService
from query_parser_langchain.models import ParsedIntent, TaskType


class ActionReviewServiceTest(unittest.TestCase):
    def test_should_request_missing_fields_for_create(self):
        service = ActionReviewService()
        request = ActionReviewRequest(
            current_message='\u5e2e\u6211\u53d1\u5e03\u4e00\u4e2a\u5546\u54c1',
            parsed_intent=ParsedIntent(task_type=TaskType.PRODUCT_SEARCH, query_text='\u5e2e\u6211\u53d1\u5e03\u4e00\u4e2a\u5546\u54c1'),
        )
        result = service.review(request)
        self.assertTrue(result.handled)
        self.assertEqual('need_clarification', result.outcome)
        self.assertIn('title', result.missing_fields)

    def test_should_confirm_when_pending_action_is_complete(self):
        service = ActionReviewService()
        request = ActionReviewRequest(
            current_message='\u786e\u8ba4\u6267\u884c',
            parsed_intent=ParsedIntent(task_type=TaskType.PRODUCT_SEARCH, query_text='\u786e\u8ba4\u6267\u884c'),
            pending_action=PendingActionSnapshot(
                action='create',
                resource='product',
                payload={
                    'title': '\u82f9\u679c',
                    'price': '3',
                    'stockQuantity': 10,
                    'location': '\u4e00\u98df\u5802',
                    'categoryId': 15,
                    'imageUrls': ['https://img.example.com/a.jpg'],
                },
            ),
        )
        result = service.review(request)
        self.assertEqual('ready_to_execute', result.outcome)
        self.assertEqual('create', result.executable_payload['action'])


if __name__ == '__main__':
    unittest.main()
