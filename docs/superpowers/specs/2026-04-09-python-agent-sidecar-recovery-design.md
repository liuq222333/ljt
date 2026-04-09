# Python Agent Sidecar Recovery Design

## 概述

本设计用于把当前项目的智能检索主链恢复到“Python 负责编排、Java 负责执行与兜底”的目标形态，并清理 `D:\code\aaaaljt\python` 目录下与业务主链无关的脚本布局。

当前仓库里，Java 主链已经可以运行，但设计文档、配置和目录结构仍保留了 Python LangChain / LangGraph 主链的口径；同时 `D:\code\aaaaljt\python\query_parser_langchain` 与 `D:\code\aaaaljt\python\tests` 只剩 `__pycache__`，源码缺失，导致架构口径与代码真相不一致。本设计选择恢复完整 Python sidecar，并采用“Python 优先，Java 兜底”的方式重新接回主链。

## 已确认决策

- Python 主链采用独立 FastAPI sidecar 形态运行
- Java 接回 Python 后采用“Python 优先，Java 兜底”
- Python 主链行为优先对齐原设计文档，而不是优先模仿当前 Java 输出
- Python 覆盖范围包含智能检索链路和写操作编排链路
- 写操作由 Python 负责识别、补参、确认和编排，真正执行仍由 Java 发起
- `D:\code\aaaaljt\python` 根目录下的图表生成脚本不删除，统一归档到工具目录

## 目标

1. 恢复 `D:\code\aaaaljt\python\query_parser_langchain` 的可维护源码和测试源码
2. 提供可运行的 Python sidecar，统一承接 `parse_intent`、`route_tools`、`compose_response`、`review_action`
3. 把 Java 主链调整为 Python 优先调用，并在 Python 不可用时自动回退到现有 Java 实现
4. 重新实现与设计文档一致的“编排层 / 执行层”边界
5. 清理 `D:\code\aaaaljt\python` 根目录，使业务主链代码与图表工具代码分层清晰

## 非目标

- 不把商品搜索、知识检索、实时查询或后端写接口的真正执行迁移到 Python
- 不移除现有 Java 的执行能力、回答拼装能力和写操作确认能力；这些能力保留作为 fallback
- 不在本轮改造中重写治理台、Launch、同步补偿等 W12-W14 代码
- 不把图表生成脚本改造成自动构建流程的一部分，本轮只做归档和说明

## 当前状态

### 当前有效主链

当前运行中的真实主链为 Java 主导：

- 入口：`D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\Agent\Controller\AgentChatController.java`
- facade：`D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\Agent\Service\Impl\AgentChatServiceImpl.java`
- runtime：`D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\Agent\Runtime\AgentRuntime.java`
- parser：`D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\Agent\Service\QueryParserService.java`
- composer：`D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\Agent\Runtime\RuntimeAnswerComposer.java`
- action review：`D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\Agent\Runtime\ActionIntentReviewService.java`

Java 侧已经具备：

- `parse_intent -> normalize_params -> route_tools -> execute -> compose_response`
- `action_review -> action_execute`
- 商品搜索、知识检索、实时查询、写操作确认和执行

### 当前失配点

- `D:\code\aaaaljt\python\query_parser_langchain` 无 Python 源码，仅有 `__pycache__`
- `D:\code\aaaaljt\python\tests` 无测试源码，仅有 `__pycache__`
- `D:\code\aaaaljt\backend\demo1\src\main\resources\application.properties` 中保留了 Python parser/router/composer 配置，但 Java 主链没有完整消费这些配置
- `D:\code\aaaaljt\python` 根目录下存在多份 draw.io 图表生成脚本，与业务主链混放

## 方案对比

### 方案 A：完整恢复 Python 主链

恢复 Python parser / router / composer / action review，并让 Java 改为 Python 优先、Java 兜底。

优点：

- 与原设计文档最一致
- 重新建立“Python 编排、Java 执行”的清晰边界
- 后续 LangChain / LangGraph 迭代有明确落点

缺点：

- 改动面最大
- 需要补全 Python 包、测试、Java client 和 fallback 接线

### 方案 B：半恢复 Python

只恢复 parser/router，composer 和 action review 继续放在 Java。

优点：

- 风险较低
- 切换较快

缺点：

- 与已确认目标不一致
- 长期仍然是双中心实现

### 方案 C：影子恢复

Python 主链先补齐，但先不切主链，只做双跑对比。

优点：

- 切换风险最低

缺点：

- 周期长
- 与用户当前希望的完整恢复目标不一致

### 选型结论

采用方案 A，但实施顺序按阶段推进：先恢复 Python 包和测试，再接 parser / router / composer，最后接 action review。整个过程中 Java 执行层与 fallback 全程保留。

## 目标架构

### 架构分层

- Python sidecar：Agent 编排层
  - 负责意图解析
  - 负责工具路由
  - 负责写操作识别、补参、确认态推进和执行计划生成
  - 负责最终回答拼装
- Java backend：业务能力层 + 执行层
  - 负责商品搜索执行
  - 负责知识检索执行
  - 负责实时查询执行
  - 负责写操作真正调用后端接口
  - 负责 fallback 逻辑

### 最终主链

最终主链形态为：

`parse_intent(Python 优先) -> route_tools / review_action(Python 优先) -> execute(Java) -> compose_response(Python 优先)`

### 回退原则

- Python sidecar 不可达、超时、返回非法结构、返回不满足契约的数据时，Java 自动切回本地实现
- Java 执行结果始终是业务真相，Python 不直接拥有写执行权

## Python 侧设计

### 目录结构

`D:\code\aaaaljt\python\query_parser_langchain` 恢复为以下结构：

- `__init__.py`
- `api.py`
- `config.py`
- `models.py`
- `prompt.py`
- `rule_fallback.py`
- `service.py`
- `tool_router_models.py`
- `tool_router_service.py`
- `response_composer_models.py`
- `response_composer_service.py`
- `action_review_models.py`
- `action_review_service.py`

### 接口设计

Python sidecar 暴露以下接口：

- `GET /health`
- `POST /parse_intent`
- `POST /route_tools`
- `POST /compose_response`
- `POST /review_action`

### Python 职责

#### `/parse_intent`

输入：

- 当前消息
- 最近消息列表
- 可选 session 摘要

输出：

- `taskType`
- `intentConfidence`
- `candidateSlots`
- `needRealtime`
- `needExplanation`
- `needRecommendation`
- `followUp`
- `followUpType`
- `negation`

#### `/route_tools`

输入：

- `parsedIntent`
- 归一化后的请求摘要
- 会话上下文摘要

输出：

- 路由计划
- 是否需要澄清
- 预计执行步骤
- route plan code

#### `/compose_response`

输入：

- `parsedIntent`
- 路由计划
- Java execute 层返回的搜索结果、知识结果、实时结果、错误状态、执行元数据

输出：

- 最终回答对象草稿
- summary / answerText / cards / citations / disclaimers / nextActions / metadata

#### `/review_action`

输入：

- 当前消息
- `parsedIntent`
- Java 提供的当前 `PendingAction` 状态快照
- 可用 action route 摘要

输出：

- 是否识别为写操作
- 动作资源与动作名
- 缺失字段
- 是否进入确认态
- 是否可以执行
- 准备交给 Java 的执行 payload 草稿

### 状态原则

Python 不保存写操作权威状态。写操作会话状态的权威存储仍然是 Java `ActionConversationStore`。Python 只基于请求、上下文和当前状态快照，产出新的动作编排结果，避免双状态源。

## Java 侧设计

### 保留的执行能力

以下能力继续由 Java 执行，不迁移到 Python：

- `ProductSearchSnapshotMapper`
- `KnowledgeSearchService`
- `RealtimeQueryOrchestratorService`
- `BackendApiProxyService`

### 需要新增的适配器

在 `D:\code\aaaaljt\backend\demo1\src\main\java\com\example\demo\demos\Agent` 下新增 Python sidecar client：

- `PythonQueryParserClient`
- `PythonToolRouterClient`
- `PythonResponseComposerClient`
- `PythonActionReviewClient`

这些 client 负责：

- 读取现有 `application.properties` 中的 Python 配置
- 发起 HTTP 请求
- 做超时、错误码、结构校验
- 统一抛出可回退异常

### Java 主链改造点

#### `QueryParserService`

- 调整为 Python 优先
- 调用 `PythonQueryParserClient`
- Python 失败时回退到当前 Java LLM + `RuleFallbackParser`

#### `AgentRuntime`

- 结构保留，但不再由本地逻辑主导编排
- 路由优先调用 `PythonToolRouterClient`
- 写操作 review 优先调用 `PythonActionReviewClient`
- 回答拼装优先调用 `PythonResponseComposerClient`
- `execute` 仍继续使用当前 Java 搜索 / 知识 / 实时 / 写接口执行逻辑

#### `RuntimeAnswerComposer`

- 保留为 Java fallback composer
- 不再作为默认主链实现

#### `ActionIntentReviewService`

- 保留为 Java fallback action reviewer
- 在 Python `review_action` 不可用时接管

## 数据流

### 检索链路

1. 前端请求进入 `AgentChatController`
2. Java facade 调用 `AgentRuntime`
3. Java 优先请求 Python `/parse_intent`
4. Java 优先请求 Python `/route_tools`
5. Java 按 route plan 执行搜索 / 知识 / 实时
6. Java 优先请求 Python `/compose_response`
7. 如果任一 Python 节点失败，则该节点级别回退到 Java 实现

### 写操作链路

1. 前端请求进入 `AgentRuntime`
2. Java 获取当前 `PendingAction` 快照
3. Java 优先请求 Python `/review_action`
4. Python 返回：
   - 缺参
   - 确认态
   - 可执行计划
   - 取消
5. Java 根据 Python 结果更新 `ActionConversationStore`
6. 真正执行时由 Java `BackendApiProxyService` 发起
7. 最终执行结果交给 Python `/compose_response` 或 Java fallback composer

## 失败与回退策略

### 回退触发条件

以下情况触发节点级 fallback：

- sidecar 不可达
- sidecar 超时
- 返回 5xx
- 返回 2xx 但结构不合法
- 返回缺少必填字段

### 回退行为

- parser 回退到 `QueryParserService` 本地解析
- route 回退到 `AgentRuntime` 当前本地 `routePlan`
- action review 回退到 `ActionIntentReviewService`
- compose 回退到 `RuntimeAnswerComposer`

### 一致性原则

- Java execute 结果是最终事实来源
- Python 不能绕开 Java 直接写业务数据
- Python 不能成为唯一状态源

## 图表脚本清理

### 需要归档的脚本

以下脚本从 `D:\code\aaaaljt\python` 根目录迁移到工具目录：

- `generate_access_business_notification_architecture_drawio.py`
- `generate_backend_api_model_branch_flow_drawio.py`
- `generate_backend_api_tool_flow_drawio.py`
- `generate_chen_er_drawio.py`
- `generate_class_drawio_from_mmd.py`
- `generate_experiment_validation_flow_drawio.py`
- `generate_flow_drawio.py`
- `generate_frontend_backend_infra_architecture_drawio.py`
- `generate_notification_architecture_drawio.py`

### 新目录

迁移到：

`D:\code\aaaaljt\python\tools\diagram_generators`

新增：

- `D:\code\aaaaljt\python\tools\diagram_generators\README.md`

README 说明：

- 这些脚本仅用于生成 `D:\code\aaaaljt\docs\diagrams` 下的图资产
- 不属于业务主链
- 不参与主服务启动

## 迁移顺序

### 阶段 1：恢复 Python 包和测试

- 恢复 `query_parser_langchain` 源码
- 恢复 `python/tests` 源码
- 能本地启动 FastAPI 和跑单测

### 阶段 2：接回 parser

- Java parser 改为 Python 优先
- 验证 fallback 生效

### 阶段 3：接回 router 和 composer

- Java runtime 接 `/route_tools`
- Java runtime 接 `/compose_response`

### 阶段 4：接回 action review

- Python `/review_action`
- Java `ActionConversationStore` 一致性联调
- 执行前确认链路回归

### 阶段 5：归档图表脚本

- 脚本迁移到工具目录
- 根目录保留主链相关目录

## 测试策略

### Python 单测

- parser：正常解析、低置信度、fallback、chitchat、follow-up
- router：search / knowledge / realtime / mixed / clarification / action
- composer：search-only、knowledge-only、search+knowledge、search+realtime、degraded
- action review：缺参、确认、取消、执行计划生成

### Python 接口测试

- `/health`
- `/parse_intent`
- `/route_tools`
- `/compose_response`
- `/review_action`

### Java 单测

- Python client 成功路径
- timeout / 5xx / 非法结构 fallback
- `AgentRuntime` 在 Python enabled / disabled 模式下行为一致

### 集成测试

- 搜索链路
- 知识链路
- 实时链路
- 搜索 + 知识
- 搜索 + 实时
- 写操作确认流
- Python 故障回退流

## 验收标准

- Python sidecar 可独立启动并返回健康检查
- Java 在 Python 正常时优先使用 Python parser / router / composer / action review
- Python 不可用时，Java 自动回退且主链可继续工作
- 检索、知识、实时和写操作确认流都通过回归
- `D:\code\aaaaljt\python` 根目录不再混放图表生成脚本
- 设计文档、配置和代码主链重新对齐

## 风险与约束

- 最大风险点是写操作确认流的状态一致性
- 第二风险点是 Python composer 与 Java `FinalAnswer` 契约对齐
- 第三风险点是 Python 行为更贴近设计文档后，可能与当前 Java 回答风格存在回归差异

本设计通过“Python 优先，Java 兜底”和“Java 持有权威状态、Python 只做编排”的原则控制这些风险。

## 结论

本方案采用完整恢复 Python sidecar 的方式，把当前 Java 可运行主链重新收口为设计文档中的目标形态：Python 负责理解和编排，Java 负责执行和兜底。这样既能恢复架构一致性，又不会牺牲现有系统的稳定性。
