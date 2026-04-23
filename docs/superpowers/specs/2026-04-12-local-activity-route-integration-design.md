# Local Activity Route Integration Design

## Goal

让聊天主链支持“查活动”的本地生活查询，并且统一走数据库 `api_routes` 执行链，而不是在 runtime 里直接调用本地生活 service。

本批范围只做只读能力：
- 当前有哪些活动
- 最近有什么活动
- 我附近有什么活动

本批不做：
- 创建活动
- 报名活动
- 活动管理/审核
- 复杂时间过滤

## Current Context

当前 `agent/chat` 的可执行动作链已经支持 `product` 写操作，但 `local_activity` 还没有进入 `action review` 执行链。

现有本地生活后端接口已经存在：
- `/api/local-act/activities/list`
- `/api/local-act/activities/nearby`
- `/api/local-act/heatmap`

现有数据库 route 机制已经存在：
- `api_routes` 保存 `resource/action/http_method/path_template/operation_type/query_schema/body_schema`
- `BackendApiProxyService` 负责按 route 转发 HTTP 调用

现状问题：
- 聊天入口不能识别活动查询并走 `api_routes`
- `ActionIntentReviewService` 目前只硬编码了 `product` 五类动作
- 若直接把活动逻辑继续堆进 `ActionIntentReviewService`，后续扩展会继续恶化

## Recommended Approach

采用“半动态 + 轻适配器”方案：

1. 保留现有 `AgentRuntime -> ActionIntentReviewService -> BackendApiProxyService` 主执行链。
2. 新增 `LocalActivityActionAdapter`，只负责：
   - 活动查询信号识别
   - `list` / `nearby` 判定
   - 参数抽取
   - route 候选筛选
   - 结果摘要化
3. `ActionIntentReviewService` 只做路由分发：
   - `product` 仍走现有逻辑
   - `local_activity` 只读查询交给新适配器
4. 最终仍通过 `BackendApiProxyService` 调用数据库 route 对应接口

这样做的原因：
- 先接通本地生活，不推翻当前 action 主链
- 保持 route 执行统一入口
- 为后续 `local_activity` 写操作留出适配器边界

## Data Flow

### Case A: 当前有哪些活动

1. 用户消息进入 `agent/chat`
2. 通用意图层运行
3. `ActionIntentReviewService` 调用 `LocalActivityActionAdapter.canHandle(...)`
4. 识别为 `resource=local_activity, action=list`
5. 从 `api_routes` 查询 `local_activity + READ` 候选并选中 `list`
6. 组装 GET 请求参数：
   - `status=PUBLISHED`
   - `page=1`
   - `size=10`
7. `BackendApiProxyService` 调用 `/api/local-act/activities/list`
8. 适配器把返回结果摘要化为活动列表回答

### Case B: 我附近有什么活动

1. 用户消息进入 `agent/chat`
2. 通用意图层运行
3. `ActionIntentReviewService` 调用 `LocalActivityActionAdapter.canHandle(...)`
4. 识别为 `resource=local_activity, action=nearby`
5. 抽取参数：
   - `lat/lon`
   - `radiusKm`
   - `keyword`
   - `category`
6. 若缺 `lat/lon`：
   - 优先从 `AgentChatRequest.userProfile` 或会话上下文补
   - 补不到则返回澄清，不执行接口
7. 从 `api_routes` 查询 `local_activity + READ` 候选并选中 `nearby`
8. `BackendApiProxyService` 调用 `/api/local-act/activities/nearby`
9. 适配器把结果摘要化为附近活动回答

## Route Requirements

本批要求 `api_routes` 至少存在两条启用记录：

### local_activity/list
- `resource=local_activity`
- `action=list`
- `operation_type=READ`
- `http_method=GET`
- `path_template=/api/local-act/activities/list`

### local_activity/nearby
- `resource=local_activity`
- `action=nearby`
- `operation_type=READ`
- `http_method=GET`
- `path_template=/api/local-act/activities/nearby`

建议同时补齐：
- `description`
- `query_schema`

## Code Changes

### New
- `backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/LocalActivityActionAdapter.java`
  - 只负责 local_activity 只读查询识别、参数抽取、route 选择、结果格式化

### Modify
- `backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/ActionIntentReviewService.java`
  - 接入 `LocalActivityActionAdapter`
  - 让活动查询走 adapter，而不是继续硬编码到 `product` 分支

- `backend/demo1/src/main/java/com/example/demo/demos/Agent/Runtime/RuntimeAnswerComposer.java`
  - 如有必要，仅补最小的活动摘要展示支持

- 测试文件
  - `backend/demo1/src/test/java/com/example/demo/demos/Agent/Runtime/AgentRuntimeTest.java`
  - 新增 `LocalActivityActionAdapterTest.java`

## Matching Strategy

匹配分两层：

1. 语义识别层
   - 关键词：`活动`、`同城活动`、`附近活动`、`最近活动`
   - 附近信号：`附近`、`周边`、`离我近`

2. 数据库 route 层
   - `resource=local_activity`
   - `operation_type=READ`
   - 由 adapter 决定偏向 `list` 还是 `nearby`

这样做的目的是避免：
- 直接把自然语言全部交给 `api_routes` 生匹配，导致误判率高
- 继续把所有活动逻辑塞进 `ActionIntentReviewService`

## Error Handling

- 没有活动 route：返回“当前未配置可用活动查询接口”
- `nearby` 缺位置：返回“还需要你提供当前位置”
- 后端 route 调用失败：返回活动查询失败，并带 route 错误摘要
- 活动结果为空：返回明确中文“当前没有命中的活动”

## Testing

需要覆盖：
- `当前有哪些活动` -> 命中 `local_activity/list`
- `最近有什么活动` -> 命中 `local_activity/list`
- `我附近有什么活动` 且带位置 -> 命中 `local_activity/nearby`
- `我附近有什么活动` 但无位置 -> 返回澄清
- route 缺失 -> 返回配置缺失提示
- adapter 不应抢占普通商品搜索和 product 写操作

## Decision

本批采用“数据库 route + 轻量 local_activity adapter”的方式接入，不做全动态重构，不做活动写操作。
