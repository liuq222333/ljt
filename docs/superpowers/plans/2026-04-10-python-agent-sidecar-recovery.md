
# Python Agent Sidecar Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 恢复 Python 智能检索 sidecar，按“Python 优先、Java 兜底”的方式重新接回主链，并把 `python` 目录下非运行时的图表脚本归档到工具目录。

**Architecture:** Python sidecar 负责 `parse_intent`、`route_tools`、`compose_response`、`review_action` 四类编排能力，Java 保留搜索、知识检索、实时查询和真实写接口执行的权威能力。Java 通过独立 HTTP client 调用 sidecar，并在 sidecar 关闭、超时、5xx、返回结构非法或字段缺失时自动切回当前本地实现，保证现有链路不被新恢复工作阻断。

**Tech Stack:** Java 17 + Spring Boot + RestTemplate + MockWebServer，Python 3.13 + FastAPI + Pydantic + LangChain/LangGraph + `unittest`

---

## File Structure

### Python runtime sidecar
- Create: `python/pyproject.toml` - Python sidecar 包元数据，修复 editable install 指向缺失源码目录的问题。
- Create: `python/query_parser_langchain/__init__.py` - 暴露 `create_app` 与核心服务导出。
- Create: `python/query_parser_langchain/api.py` - FastAPI app，统一挂载 `/health`、`/parse_intent`、`/route_tools`、`/compose_response`、`/review_action`。
- Create: `python/query_parser_langchain/config.py` - Query Parser / Response Composer 配置读取和默认值。
- Create: `python/query_parser_langchain/models.py` - `parse_intent` 请求响应模型与枚举。
- Create: `python/query_parser_langchain/prompt.py` - Query Parser prompt 模板。
- Create: `python/query_parser_langchain/rule_fallback.py` - parser 规则兜底，覆盖空消息、追问、否定、实时意图、候选实体引用。
- Create: `python/query_parser_langchain/service.py` - `LangChainQueryParser`，链路超时/异常时自动退回 `RuleFallbackParser`。
- Create: `python/query_parser_langchain/tool_router_models.py` - tool router 输入输出模型。
- Create: `python/query_parser_langchain/tool_router_service.py` - `LangGraphToolRouter`，输出 ToolPlan/FallbackPlan。
- Create: `python/query_parser_langchain/response_composer_models.py` - response composer 输入输出模型，补齐 `summary` / `composer_meta` / `debug_trace`。
- Create: `python/query_parser_langchain/response_composer_service.py` - `LangChainResponseComposer`，负责检索结果、知识结果、实时结果、降级提示拼装。
- Create: `python/query_parser_langchain/action_review_models.py` - 写操作识别/补参/确认状态的请求响应模型。
- Create: `python/query_parser_langchain/action_review_service.py` - 写操作编排服务，只输出识别结果与执行草稿，不直接调业务接口。

### Python tests and runbook
- Create: `python/tests/test_rule_fallback.py` - parser 规则兜底测试。
- Create: `python/tests/test_service.py` - `LangChainQueryParser` 超时/异常/fallback 测试。
- Create: `python/tests/test_tool_router_service.py` - Tool router 路由与 fallback plan 测试。
- Create: `python/tests/test_response_composer_service.py` - composer 卡片、免责声明、调试轨迹测试。
- Create: `python/tests/test_action_review_service.py` - 写操作识别、补参、确认、取消测试。
- Create: `python/tests/test_api.py` - FastAPI contract smoke tests。
- Create: `python/README.md` - Python sidecar 启动、测试、联调说明。

### Archived tools
- Move: `python/generate_access_business_notification_architecture_drawio.py`
- Move: `python/generate_backend_api_model_branch_flow_drawio.py`
- Move: `python/generate_backend_api_tool_flow_drawio.py`
- Move: `python/generate_chen_er_drawio.py`
- Move: `python/generate_class_drawio_from_mmd.py`
- Move: `python/generate_experiment_validation_flow_drawio.py`
- Move: `python/generate_flow_drawio.py`
- Move: `python/generate_frontend_backend_infra_architecture_drawio.py`
- Move: `python/generate_notification_architecture_drawio.py`
- Create: `python/tools/diagram_generators/README.md` - 说明这些脚本只服务 `docs/diagrams`，不参与业务主链运行。

### Java sidecar integration
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Config/QueryParserPythonProperties.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Config/AgentRouterPythonProperties.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Config/AgentComposerPythonProperties.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Config/AgentActionReviewPythonProperties.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonSidecarException.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonSidecarHttpClient.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonSidecarModels.java` - sidecar DTO，集中定义 snake_case payload。
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonSidecarMapper.java` - DTO 与现有 `ParsedIntent` / `FinalAnswer` / `PendingAction` 之间的映射。
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonQueryParserClient.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonToolRouterClient.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonResponseComposerClient.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonActionReviewClient.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/SidecarToolPlanAdapter.java` - 把 Python ToolPlan 收敛到当前 Java 执行层使用的 runtime plan。
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/SidecarActionReviewCoordinator.java` - 根据 Python review 结果更新 `ActionConversationStore` 并在需要时调用 Java 写接口。
- Modify: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Service/QueryParserService.java`
- Modify: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/AgentRuntime.java`
- Modify: `backend/demo1/src/main/resources/application.properties`

### Java tests
- Create: `backend/demo1/src/test/java/com/example/demo/demos/Agent/Python/PythonQueryParserClientTest.java`
- Create: `backend/demo1/src/test/java/com/example/demo/demos/Agent/Python/PythonToolRouterClientTest.java`
- Create: `backend/demo1/src/test/java/com/example/demo/demos/Agent/Python/PythonResponseComposerClientTest.java`
- Create: `backend/demo1/src/test/java/com/example/demo/demos/Agent/Python/PythonActionReviewClientTest.java`
- Create: `backend/demo1/src/test/java/com/example/demo/demos/Agent/Service/QueryParserServiceTest.java`
- Modify: `backend/demo1/src/test/java/com/example/demo/demos/Agent/Runtime/AgentRuntimeTest.java`

---

### Task 1: Restore the Python package skeleton and archive diagram generators

**Files:**
- Create: `python/pyproject.toml`
- Create: `python/query_parser_langchain/__init__.py`
- Create: `python/tools/diagram_generators/README.md`
- Move: `python/generate_access_business_notification_architecture_drawio.py`
- Move: `python/generate_backend_api_model_branch_flow_drawio.py`
- Move: `python/generate_backend_api_tool_flow_drawio.py`
- Move: `python/generate_chen_er_drawio.py`
- Move: `python/generate_class_drawio_from_mmd.py`
- Move: `python/generate_experiment_validation_flow_drawio.py`
- Move: `python/generate_flow_drawio.py`
- Move: `python/generate_frontend_backend_infra_architecture_drawio.py`
- Move: `python/generate_notification_architecture_drawio.py`

- [ ] **Step 1: Create package metadata and a minimal importable package**

```toml
[build-system]
requires = ["setuptools>=69", "wheel"]
build-backend = "setuptools.build_meta"

[project]
name = "query-parser-langchain"
version = "0.1.0"
requires-python = ">=3.11"
dependencies = [
  "fastapi>=0.115,<1",
  "uvicorn>=0.30,<1",
  "pydantic>=2.8,<3",
  "langchain>=0.3,<0.4",
  "langchain-openai>=0.3,<0.4",
  "langgraph>=0.3,<0.4",
]

[tool.setuptools.packages.find]
where = ["."]
include = ["query_parser_langchain*"]
```

```python
# python/query_parser_langchain/__init__.py
from .api import create_app

__all__ = ["create_app"]
```

- [ ] **Step 2: Move the non-runtime draw.io generators into an archived tools directory**

```powershell
New-Item -ItemType Directory -Force 'python/tools/diagram_generators' | Out-Null
Move-Item 'python/generate_access_business_notification_architecture_drawio.py' 'python/tools/diagram_generators/'
Move-Item 'python/generate_backend_api_model_branch_flow_drawio.py' 'python/tools/diagram_generators/'
Move-Item 'python/generate_backend_api_tool_flow_drawio.py' 'python/tools/diagram_generators/'
Move-Item 'python/generate_chen_er_drawio.py' 'python/tools/diagram_generators/'
Move-Item 'python/generate_class_drawio_from_mmd.py' 'python/tools/diagram_generators/'
Move-Item 'python/generate_experiment_validation_flow_drawio.py' 'python/tools/diagram_generators/'
Move-Item 'python/generate_flow_drawio.py' 'python/tools/diagram_generators/'
Move-Item 'python/generate_frontend_backend_infra_architecture_drawio.py' 'python/tools/diagram_generators/'
Move-Item 'python/generate_notification_architecture_drawio.py' 'python/tools/diagram_generators/'
```

```markdown
# Diagram Generator Archive

这些脚本只用于生成 `docs/diagrams` 下的图表产物。
它们不是 Python sidecar 运行时的一部分，也不会在主服务启动时被导入。
如需重新生成图表，请在 `python/tools/diagram_generators` 目录下单独执行对应脚本。
```

- [ ] **Step 3: Refresh the editable install so the virtualenv points back to real source files**

Run: `Set-Location python; .\.venv\Scripts\python.exe -m pip install -e .`
Expected: `Successfully installed query-parser-langchain-0.1.0`

- [ ] **Step 4: Verify the skeleton imports and archived tools compile cleanly**

Run: `Set-Location python; .\.venv\Scripts\python.exe -m compileall query_parser_langchain tools/diagram_generators`
Expected: `Compiling 'query_parser_langchain\\__init__.py'...` and no syntax errors

- [ ] **Step 5: Commit**

```bash
git add python/pyproject.toml python/query_parser_langchain/__init__.py python/tools/diagram_generators
git commit -m "chore(python): restore sidecar package skeleton"
```
### Task 2: Rebuild `parse_intent` with LangChain + rule fallback

**Files:**
- Create: `python/query_parser_langchain/models.py`
- Create: `python/query_parser_langchain/config.py`
- Create: `python/query_parser_langchain/prompt.py`
- Create: `python/query_parser_langchain/rule_fallback.py`
- Create: `python/query_parser_langchain/service.py`
- Modify: `python/query_parser_langchain/api.py`
- Test: `python/tests/test_rule_fallback.py`
- Test: `python/tests/test_service.py`

- [ ] **Step 1: Write the failing parser and fallback tests**

```python
import unittest

from query_parser_langchain.models import ParsedIntent, QueryParserRequest, SessionContextSummary, TaskType
from query_parser_langchain.rule_fallback import RuleFallbackParser
from query_parser_langchain.service import LangChainQueryParser


class StubChain:
    def __init__(self, result=None, error=None):
        self.result = result
        self.error = error

    def invoke(self, payload):
        if self.error:
            raise self.error
        return self.result


class LangChainQueryParserTest(unittest.TestCase):
    def test_should_use_chain_result_when_available(self):
        parser = LangChainQueryParser(
            chain=StubChain({
                "task_type": "product_search",
                "intent_confidence": 0.91,
                "query_text": "买苹果",
                "candidate_slots": {"keyword": "苹果"},
            })
        )
        result = parser.parse_request(QueryParserRequest(current_message="买苹果"))
        self.assertEqual(TaskType.PRODUCT_SEARCH, result.task_type)
        self.assertEqual("苹果", result.candidate_slots.keyword)

    def test_should_fallback_when_chain_fails(self):
        parser = LangChainQueryParser(chain=StubChain(error=RuntimeError("boom")))
        result = parser.parse_request(QueryParserRequest(current_message="现在还有苹果吗"))
        self.assertTrue(result.need_realtime)


class RuleFallbackParserTest(unittest.TestCase):
    def test_should_parse_mixed_search_realtime(self):
        parser = RuleFallbackParser()
        result = parser.parse(QueryParserRequest(current_message="我想买点水果，现在有什么水果在卖"))
        self.assertEqual(TaskType.MIXED_SEARCH_REALTIME, result.task_type)
        self.assertEqual("水果", result.candidate_slots.keyword)
```

- [ ] **Step 2: Run the parser tests to verify they fail first**

Run: `Set-Location python; .\.venv\Scripts\python.exe -m unittest tests.test_rule_fallback tests.test_service -v`
Expected: FAIL with import errors or missing class/function assertions for `RuleFallbackParser` / `LangChainQueryParser`

- [ ] **Step 3: Implement the parser models, prompts, config and fallback logic**

```python
# python/query_parser_langchain/models.py
class ParsedIntent(BaseModel):
    task_type: TaskType = TaskType.CHITCHAT
    intent_confidence: float = 0.0
    query_text: str = ""
    candidate_slots: CandidateSlots = Field(default_factory=CandidateSlots)
    need_realtime: bool = False
    need_explanation: bool = False
    need_recommendation: bool = False
    is_follow_up: bool = False
    is_negation: bool = False
    follow_up_type: FollowUpType | None = None
    negated_entities: list[str] = Field(default_factory=list)

# python/query_parser_langchain/rule_fallback.py
class RuleFallbackParser:
    def parse(self, request: QueryParserRequest) -> ParsedIntent:
        text = (request.current_message or "").strip()
        if not text:
            return ParsedIntent(task_type=TaskType.CHITCHAT, query_text="")
        result = ParsedIntent(query_text=text)
        result.need_realtime = any(token in text for token in ["现在", "今天", "还能", "在卖"])
        result.need_explanation = any(token in text for token in ["规则", "退款", "怎么", "注意事项"])
        result.need_recommendation = any(token in text for token in ["推荐", "找几个", "有什么"])
        result.candidate_slots.keyword = self._extract_keyword(text)
        result.task_type = self._resolve_task_type(text, result)
        return result

# python/query_parser_langchain/service.py
class LangChainQueryParser:
    def __init__(self, chain: Invokable | None = None, fallback_parser: RuleFallbackParser | None = None, chain_timeout_seconds: float = 0.45):
        self._chain = chain
        self._fallback = fallback_parser or RuleFallbackParser()
        self._timeout = chain_timeout_seconds

    def parse_request(self, request: QueryParserRequest) -> ParsedIntent:
        if not (request.current_message or "").strip():
            return ParsedIntent(task_type=TaskType.CHITCHAT, query_text="")
        if self._chain is None:
            return self._fallback.parse(request)
        try:
            with ThreadPoolExecutor(max_workers=1) as executor:
                future = executor.submit(self._invoke_chain, request)
                payload = future.result(timeout=self._timeout)
            return self._coerce_result(payload, request.current_message)
        except Exception:
            return self._fallback.parse(request)
```

- [ ] **Step 4: Expose `/health` and `/parse_intent` in the FastAPI app**

```python
# python/query_parser_langchain/api.py
app = FastAPI(title="query-parser-langchain")
parser = LangChainQueryParser()

@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}

@app.post("/parse_intent", response_model=ParsedIntent)
def parse_intent(request: QueryParserRequest) -> ParsedIntent:
    return parser.parse_request(request)
```

- [ ] **Step 5: Run the parser tests again and confirm they pass**

Run: `Set-Location python; .\.venv\Scripts\python.exe -m unittest tests.test_rule_fallback tests.test_service -v`
Expected: `OK`

- [ ] **Step 6: Commit**

```bash
git add python/query_parser_langchain/models.py python/query_parser_langchain/config.py python/query_parser_langchain/prompt.py python/query_parser_langchain/rule_fallback.py python/query_parser_langchain/service.py python/query_parser_langchain/api.py python/tests/test_rule_fallback.py python/tests/test_service.py
git commit -m "feat(python): restore parse intent sidecar"
```

### Task 3: Rebuild the tool router and fallback execution plans

**Files:**
- Create: `python/query_parser_langchain/tool_router_models.py`
- Create: `python/query_parser_langchain/tool_router_service.py`
- Modify: `python/query_parser_langchain/api.py`
- Test: `python/tests/test_tool_router_service.py`

- [ ] **Step 1: Write the failing tool router tests**

```python
import unittest

from query_parser_langchain.models import ParsedIntent, TaskType
from query_parser_langchain.tool_router_models import ExecutionPolicy, NormalizedParams, SessionContext, ToolRegistry, ToolRouterRequest
from query_parser_langchain.tool_router_service import LangGraphToolRouter


class LangGraphToolRouterTest(unittest.TestCase):
    def test_should_route_focused_entity_realtime_directly(self):
        router = LangGraphToolRouter()
        intent = ParsedIntent(task_type=TaskType.REALTIME_QUERY, query_text="这个现在还能买吗", need_realtime=True)
        request = ToolRouterRequest(
            parsed_intent=intent,
            normalized_params=NormalizedParams(task_type=intent.task_type, keywords="苹果", entity_type="product", execution_ready=True),
            session_context=SessionContext(focused_entity_id="123", focused_entity_type="product"),
            execution_policy=ExecutionPolicy(),
            tool_registry=ToolRegistry(),
        )
        plan = router.route_request(request)
        self.assertEqual("search_then_realtime", plan.plan_type.value)
        self.assertEqual("realtime_query", plan.steps[-1].tool_name.value)

    def test_should_clarify_when_missing_location(self):
        router = LangGraphToolRouter()
        intent = ParsedIntent(task_type=TaskType.STORE_SEARCH, query_text="帮我找门店")
        request = ToolRouterRequest(parsed_intent=intent, normalized_params=NormalizedParams(task_type=intent.task_type, keywords="门店", entity_type="store", execution_ready=False, missing_required_slots=["location"]), session_context=SessionContext(), execution_policy=ExecutionPolicy(), tool_registry=ToolRegistry())
        plan = router.route_request(request)
        self.assertTrue(plan.requires_clarification)
```

- [ ] **Step 2: Run the router tests to verify they fail first**

Run: `Set-Location python; .\.venv\Scripts\python.exe -m unittest tests.test_tool_router_service -v`
Expected: FAIL because `ToolRouterRequest` / `LangGraphToolRouter` or route logic is missing

- [ ] **Step 3: Implement router models and LangGraph routing logic**

```python
# python/query_parser_langchain/tool_router_models.py
class ToolPlan(BaseModel):
    plan_id: str | None = None
    plan_type: PlanType
    execution_mode: str = "single"
    requires_clarification: bool = False
    clarification_prompt: str | None = None
    steps: list[ToolStep] = Field(default_factory=list)
    fallback_plan: "ToolPlan | None" = None
    routing_reason: str | None = None
    plan_confidence: float = 0.0
    max_parallel_tools: int = 1

# python/query_parser_langchain/tool_router_service.py
class LangGraphToolRouter:
    def route_request(self, request: ToolRouterRequest) -> ToolPlan:
        if self._requires_user_clarification(request):
            return self._clarification(request)
        if request.parsed_intent.task_type == TaskType.FAQ_QUERY:
            return self._route_faq(request)
        if request.parsed_intent.need_realtime or request.parsed_intent.task_type == TaskType.REALTIME_QUERY:
            return self._route_search_then_realtime(request)
        if request.parsed_intent.need_explanation or request.parsed_intent.task_type == TaskType.MIXED_SEARCH_KNOWLEDGE:
            return self._route_search_then_knowledge(request)
        return self._route_search_only(request)
```

- [ ] **Step 4: Add the `/route_tools` endpoint to the FastAPI app**

```python
router = LangGraphToolRouter()

@app.post("/route_tools", response_model=ToolPlan)
def route_tools(request: ToolRouterRequest) -> ToolPlan:
    return router.route_request(request)
```

- [ ] **Step 5: Run the router tests again and confirm they pass**

Run: `Set-Location python; .\.venv\Scripts\python.exe -m unittest tests.test_tool_router_service -v`
Expected: `OK`

- [ ] **Step 6: Commit**

```bash
git add python/query_parser_langchain/tool_router_models.py python/query_parser_langchain/tool_router_service.py python/query_parser_langchain/api.py python/tests/test_tool_router_service.py
git commit -m "feat(python): restore tool router sidecar"
```

### Task 4: Rebuild the response composer and align its output contract with Java

**Files:**
- Create: `python/query_parser_langchain/response_composer_models.py`
- Create: `python/query_parser_langchain/response_composer_service.py`
- Modify: `python/query_parser_langchain/api.py`
- Test: `python/tests/test_response_composer_service.py`

- [ ] **Step 1: Write the failing response composer tests**

```python
import unittest

from query_parser_langchain.models import ParsedIntent, TaskType
from query_parser_langchain.response_composer_models import AnswerType, ExecutionMeta, ResponseComposerRequest, SearchItem, SearchResults
from query_parser_langchain.response_composer_service import LangChainResponseComposer
from query_parser_langchain.tool_router_models import PlanType, ToolPlan


class LangChainResponseComposerTest(unittest.TestCase):
    def test_should_build_search_answer_with_cards_and_disclaimers(self):
        composer = LangChainResponseComposer(renderer=None)
        intent = ParsedIntent(task_type=TaskType.PRODUCT_SEARCH, query_text="找苹果", need_recommendation=True)
        request = ResponseComposerRequest(
            parsed_intent=intent,
            tool_plan=ToolPlan(plan_type=PlanType.SINGLE_TOOL, execution_mode="single"),
            search_results=SearchResults(entity_type="product", total=1, returned=1, search_status="success", items=[SearchItem(product_id="1", title="红富士苹果", display_price="5元/斤")]),
            execution_meta=ExecutionMeta(request_id="req-1", trace_id="trace-1", session_id="s-1"),
        )
        answer = composer.compose_request(request)
        self.assertEqual(AnswerType.RECOMMENDATION, answer.answer_type)
        self.assertEqual(1, len(answer.cards))
        self.assertTrue(answer.summary)
        self.assertIn("pre_answer", answer.debug_trace)
```

- [ ] **Step 2: Run the composer tests to verify they fail first**

Run: `Set-Location python; .\.venv\Scripts\python.exe -m unittest tests.test_response_composer_service -v`
Expected: FAIL because `ResponseComposerRequest` / `LangChainResponseComposer` / `summary` contract is not implemented yet

- [ ] **Step 3: Implement response models and the composer service**

```python
# python/query_parser_langchain/response_composer_models.py
class FinalAnswer(BaseModel):
    answer_type: AnswerType
    summary: str | None = None
    answer_text: str
    cards: list[EntityCard] = Field(default_factory=list)
    disclaimers: list[str] = Field(default_factory=list)
    citations: list[Citation] = Field(default_factory=list)
    next_actions: list[str] = Field(default_factory=list)
    composer_meta: dict[str, Any] = Field(default_factory=dict)
    debug_trace: dict[str, Any] = Field(default_factory=dict)

# python/query_parser_langchain/response_composer_service.py
class LangChainResponseComposer:
    def compose_request(self, request: ResponseComposerRequest) -> FinalAnswer:
        draft = self._build_pre_answer(request)
        return FinalAnswer(
            answer_type=draft.answer_type,
            summary=draft.summary,
            answer_text=draft.answer_text,
            cards=draft.cards,
            disclaimers=self._deduplicate(draft.disclaimers),
            citations=self._deduplicate(draft.citations),
            next_actions=self._deduplicate(draft.next_actions),
            composer_meta={
                "used_sources": draft.used_sources,
                "degraded": request.execution_meta.degraded,
            },
            debug_trace=self._build_debug_trace(request, draft),
        )
```

- [ ] **Step 4: Wire `/compose_response` into the FastAPI app with the richer contract**

```python
composer = LangChainResponseComposer()

@app.post("/compose_response", response_model=FinalAnswer)
def compose_response(request: ResponseComposerRequest) -> FinalAnswer:
    return composer.compose_request(request)
```

- [ ] **Step 5: Run the composer tests again and confirm they pass**

Run: `Set-Location python; .\.venv\Scripts\python.exe -m unittest tests.test_response_composer_service -v`
Expected: `OK`

- [ ] **Step 6: Commit**

```bash
git add python/query_parser_langchain/response_composer_models.py python/query_parser_langchain/response_composer_service.py python/query_parser_langchain/api.py python/tests/test_response_composer_service.py
git commit -m "feat(python): restore response composer sidecar"
```

### Task 5: Add Python-side write-action review orchestration

**Files:**
- Create: `python/query_parser_langchain/action_review_models.py`
- Create: `python/query_parser_langchain/action_review_service.py`
- Modify: `python/query_parser_langchain/api.py`
- Test: `python/tests/test_action_review_service.py`
- Test: `python/tests/test_api.py`

- [ ] **Step 1: Write the failing action review tests**

```python
import unittest

from query_parser_langchain.action_review_models import ActionReviewRequest, PendingActionSnapshot
from query_parser_langchain.action_review_service import ActionReviewService
from query_parser_langchain.models import ParsedIntent, TaskType


class ActionReviewServiceTest(unittest.TestCase):
    def test_should_request_missing_fields_for_create(self):
        service = ActionReviewService()
        request = ActionReviewRequest(
            current_message="帮我发布一个商品",
            parsed_intent=ParsedIntent(task_type=TaskType.PRODUCT_SEARCH, query_text="帮我发布一个商品"),
        )
        result = service.review(request)
        self.assertTrue(result.handled)
        self.assertEqual("need_clarification", result.outcome)
        self.assertIn("title", result.missing_fields)

    def test_should_confirm_when_pending_action_is_complete(self):
        service = ActionReviewService()
        request = ActionReviewRequest(
            current_message="确认执行",
            parsed_intent=ParsedIntent(task_type=TaskType.PRODUCT_SEARCH, query_text="确认执行"),
            pending_action=PendingActionSnapshot(action="create", resource="product", payload={"title": "苹果", "price": 3, "stockQuantity": 10, "location": "一食堂", "categoryId": 15, "imageUrls": ["https://img.example.com/a.jpg"]}),
        )
        result = service.review(request)
        self.assertEqual("ready_to_execute", result.outcome)
```

- [ ] **Step 2: Run the action review tests to verify they fail first**

Run: `Set-Location python; .\.venv\Scripts\python.exe -m unittest tests.test_action_review_service -v`
Expected: FAIL because review models and service do not exist yet

- [ ] **Step 3: Implement action review models and non-executing orchestration logic**

```python
# python/query_parser_langchain/action_review_models.py
class PendingActionSnapshot(BaseModel):
    session_id: str | None = None
    resource: str | None = None
    action: str | None = None
    operation_type: str | None = None
    display_name: str | None = None
    route_id: int | None = None
    route_description: str | None = None
    params: dict[str, Any] = Field(default_factory=dict)
    payload: dict[str, Any] = Field(default_factory=dict)
    missing_fields: list[str] = Field(default_factory=list)
    awaiting_confirmation: bool = False

class ActionReviewResponse(BaseModel):
    handled: bool = False
    outcome: str = "none"
    pending_action: PendingActionSnapshot | None = None
    missing_fields: list[str] = Field(default_factory=list)
    executable_payload: dict[str, Any] = Field(default_factory=dict)
    confirmation_message: str | None = None

# python/query_parser_langchain/action_review_service.py
class ActionReviewService:
    def review(self, request: ActionReviewRequest) -> ActionReviewResponse:
        working = self._hydrate_pending_action(request)
        if working is None:
            return ActionReviewResponse(handled=False)
        self._merge_params(working, request.current_message)
        missing = self._missing_fields(working)
        if missing:
            working.missing_fields = missing
            return ActionReviewResponse(handled=True, outcome="need_clarification", pending_action=working, missing_fields=missing)
        if self._is_cancel(request.current_message):
            return ActionReviewResponse(handled=True, outcome="cancelled", pending_action=working)
        if self._is_confirm(request.current_message):
            return ActionReviewResponse(handled=True, outcome="ready_to_execute", pending_action=working, executable_payload={"resource": working.resource, "action": working.action, "params": working.params, "payload": working.payload})
        return ActionReviewResponse(handled=True, outcome="need_confirmation", pending_action=working, confirmation_message=self._build_confirmation_message(working))
```

- [ ] **Step 4: Expose the `/review_action` endpoint and add FastAPI contract coverage**

```python
review_service = ActionReviewService()

@app.post("/review_action", response_model=ActionReviewResponse)
def review_action(request: ActionReviewRequest) -> ActionReviewResponse:
    return review_service.review(request)
```

```python
from fastapi.testclient import TestClient
from query_parser_langchain.api import create_app

client = TestClient(create_app())
response = client.post("/review_action", json={"current_message": "帮我发布一个商品"})
self.assertEqual(200, response.status_code)
self.assertEqual("need_clarification", response.json()["outcome"])
```

- [ ] **Step 5: Run the action review tests again and confirm they pass**

Run: `Set-Location python; .\.venv\Scripts\python.exe -m unittest tests.test_action_review_service tests.test_api -v`
Expected: `OK`

- [ ] **Step 6: Commit**

```bash
git add python/query_parser_langchain/action_review_models.py python/query_parser_langchain/action_review_service.py python/query_parser_langchain/api.py python/tests/test_action_review_service.py python/tests/test_api.py
git commit -m "feat(python): add review action sidecar"
```

### Task 6: Add Java query-parser sidecar transport and Python-first fallback

**Files:**
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Config/QueryParserPythonProperties.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Config/AgentRouterPythonProperties.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Config/AgentComposerPythonProperties.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Config/AgentActionReviewPythonProperties.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonSidecarException.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonSidecarHttpClient.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonSidecarModels.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonSidecarMapper.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonQueryParserClient.java`
- Modify: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Service/QueryParserService.java`
- Modify: `backend/demo1/src/main/resources/application.properties`
- Test: `backend/demo1/src/test/java/com/example/demo/demos/Agent/Python/PythonQueryParserClientTest.java`
- Test: `backend/demo1/src/test/java/com/example/demo/demos/Agent/Service/QueryParserServiceTest.java`

- [ ] **Step 1: Write the failing Java tests for the parser client and Python-first service flow**

```java
@Test
void parseShouldUsePythonClientWhenAvailable() {
    ParsedIntent payload = new ParsedIntent();
    payload.setTaskType(TaskType.PRODUCT_SEARCH);
    when(pythonQueryParserClient.parse("买苹果", Collections.emptyList())).thenReturn(payload);

    ParsedIntent result = service.parse("买苹果", Collections.emptyList());

    assertEquals(TaskType.PRODUCT_SEARCH, result.getTaskType());
    verify(ruleFallbackParser, never()).parse("买苹果");
}

@Test
void parseShouldFallbackWhenPythonClientFails() {
    when(pythonQueryParserClient.parse(anyString(), anyList())).thenThrow(new PythonSidecarException("boom"));
    when(ruleFallbackParser.parse("现在还有苹果吗")).thenReturn(new ParsedIntent());

    service.parse("现在还有苹果吗", Collections.emptyList());

    verify(ruleFallbackParser).parse("现在还有苹果吗");
}
```

```java
@Test
void parseShouldDeserializeSnakeCasePayload() throws Exception {
    server.enqueue(new MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody("{" +
            "\"task_type\":\"product_search\"," +
            "\"intent_confidence\":0.91," +
            "\"query_text\":\"买苹果\"," +
            "\"candidate_slots\":{\"keyword\":\"苹果\"}" +
        "}"));
    ParsedIntent result = client.parse("买苹果", Collections.emptyList());
    assertEquals(TaskType.PRODUCT_SEARCH, result.getTaskType());
}
```

- [ ] **Step 2: Run the Java parser tests to verify they fail first**

Run: `Set-Location backend/demo1; mvn -q "-Dtest=PythonQueryParserClientTest,QueryParserServiceTest" test`
Expected: FAIL because `PythonQueryParserClient` and the Python-first `QueryParserService` branch do not exist yet

- [ ] **Step 3: Implement sidecar property beans, shared HTTP transport, DTOs and the parser client**

```java
@Component
@ConfigurationProperties(prefix = "query.parser.python")
public class QueryParserPythonProperties {
    private boolean enabled = true;
    private String baseUrl = "http://127.0.0.1:9001";
    private String parsePath = "/parse_intent";
    private int connectTimeoutMs = 200;
    private int readTimeoutMs = 650;
}
```

```java
public class PythonSidecarHttpClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper snakeCaseMapper;

    public <T> T post(String baseUrl, String path, Object request, Class<T> responseType) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + path, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new PythonSidecarException("non-success response");
            }
            return snakeCaseMapper.readValue(response.getBody(), responseType);
        } catch (Exception ex) {
            throw new PythonSidecarException("sidecar request failed", ex);
        }
    }
}
```

```java
public class PythonQueryParserClient {
    public ParsedIntent parse(String currentMessage, List<AgentChatMessage> recentMessages) {
        PythonSidecarModels.QueryParserResponse payload = httpClient.post(
                properties.getBaseUrl(),
                properties.getParsePath(),
                PythonSidecarModels.queryParserRequest(currentMessage, recentMessages),
                PythonSidecarModels.QueryParserResponse.class
        );
        return mapper.toParsedIntent(payload);
    }
}
```

- [ ] **Step 4: Modify `QueryParserService` to call Python first and fall back to the current Java logic**

```java
public ParsedIntent parse(String currentMessage, List<AgentChatMessage> recentMessages) {
    if (!StringUtils.hasText(currentMessage)) {
        ParsedIntent empty = new ParsedIntent();
        empty.setTaskType(TaskType.CHITCHAT);
        empty.setQueryText("");
        empty.setIntentConfidence(0.0);
        return empty;
    }

    if (pythonQueryParserClient.isEnabled()) {
        try {
            return pythonQueryParserClient.parse(currentMessage, recentMessages);
        } catch (PythonSidecarException ex) {
            log.warn("Python parse_intent unavailable, fallback to Java parser: {}", ex.getMessage());
        }
    }

    return parseWithLocalLlmOrRuleFallback(currentMessage, recentMessages);
}
```

```properties
agent.action-review.python.enabled=true
agent.action-review.python.base-url=http://127.0.0.1:9001
agent.action-review.python.review-path=/review_action
agent.action-review.python.connect-timeout-ms=200
agent.action-review.python.read-timeout-ms=900
```

- [ ] **Step 5: Run the Java parser tests again and confirm they pass**

Run: `Set-Location backend/demo1; mvn -q "-Dtest=PythonQueryParserClientTest,QueryParserServiceTest" test`
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add backend/demo1/src/main/java/com/example/demo/demos/Agent/Config backend/demo1/src/main/java/com/example/demo/demos/Agent/Python backend/demo1/src/main/java/com/example/demo/demos/Agent/Service/QueryParserService.java backend/demo1/src/main/resources/application.properties backend/demo1/src/test/java/com/example/demo/demos/Agent/Python/PythonQueryParserClientTest.java backend/demo1/src/test/java/com/example/demo/demos/Agent/Service/QueryParserServiceTest.java
git commit -m "feat(agent): add python-first parser client"
```

### Task 7: Connect Java runtime to Python router, composer and action review with node-level fallback

**Files:**
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonToolRouterClient.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonResponseComposerClient.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Python/PythonActionReviewClient.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/SidecarToolPlanAdapter.java`
- Create: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/SidecarActionReviewCoordinator.java`
- Modify: `backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/AgentRuntime.java`
- Modify: `backend/demo1/src/main/resources/application.properties`
- Test: `backend/demo1/src/test/java/com/example/demo/demos/Agent/Python/PythonToolRouterClientTest.java`
- Test: `backend/demo1/src/test/java/com/example/demo/demos/Agent/Python/PythonResponseComposerClientTest.java`
- Test: `backend/demo1/src/test/java/com/example/demo/demos/Agent/Python/PythonActionReviewClientTest.java`
- Test: `backend/demo1/src/test/java/com/example/demo/demos/Agent/Runtime/AgentRuntimeTest.java`

- [ ] **Step 1: Write the failing runtime integration and remaining client tests**

```java
@Test
void runShouldUsePythonRouteAndComposerWhenAvailable() {
    when(queryParserService.parse(anyString(), anyList())).thenReturn(parsedIntent(TaskType.PRODUCT_SEARCH));
    when(pythonToolRouterClient.route(any())).thenReturn(pythonSearchOnlyPlan());
    when(pythonResponseComposerClient.compose(any())).thenReturn(pythonRecommendationAnswer());

    SessionState state = agentRuntime.run(request("找苹果"), null);

    assertEquals(AnswerType.RECOMMENDATION, state.getFinalAnswer().getAnswerType());
    verify(pythonToolRouterClient).route(any());
    verify(pythonResponseComposerClient).compose(any());
}

@Test
void runShouldFallbackToLocalRuntimePlanWhenPythonRouterFails() {
    when(queryParserService.parse(anyString(), anyList())).thenReturn(parsedIntent(TaskType.PRODUCT_SEARCH));
    when(pythonToolRouterClient.route(any())).thenThrow(new PythonSidecarException("timeout"));

    SessionState state = agentRuntime.run(request("找苹果"), null);

    assertTrue(state.getExecutionMeta().getCompletedNodes().contains("route_tools"));
    verify(productSearchSnapshotMapper).searchForProducts(any());
}
```

- [ ] **Step 2: Run the runtime/client tests to verify they fail first**

Run: `Set-Location backend/demo1; mvn -q "-Dtest=PythonToolRouterClientTest,PythonResponseComposerClientTest,PythonActionReviewClientTest,AgentRuntimeTest" test`
Expected: FAIL because the three runtime clients and sidecar adapters do not exist yet

- [ ] **Step 3: Implement the remaining sidecar clients and runtime adapters**

```java
public class SidecarToolPlanAdapter {
    public RuntimePlan adapt(PythonSidecarModels.ToolPlanResponse payload) {
        RuntimePlan plan = new RuntimePlan();
        plan.setRunSearch(hasStep(payload, "structured_search"));
        plan.setRunKnowledge(hasStep(payload, "knowledge_retrieval"));
        plan.setRunRealtime(hasStep(payload, "realtime_query"));
        plan.setPlanCode(payload.getPlanType());
        return plan;
    }
}
```

```java
public class SidecarActionReviewCoordinator {
    public ActionIntentReviewService.ActionReviewResult apply(String sessionId,
                                                              PythonSidecarModels.ActionReviewResponse payload,
                                                              String authorization) {
        if (!payload.isHandled()) {
            return new ActionIntentReviewService.ActionReviewResult();
        }
        ActionConversationStore.PendingAction pending = mapper.toPendingAction(sessionId, payload.getPendingAction());
        if ("cancelled".equals(payload.getOutcome())) {
            actionConversationStore.clear(sessionId);
            return cancelled(pending);
        }
        if (!payload.getMissingFields().isEmpty()) {
            actionConversationStore.put(sessionId, pending);
            return clarification(pending);
        }
        if ("ready_to_execute".equals(payload.getOutcome())) {
            BackendApiProxyService.InvocationResult invocation = backendApiProxyService.invoke(mapper.toInvocationRequest(payload), authorization);
            actionConversationStore.clear(sessionId);
            return executed(pending, invocation);
        }
        actionConversationStore.put(sessionId, pending);
        return confirmation(pending);
    }
}
```

- [ ] **Step 4: Modify `AgentRuntime` so each node tries Python first and falls back locally per node**

```java
private RuntimePlan traceRouteTools(ParsedIntent parsedIntent, RuntimeRequest normalized, SessionState state, SessionState.ExecutionMeta executionMeta, FinalAnswer.DebugTrace debugTrace) {
    try {
        PythonSidecarModels.ToolPlanResponse payload = pythonToolRouterClient.route(mapper.toToolRouterRequest(parsedIntent, normalized, state));
        RuntimePlan plan = sidecarToolPlanAdapter.adapt(payload);
        state.getIntermediateData().put("routePlan", plan.getPlanCode());
        executionMeta.getCompletedNodes().add("route_tools");
        return plan;
    } catch (PythonSidecarException ex) {
        log.warn("Python route_tools fallback to local runtime plan: {}", ex.getMessage());
        return routePlan(parsedIntent, normalized);
    }
}

private FinalAnswer traceComposeResponse(AgentChatMessage latestMessage,
                                         ParsedIntent parsedIntent,
                                         RuntimeRequest normalized,
                                         RuntimePlan plan,
                                         RuntimeExecution execution,
                                         SessionState state,
                                         SessionState.ExecutionMeta executionMeta) {
    try {
        return pythonResponseComposerClient.compose(mapper.toComposeRequest(parsedIntent, normalized, plan, execution, state, executionMeta));
    } catch (PythonSidecarException ex) {
        log.warn("Python compose_response fallback to RuntimeAnswerComposer: {}", ex.getMessage());
        if (shouldComposeChitchat(parsedIntent, latestMessage)) {
            return runtimeAnswerComposer.buildChitchatAnswer(latestMessage, parsedIntent);
        }
        if (execution.getSearchTotal() > 0) {
            return runtimeAnswerComposer.buildProductSearchAnswer(execution.getSearchResults(), execution.getSearchTotal(), normalized.getProductQuery(), latestMessage, parsedIntent);
        }
        return runtimeAnswerComposer.buildNoResultAnswer(latestMessage, parsedIntent, plan.getPlanCode(), ex.getMessage());
    }
}
```

- [ ] **Step 5: Run the runtime/client tests again and confirm they pass**

Run: `Set-Location backend/demo1; mvn -q "-Dtest=PythonToolRouterClientTest,PythonResponseComposerClientTest,PythonActionReviewClientTest,AgentRuntimeTest" test`
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add backend/demo1/src/main/java/com/example/demo/demos/Agent/Python backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/SidecarToolPlanAdapter.java backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/SidecarActionReviewCoordinator.java backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/AgentRuntime.java backend/demo1/src/main/resources/application.properties backend/demo1/src/test/java/com/example/demo/demos/Agent/Python backend/demo1/src/test/java/com/example/demo/demos/Agent/Runtime/AgentRuntimeTest.java
git commit -m "feat(agent): route runtime through python sidecar"
```

### Task 8: Run full verification, document startup flow, and finish the recovery

**Files:**
- Create: `python/tests/test_api.py`
- Create: `python/README.md`
- Modify: `backend/demo1/src/test/java/com/example/demo/demos/Agent/Runtime/AgentRuntimeTest.java`

- [ ] **Step 1: Add FastAPI contract smoke tests and a Python runbook**

```python
import unittest
from fastapi.testclient import TestClient
from query_parser_langchain.api import create_app


class ApiContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.client = TestClient(create_app())

    def test_health(self):
        response = self.client.get("/health")
        self.assertEqual(200, response.status_code)
        self.assertEqual("ok", response.json()["status"])

    def test_parse_intent_contract(self):
        response = self.client.post("/parse_intent", json={"current_message": "买苹果"})
        self.assertEqual(200, response.status_code)
        self.assertIn("task_type", response.json())
```

```markdown
# Python Sidecar Runbook

## Start
Set-Location python
.\.venv\Scripts\python.exe -m uvicorn query_parser_langchain.api:app --host 127.0.0.1 --port 9001

## Test
.\.venv\Scripts\python.exe -m unittest discover -s tests -p "test_*.py" -v

## Contract
- Python 只负责编排和回答拼装，不直接执行后端写接口。
- Java 保留搜索、实时查询、知识检索和写操作落库能力。
- sidecar 失败时，Java 自动走本地 fallback。
```

- [ ] **Step 2: Run the full Python test suite**

Run: `Set-Location python; .\.venv\Scripts\python.exe -m unittest discover -s tests -p "test_*.py" -v`
Expected: all parser/router/composer/action/api tests pass with `OK`

- [ ] **Step 3: Run the Java regression suite that covers the restored intelligent-search path**

Run: `Set-Location backend/demo1; mvn -q "-Dtest=AgentRuntimeTest,RuntimeAnswerComposerTest,KnowledgeSearchServiceTest,RealtimeQueryOrchestratorServiceTest,QueryParserServiceTest,PythonQueryParserClientTest,PythonToolRouterClientTest,PythonResponseComposerClientTest,PythonActionReviewClientTest" test`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Smoke-test the Python sidecar endpoints directly**

Run: `Set-Location python; .\.venv\Scripts\python.exe -m uvicorn query_parser_langchain.api:app --host 127.0.0.1 --port 9001`
Expected: server starts on `http://127.0.0.1:9001`

Run: `curl.exe -X POST http://127.0.0.1:9001/parse_intent -H "Content-Type: application/json" -d "{\"current_message\":\"我想买点水果，现在有什么水果在卖\"}"`
Expected: JSON with `task_type=mixed_search_realtime`

Run: `curl.exe -X POST http://127.0.0.1:9001/review_action -H "Content-Type: application/json" -d "{\"current_message\":\"帮我发布一个商品\"}"`
Expected: JSON with `outcome=need_clarification`

- [ ] **Step 5: Smoke-test the Java backend with the sidecar running**

Run: `Set-Location backend/demo1; mvn spring-boot:run`
Expected: backend listens on `http://127.0.0.1:8080`

Run: `curl.exe -X POST http://127.0.0.1:8080/api/agent/chat -H "Content-Type: application/json" -d "{\"sessionId\":\"smoke-search-1\",\"userId\":\"u-1\",\"messages\":[{\"role\":\"user\",\"content\":\"找几个苹果\"}],\"debug\":true}"`
Expected: recommendation answer with cards

Run: `curl.exe -X POST http://127.0.0.1:8080/api/agent/chat -H "Content-Type: application/json" -d "{\"sessionId\":\"smoke-realtime-1\",\"userId\":\"u-1\",\"messages\":[{\"role\":\"user\",\"content\":\"商品 id 1 现在还能买吗\"}],\"debug\":true}"`
Expected: realtime confirmation or degraded partial result, but not a 500

Run: `curl.exe -X POST http://127.0.0.1:8080/api/agent/chat -H "Content-Type: application/json" -d "{\"sessionId\":\"smoke-action-1\",\"userId\":\"u-1\",\"messages\":[{\"role\":\"user\",\"content\":\"发布商品，商品名叫苹果手机，价格3元，库存10件，地点一食堂，图片https://img.example.com/apple.jpg\"}],\"debug\":true}"`
Expected: clarification or confirmation answer, and follow-up `确认执行` can触发 Java 真正执行

- [ ] **Step 6: Commit**

```bash
git add python/tests/test_api.py python/README.md
git commit -m "docs: add python sidecar runbook and verification checklist"
```


## 实施完成情况（2026-04-12）

- 已恢复 Python sidecar 运行时源码：`query_parser_langchain`、FastAPI `api.py`、四类服务与测试。
- 已完成 Java 侧桥接：`PythonQueryParserClient`、`PythonToolRouterClient`、`PythonResponseComposerClient`、`PythonActionReviewClient`。
- 实际落地形态为：
  - `parse_intent`：Python 优先，Java / rules 兜底
  - `route_tools`：Python 优先，Java 兜底
  - `compose_response`：Python 优先，Java `RuntimeAnswerComposer` 兜底
  - `review_action`：Python 优先，Java `ActionIntentReviewService` 本地识别与真实后端执行兜底
- 与原计划相比，`review_action` 没有新增独立 `SidecarActionReviewCoordinator`，而是直接收口到现有 `ActionIntentReviewService`，以减少额外编排层。
- 已完成验证：
  - Java 定向测试：`PythonQueryParserClientTest`、`PythonToolRouterClientTest`、`PythonResponseComposerClientTest`、`PythonActionReviewClientTest`、`QueryParserServiceTest`、`AgentRuntimeTest`
  - Python 定向测试：`test_action_review_service`、`test_api`、`test_response_composer_service`
  - 2026-04-12 本地真实 smoke：结构化检索、写操作补参数、实时查询
- 当前剩余事项已不属于 sidecar 恢复本身，而是外部环境联通、上线执行与归档。

