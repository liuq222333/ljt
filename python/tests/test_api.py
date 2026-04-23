import unittest

from fastapi.testclient import TestClient

from query_parser_langchain.api import create_app


class ApiContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.client = TestClient(create_app())

    def test_health(self):
        response = self.client.get('/health')
        self.assertEqual(200, response.status_code)
        self.assertEqual('ok', response.json()['status'])

    def test_parse_intent_contract(self):
        response = self.client.post('/parse_intent', json={'current_message': '\u4e70\u82f9\u679c'})
        self.assertEqual(200, response.status_code)
        self.assertIn('task_type', response.json())

    def test_review_action_contract(self):
        response = self.client.post('/review_action', json={'current_message': '\u5e2e\u6211\u53d1\u5e03\u4e00\u4e2a\u5546\u54c1'})
        self.assertEqual(200, response.status_code)
        self.assertEqual('need_clarification', response.json()['outcome'])


if __name__ == '__main__':
    unittest.main()
