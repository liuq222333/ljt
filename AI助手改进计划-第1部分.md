# AI 助手调用准确率提升计划

## 📋 项目背景

### 当前问题
AI 助手在调用后端接口时错误率较高，主要表现为：
1. 工具选择错误
2. 参数传递错误
3. 参数类型/格式错误
4. 缺少必填参数

### 现有架构分析

**当前实现**：
- 单一工具：`backend_api`
- 参数结构：`{resource, action, pathVariables, params, payload, authorization}`
- 工具定义：抽象的通用描述
- 路由配置：存储在 `api_route` 数据库表

**核心问题**：
1. **工具定义过于抽象** - AI 需要理解 resource + action 的组合概念
2. **缺少上下文** - AI 不知道有哪些可用的 resource 和 action
3. **参数结构复杂** - 三层嵌套结构（pathVariables/params/payload）
4. **错误反馈不明确** - 失败时无法给出具体的修正建议

---

## 🎯 改进目标

1. **提高工具调用准确率**：从当前 60% 提升到 85%+
2. **降低参数错误率**：减少 70% 的参数相关错误
3. **改善用户体验**：减少 AI 重试次数，提高响应速度
4. **增强可维护性**：便于添加新接口和调试问题

---

## 💡 改进方案

### 方案1：动态生成具体工具定义（核心方案）

**优先级**：⭐⭐⭐⭐⭐ 高

**改进思路**：
将数据库中的每个 API 路由转换为独立的工具定义，让 AI 直接看到所有可用接口。

**对比**：
```
【原来】
- 1个工具：backend_api
- AI 需要知道：resource="product", action="list_all"

【改进后】
- N个工具：product_list_all, product_get_by_id, activity_nearby...
- AI 直接选择：product_list_all
```

**实现要点**：

1. **修改 `AgentToolHandler.getToolDefinitions()`**
   - 查询所有启用的 API 路由
   - 为每个路由生成独立的 `ToolDefinition`
   - 工具名称格式：`{resource}_{action}`

2. **工具定义结构**
   ```json
   {
     "name": "product_list_all",
     "description": "获取所有商品列表，支持分页和筛选",
     "parameters": {
       "type": "object",
       "properties": {
         "page": {"type": "integer", "description": "页码，默认1"},
         "size": {"type": "integer", "description": "每页数量，默认10"},
         "categoryId": {"type": "integer", "description": "分类ID，可选"}
       }
     }
   }
   ```

3. **参数扁平化**
   - 不再使用 pathVariables/params/payload 嵌套
   - 直接将所有参数放在顶层
   - 根据 HTTP 方法自动判断参数位置

4. **修改 `handleToolCalls()`**
   - 解析工具名称：`product_list_all` → resource=product, action=list_all
   - 查询对应的路由配置
   - 执行接口调用

**优势**：
- ✅ AI 直接看到所有可用接口
- ✅ 工具名称语义化，易于理解
- ✅ 参数结构简化
- ✅ 减少 AI 理解负担

**预期效果**：
- 工具选择准确率提升 30%
- 参数传递错误减少 50%

---

### 方案2：改进系统提示词

**优先级**：⭐⭐⭐⭐⭐ 高

**改进思路**：
在系统提示词中明确说明工具使用规则、可用资源类型和调用示例。

**改进后的提示词**：
```
你是社区服务智能助手，帮助用户查询商品、活动、订单等信息。

【工具使用规则】
1. 当用户需要查询数据或执行操作时，必须使用工具调用，不要编造数据
2. 工具名称格式：{资源}_{操作}，如 product_list_all
3. 必填参数不能省略，可选参数可以不传
4. 如果调用失败，根据错误提示调整参数后重试
5. 返回数据后，用友好的中文向用户展示结果

【可用资源类型】
- product（商品）：list_all, get_by_id, nearby, create
- activity（活动）：list_all, get_by_id, nearby, create
- order（订单）：list_by_user, create, get_by_id
- user（用户）：get_by_id, update_info
- notification（通知）：list, mark_read

【调用示例】
用户问："附近有什么商品？"
→ 调用工具：product_nearby
→ 参数：{"lat": 39.9, "lng": 116.4, "radiusKm": 5}

用户问："我的订单有哪些？"
→ 调用工具：order_list_by_user
→ 参数：{"userName": "当前用户名"}

【注意事项】
- 地理位置查询需要经纬度参数
- 用户相关操作需要用户名或用户ID
- 分页查询默认 page=1, size=10
```

**实现位置**：
- 文件：`AgentChatServiceImpl.java`
- 方法：`buildPayload()` 中的 `DEFAULT_SYSTEM_PROMPT`

**优势**：
- ✅ 成本最低，改动最小
- ✅ 效果立竿见影
- ✅ AI 理解工具使用规则

**预期效果**：
- 工具选择准确率提升 20%
- 减少无效调用

---

### 方案3：参数验证和友好错误提示

**优先级**：⭐⭐⭐⭐ 中高

**改进思路**：
在调用接口前验证参数完整性和正确性，给出明确的错误提示和修正建议。

**验证内容**：

1. **必填参数检查**
   ```java
   if (requiredParam == null) {
       return buildErrorResponse(
           "缺少必填参数 'id'。示例：{\"id\": 123}"
       );
   }
   ```

2. **参数类型检查**
   ```java
   if (!(value instanceof Integer)) {
       return buildErrorResponse(
           "参数 'page' 必须是整数。当前值：" + value
       );
   }
   ```

3. **参数范围检查**
   ```java
   if (page < 1) {
       return buildErrorResponse(
           "参数 'page' 必须大于0。当前值：" + page
       );
   }
   ```

4. **参数格式检查**
   ```java
   if (!isValidLatitude(lat)) {
       return buildErrorResponse(
           "纬度必须在 -90 到 90 之间。当前值：" + lat
       );
   }
   ```

**错误响应格式**：
```json
{
  "success": false,
  "error": "缺少必填参数 'id'",
  "hint": "正确示例：{\"id\": 123}",
  "requiredParams": ["id"],
  "optionalParams": ["page", "size"]
}
```

**实现位置**：
- 新增方法：`validateParameters(ApiRoute route, Map<String, Object> args)`
- 调用位置：`invokeRoute()` 之前

**优势**：
- ✅ AI 从错误中学习
- ✅ 减少重复错误
- ✅ 提供正确示例

**预期效果**：
- 参数错误率降低 60%
- 减少用户等待时间

---

继续下一部分...
