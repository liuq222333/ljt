from __future__ import annotations

from fastapi import FastAPI

from .action_review_models import ActionReviewRequest, ActionReviewResponse
from .action_review_service import ActionReviewService
from .models import ParsedIntent, QueryParserRequest
from .response_composer_models import FinalAnswer, ResponseComposerRequest
from .response_composer_service import LangChainResponseComposer
from .service import LangChainQueryParser
from .tool_router_models import ToolPlan, ToolRouterRequest
from .tool_router_service import LangGraphToolRouter


def create_app() -> FastAPI:
    app = FastAPI(title='query-parser-langchain')
    parser = LangChainQueryParser()
    router = LangGraphToolRouter()
    composer = LangChainResponseComposer()
    action_review = ActionReviewService()

    @app.get('/health')
    def health() -> dict[str, str]:
        return {'status': 'ok'}

    @app.post('/parse_intent', response_model=ParsedIntent)
    def parse_intent(request: QueryParserRequest) -> ParsedIntent:
        return parser.parse_request(request)

    @app.post('/route_tools', response_model=ToolPlan)
    def route_tools(request: ToolRouterRequest) -> ToolPlan:
        return router.route_request(request)

    @app.post('/compose_response', response_model=FinalAnswer)
    def compose_response(request: ResponseComposerRequest) -> FinalAnswer:
        return composer.compose_request(request)

    @app.post('/review_action', response_model=ActionReviewResponse)
    def review_action(request: ActionReviewRequest) -> ActionReviewResponse:
        return action_review.review(request)

    return app


app = create_app()
