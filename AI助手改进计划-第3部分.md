# AI 助手调用准确率提升计划（第3部分）

## 🔧 技术实现细节

### 实现1：动态生成工具定义

**文件位置**：`AgentToolHandler.java`

**核心代码结构**：
```java
public List<DeepSeekPayload.ToolDefinition> getToolDefinitions() {
    // 1. 从数据库查询所有启用的路由
    List<ApiRoute> routes = apiRouteService.listAllEnabledRoutes();

    // 2. 为每个路由生成工具定义
    return routes.stream()
        .map(this::buildToolDefinition)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
}

private DeepSeekPayload.ToolDefinition buildToolDefinition(ApiRoute route) {
    // 工具名称：resource_action
    String toolName = route.getResource() + "_" + route.getAction();

    // 工具描述：使用数据库配置
    String description = route.getDescription();
    if (StringUtils.hasText(route.getUsageHint())) {
        description += "\n使用提示：" + route.getUsageHint();
    }

    // 参数定义：根据路由配置生成
    Map<String, Object> parameters = buildParameterSchema(route);

    // 构建工具定义
    ToolDefinition definition = new ToolDefinition();
    ToolFunction function = new ToolFunction();
    function.setName(toolName);
    function.setDescription(description);
    function.setParameters(parameters);
    definition.setFunction(function);

    return definition;
}
```

**参数 Schema 生成**：
```java
private Map<String, Object> buildParameterSchema(ApiRoute route) {
    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");

    Map<String, Object> properties = new HashMap<>();
    List<String> required = new ArrayList<>();

    // 路径参数（必填）
    if (route.getPathParams() != null) {
        for (String param : parsePathParams(route.getPathParams())) {
            properties.put(param, Map.of(
                "type", "string",
                "description", "路径参数"
            ));
            required.add(param);
        }
    }

    // 查询参数（可选）
    if (route.getQueryParams() != null) {
        Map<String, String> queryParams = parseQueryParams(route.getQueryParams());
        queryParams.forEach((key, desc) -> {
            properties.put(key, Map.of(
                "type", inferType(key),
                "description", desc
            ));
        });
    }

    // 请求体参数
    if ("POST".equals(route.getHttpMethod()) || "PUT".equals(route.getHttpMethod())) {
        if (route.getBodySchema() != null) {
            properties.put("body", parseBodySchema(route.getBodySchema()));
            required.add("body");
        }
    }

    schema.put("properties", properties);
    if (!required.isEmpty()) {
        schema.put("required", required);
    }

    return schema;
}
```

**工具调用处理**：
```java
public AgentChatResponse handleToolCalls(DeepSeekResponse body,
                                        List<ToolCall> toolCalls,
                                        String authorization) {
    for (ToolCall call : toolCalls) {
        String toolName = call.getFunction().getName();

        // 解析工具名称：product_list_all -> resource=product, action=list_all
        String[] parts = toolName.split("_", 2);
        String resource = parts[0];
        String action = parts[1];

        // 查询路由配置
        ApiRoute route = apiRouteService.findEnabledRoute(resource, action);

        // 解析参数
        Map<String, Object> args = parseArguments(call.getFunction().getArguments());

        // 调用接口
        Object result = invokeRoute(route, args, authorization);

        // 返回结果
        return buildResponse(body, result);
    }
}
```

---

### 实现2：改进系统提示词

**文件位置**：`AgentChatServiceImpl.java`

**修改内容**：
```java
private static final String DEFAULT_SYSTEM_PROMPT = """
你是社区服务智能助手，帮助用户查询商品、活动、订单等信息。

【工具使用规则】
1. 当用户需要查询数据或执行操作时，必须使用工具调用，不要编造数据
2. 工具名称格式：{资源}_{操作}，例如 product_list_all、activity_nearby
3. 必填参数不能省略，可选参数可以不传
4. 如果调用失败，根据错误提示调整参数后重试一次
5. 返回数据后，用友好的中文向用户展示结果，突出关键信息

【可用资源类型】
- product（商品）：list_all（列表）, get_by_id（详情）, nearby（附近）, create（发布）
- activity（活动）：list_all（列表）, get_by_id（详情）, nearby（附近）, create（创建）
- order（订单）：list_by_user（我的订单）, create（创建）, get_by_id（详情）
- user（用户）：get_by_id（信息）, update_info（更新）
- notification（通知）：list（列表）, mark_read（已读）, unread_count（未读数）

【调用示例】
用户："附近有什么商品？"
→ 工具：product_nearby
→ 参数：{"lat": 39.9, "lng": 116.4, "radiusKm": 5}

用户："我的订单"
→ 工具：order_list_by_user
→ 参数：{"userName": "当前用户"}

用户："查看商品123的详情"
→ 工具：product_get_by_id
→ 参数：{"id": "123"}

【注意事项】
- 地理位置查询需要经纬度（lat, lng）
- 用户相关操作需要 userName 或 userId
- 分页查询默认 page=1, size=10
- ID 参数通常是字符串或数字类型
""";
```

---

### 实现3：参数验证

**新增方法**：
```java
private void validateParameters(ApiRoute route, Map<String, Object> args)
        throws IllegalArgumentException {

    // 1. 检查必填参数
    List<String> required = parseRequiredParams(route);
    for (String param : required) {
        if (!args.containsKey(param) || args.get(param) == null) {
            throw new IllegalArgumentException(
                String.format("缺少必填参数 '%s'。示例：{\"%s\": \"值\"}", param, param)
            );
        }
    }

    // 2. 检查参数类型
    Map<String, String> paramTypes = parseParamTypes(route);
    for (Map.Entry<String, Object> entry : args.entrySet()) {
        String expectedType = paramTypes.get(entry.getKey());
        if (expectedType != null) {
            validateType(entry.getKey(), entry.getValue(), expectedType);
        }
    }

    // 3. 检查特殊参数范围
    if (args.containsKey("lat")) {
        double lat = ((Number) args.get("lat")).doubleValue();
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException(
                "纬度必须在 -90 到 90 之间。当前值：" + lat
            );
        }
    }

    if (args.containsKey("lng")) {
        double lng = ((Number) args.get("lng")).doubleValue();
        if (lng < -180 || lng > 180) {
            throw new IllegalArgumentException(
                "经度必须在 -180 到 180 之间。当前值：" + lng
            );
        }
    }

    if (args.containsKey("page")) {
        int page = ((Number) args.get("page")).intValue();
        if (page < 1) {
            throw new IllegalArgumentException(
                "页码必须大于 0。当前值：" + page
            );
        }
    }
}

private void validateType(String paramName, Object value, String expectedType) {
    switch (expectedType) {
        case "integer":
            if (!(value instanceof Number)) {
                throw new IllegalArgumentException(
                    String.format("参数 '%s' 必须是整数。当前类型：%s",
                        paramName, value.getClass().getSimpleName())
                );
            }
            break;
        case "string":
            if (!(value instanceof String)) {
                throw new IllegalArgumentException(
                    String.format("参数 '%s' 必须是字符串。当前类型：%s",
                        paramName, value.getClass().getSimpleName())
                );
            }
            break;
        case "number":
            if (!(value instanceof Number)) {
                throw new IllegalArgumentException(
                    String.format("参数 '%s' 必须是数字。当前类型：%s",
                        paramName, value.getClass().getSimpleName())
                );
            }
            break;
    }
}
```

**调用位置**：
```java
private Object invokeRoute(ApiRoute route, Map<String, Object> args, String auth) {
    // 先验证参数
    validateParameters(route, args);

    // 再调用接口
    // ...
}
```

---

### 实现4：数据库表扩展

**SQL 脚本**：
```sql
-- 扩展 api_route 表
ALTER TABLE api_route
ADD COLUMN example_request TEXT COMMENT '请求示例（JSON）',
ADD COLUMN example_response TEXT COMMENT '响应示例（JSON）',
ADD COLUMN usage_hint VARCHAR(500) COMMENT '使用提示',
ADD COLUMN param_descriptions TEXT COMMENT '参数说明（JSON）',
ADD COLUMN query_params TEXT COMMENT '查询参数定义（JSON）',
ADD COLUMN body_schema TEXT COMMENT '请求体结构（JSON）';

-- 示例数据
INSERT INTO api_route (resource, action, description, http_method, path_template,
    path_params, query_params, usage_hint, example_request, enabled)
VALUES
('product', 'nearby', '查询附近的商品', 'GET', '/api/products/nearby',
    NULL,
    '{"lat": "纬度（必填）", "lng": "经度（必填）", "radiusKm": "搜索半径（可选，默认5）", "categoryId": "分类ID（可选）"}',
    '需要提供当前位置的经纬度坐标',
    '{"lat": 39.9, "lng": 116.4, "radiusKm": 5}',
    1);
```

---

## 📊 效果评估

### 评估指标

1. **工具调用准确率**
   - 定义：AI 选择正确工具的比例
   - 目标：从 60% 提升到 85%+
   - 测量：正确调用次数 / 总调用次数

2. **参数传递准确率**
   - 定义：参数完整且格式正确的比例
   - 目标：从 50% 提升到 90%+
   - 测量：参数正确次数 / 总调用次数

3. **首次调用成功率**
   - 定义：第一次调用就成功的比例
   - 目标：从 40% 提升到 75%+
   - 测量：首次成功次数 / 总会话数

4. **平均重试次数**
   - 定义：每次成功调用的平均尝试次数
   - 目标：从 2.5 次降低到 1.3 次
   - 测量：总尝试次数 / 成功次数

### 测试场景

**基础场景**（必须100%成功）：
1. 查询所有商品列表
2. 根据ID查询商品详情
3. 查询用户订单列表
4. 查询用户信息

**中等场景**（目标90%成功）：
1. 附近商品查询（带经纬度）
2. 附近活动查询
3. 分页查询（带page/size参数）
4. 条件筛选查询（带多个可选参数）

**复杂场景**（目标80%成功）：
1. 发布商品（多参数POST请求）
2. 创建活动（复杂请求体）
3. 更新用户信息（部分字段更新）
4. 组合查询（多个条件组合）

---

## ⚠️ 风险与注意事项

### 技术风险

1. **工具数量过多**
   - 风险：工具定义超过模型上下文限制
   - 缓解：只加载常用工具，按需动态加载

2. **参数验证性能**
   - 风险：复杂验证影响响应速度
   - 缓解：只验证关键参数，异步记录日志

3. **数据库查询性能**
   - 风险：每次对话都查询路由配置
   - 缓解：添加缓存，定期刷新

### 兼容性风险

1. **现有接口影响**
   - 风险：修改可能影响现有功能
   - 缓解：保留原有实现，新旧并存，逐步切换

2. **数据库迁移**
   - 风险：表结构变更可能失败
   - 缓解：先在测试环境验证，做好数据备份

---

## 📝 总结

### 核心改进点

1. **工具定义具体化** - 从1个抽象工具变为N个具体工具
2. **提示词优化** - 明确规则、示例和注意事项
3. **参数验证增强** - 提前发现错误，给出修正建议
4. **示例学习** - Few-Shot 提高标准场景准确率

### 预期收益

- 工具调用准确率：60% → 85%+
- 参数错误率：降低 70%
- 用户体验：显著提升
- 维护成本：降低（数据库驱动）

### 实施建议

**优先级排序**：
1. 方案2（系统提示词）- 立即实施
2. 方案3（参数验证）- 立即实施
3. 方案1（动态工具）- 第二阶段
4. 方案4（工具描述）- 第二阶段
5. 方案5（Few-Shot）- 第三阶段

**时间安排**：
- 第一阶段：1-2天（快速见效）
- 第二阶段：3-5天（架构优化）
- 第三阶段：1周（持续优化）

---

**文档完成时间**：2026-03-15
**文档版本**：v1.0
