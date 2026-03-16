# AI 助手调用准确率提升计划（第2部分）

## 💡 改进方案（续）

### 方案4：增强工具描述和示例

**优先级**：⭐⭐⭐⭐ 中高

**改进思路**：
在数据库 `api_route` 表中添加详细的使用说明和示例，让 AI 看到具体的调用案例。

**数据库表结构扩展**：
```sql
ALTER TABLE api_route ADD COLUMN example_request TEXT COMMENT '请求示例（JSON）';
ALTER TABLE api_route ADD COLUMN example_response TEXT COMMENT '响应示例（JSON）';
ALTER TABLE api_route ADD COLUMN usage_hint VARCHAR(500) COMMENT '使用提示（中文）';
ALTER TABLE api_route ADD COLUMN param_descriptions TEXT COMMENT '参数说明（JSON）';
```

**示例数据**：
```json
{
  "resource": "product",
  "action": "nearby",
  "description": "查询附近的商品",
  "usage_hint": "需要提供当前位置的经纬度，可选搜索半径（默认5公里）",
  "example_request": {
    "lat": 39.9042,
    "lng": 116.4074,
    "radiusKm": 5,
    "categoryId": 1
  },
  "example_response": {
    "code": 200,
    "data": [
      {
        "id": 1,
        "name": "二手自行车",
        "price": 200,
        "distance": 1.2
      }
    ]
  },
  "param_descriptions": {
    "lat": "纬度，必填，范围 -90 到 90",
    "lng": "经度，必填，范围 -180 到 180",
    "radiusKm": "搜索半径（公里），可选，默认5",
    "categoryId": "商品分类ID，可选"
  }
}
```

**工具定义生成时使用**：
```java
private DeepSeekPayload.ToolDefinition buildToolDefinition(ApiRoute route) {
    function.setDescription(
        route.getDescription() +
        "\n使用提示：" + route.getUsageHint() +
        "\n示例：" + route.getExampleRequest()
    );
}
```

**优势**：
- ✅ AI 看到具体示例，模仿调用
- ✅ 减少参数理解错误
- ✅ 提高首次调用成功率

**预期效果**：
- 首次调用成功率提升 25%
- 参数格式错误减少 40%

---

### 方案5：Few-Shot 示例学习

**优先级**：⭐⭐⭐⭐ 中高

**改进思路**：
在对话历史中注入成功的调用示例，让 AI 通过示例学习正确的调用模式。

**实现方式**：
```java
private DeepSeekPayload buildPayload(AgentChatRequest request) {
    List<AgentChatMessage> finalMessages = new ArrayList<>();

    // 1. 系统提示词
    finalMessages.add(systemMessage);

    // 2. 注入 Few-Shot 示例（3-5个典型场景）
    finalMessages.addAll(buildFewShotExamples());

    // 3. 用户实际对话历史
    finalMessages.addAll(history);

    payload.setMessages(finalMessages);
    return payload;
}

private List<AgentChatMessage> buildFewShotExamples() {
    return Arrays.asList(
        // 示例1：查询附近商品
        user("附近有什么商品？"),
        assistant(toolCall("product_nearby", Map.of(
            "lat", 39.9,
            "lng", 116.4,
            "radiusKm", 5
        ))),
        tool("返回了5个商品..."),
        assistant("为您找到附近5个商品：1. 二手自行车..."),

        // 示例2：查询订单
        user("我的订单有哪些？"),
        assistant(toolCall("order_list_by_user", Map.of(
            "userName", "currentUser"
        ))),
        tool("返回了3个订单..."),
        assistant("您有3个订单：1. 订单号...")
    );
}
```

**示例场景选择**：
1. 地理位置查询（附近商品/活动）
2. 用户数据查询（订单/个人信息）
3. 创建操作（发布商品/创建活动）
4. 列表查询（分页参数）
5. 详情查询（ID参数）

**优势**：
- ✅ AI 通过示例学习标准模式
- ✅ 减少理解偏差
- ✅ 提高调用一致性

**预期效果**：
- 标准场景准确率提升 30%
- 减少参数顺序错误

---

### 方案6：添加常用接口的快捷工具

**优先级**：⭐⭐⭐ 中

**改进思路**：
为高频使用的接口创建专门的简化工具，减少参数复杂度。

**快捷工具示例**：

1. **附近商品查询**
   ```
   通用工具：product_nearby(lat, lng, radiusKm, categoryId, minPrice, maxPrice, keyword)
   快捷工具：search_nearby_products(lat, lng, radius)  // 只保留核心参数
   ```

2. **我的订单**
   ```
   通用工具：order_list_by_user(userName, status, page, size)
   快捷工具：my_orders()  // 自动获取当前用户
   ```

3. **发布商品**
   ```
   通用工具：product_create(name, description, price, stock, categoryId, location, ...)
   快捷工具：quick_publish_product(name, price, description)  // 简化版
   ```

**实现方式**：
```java
// 在 getToolDefinitions() 中添加快捷工具
List<DeepSeekPayload.ToolDefinition> tools = new ArrayList<>();

// 1. 添加所有标准工具
tools.addAll(generateStandardTools());

// 2. 添加快捷工具
tools.add(buildQuickTool("search_nearby_products",
    "快速查询附近商品，只需提供位置和半径",
    Map.of("lat", "纬度", "lng", "经度", "radius", "半径（公里）")));

return tools;
```

**优势**：
- ✅ 高频场景调用更简单
- ✅ 减少参数选择困难
- ✅ 提升用户体验

**预期效果**：
- 高频接口准确率提升 20%
- 减少用户等待时间

---

### 方案7：工具调用日志和分析

**优先级**：⭐⭐⭐ 中

**改进思路**：
记录每次工具调用的详细信息，分析失败原因，持续优化。

**日志记录内容**：
```java
@Entity
public class AgentToolCallLog {
    private Long id;
    private String sessionId;
    private String userQuestion;
    private String toolName;
    private String arguments;
    private Boolean success;
    private String errorMessage;
    private String response;
    private Integer responseTime;
    private LocalDateTime createdAt;
}
```

**分析维度**：

1. **工具调用统计**
   - 各工具调用次数
   - 成功率排名
   - 平均响应时间

2. **失败原因分析**
   - 参数错误类型分布
   - 高频错误参数
   - 失败场景模式

3. **优化建议生成**
   - 识别需要改进的工具
   - 发现参数定义问题
   - 提示词优化方向

**实现位置**：
```java
// 在 handleToolCall() 中添加日志记录
private AgentChatResponse handleToolCall(...) {
    AgentToolCallLog log = new AgentToolCallLog();
    log.setToolName(toolName);
    log.setArguments(arguments);

    try {
        Object result = invokeRoute(...);
        log.setSuccess(true);
        log.setResponse(result.toString());
    } catch (Exception e) {
        log.setSuccess(false);
        log.setErrorMessage(e.getMessage());
    } finally {
        logRepository.save(log);
    }
}
```

**优势**：
- ✅ 数据驱动优化
- ✅ 发现隐藏问题
- ✅ 持续改进

**预期效果**：
- 识别 80% 的问题模式
- 为优化提供数据支持

---

## 📅 实施计划

### 第一阶段：快速见效（1-2天）

**目标**：立即提升 20-30% 准确率

**任务**：
1. ✅ 改进系统提示词（方案2）
   - 修改 `DEFAULT_SYSTEM_PROMPT`
   - 添加工具使用规则
   - 列出可用资源类型
   - 提供调用示例

2. ✅ 添加参数验证（方案3）
   - 实现 `validateParameters()` 方法
   - 检查必填参数
   - 验证参数类型
   - 返回友好错误提示

**预期成果**：
- 工具选择准确率提升到 75%
- 参数错误率降低 40%

---

### 第二阶段：架构优化（3-5天）

**目标**：根本性提升准确率到 85%+

**任务**：
1. ✅ 动态生成具体工具（方案1）
   - 重构 `getToolDefinitions()`
   - 为每个路由生成独立工具
   - 参数扁平化处理
   - 修改 `handleToolCalls()` 解析逻辑

2. ✅ 增强工具描述（方案4）
   - 扩展 `api_route` 表结构
   - 添加示例数据
   - 在工具定义中使用示例

**预期成果**：
- 工具调用准确率达到 85%
- 参数传递错误减少 70%

---

### 第三阶段：持续优化（1周）

**目标**：优化用户体验，建立监控体系

**任务**：
1. ✅ Few-Shot 示例（方案5）
   - 选择典型场景
   - 构建示例对话
   - 注入到对话历史

2. ✅ 快捷工具（方案6）
   - 识别高频接口
   - 创建简化工具
   - 测试效果

3. ✅ 日志分析（方案7）
   - 创建日志表
   - 记录调用详情
   - 构建分析报表

**预期成果**：
- 高频场景准确率达到 90%+
- 建立持续优化机制

---

继续第3部分...
