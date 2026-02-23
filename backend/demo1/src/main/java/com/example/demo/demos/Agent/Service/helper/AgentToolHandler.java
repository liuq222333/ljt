package com.example.demo.demos.Agent.Service.helper;

import com.example.demo.demos.Agent.Config.AgentAiProperties;
import com.example.demo.demos.Agent.Entity.ApiRoute;
import com.example.demo.demos.Agent.Pojo.AgentChatResponse;
import com.example.demo.demos.Agent.Service.ApiRouteService;
import com.example.demo.demos.Agent.Service.helper.deepseek.DeepSeekPayload;
import com.example.demo.demos.Agent.Service.helper.deepseek.DeepSeekResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import com.example.demo.demos.CommunityMarket.Pojo.Product;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

/**
 * 工具定义与执行的集中入口。
 * 当大模型提出“工具调用”请求时，在此处解析参数并执行相应逻辑。
 * 当前仅暴露一个名为 backend_api 的工具：根据数据库 api_routes 的配置，
 * 动态拼接路径与参数，转发请求到后端真实接口。
 */
@Component
public class AgentToolHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentToolHandler.class);
    private static final String BACKEND_API_TOOL_NAME = "backend_api";

    private final ApiRouteService apiRouteService;
    private final AgentAiProperties agentAiProperties;
    private final RestTemplate backendRestTemplate;
    private final ObjectMapper objectMapper;
    private final List<DeepSeekPayload.ToolDefinition> toolDefinitions;

    public AgentToolHandler(ApiRouteService apiRouteService,
                            AgentAiProperties agentAiProperties,
                            RestTemplateBuilder restTemplateBuilder) {
        this.apiRouteService = apiRouteService;
        this.agentAiProperties = agentAiProperties;
        this.backendRestTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.toolDefinitions = Collections.singletonList(buildBackendToolDefinition());
    }

    public List<DeepSeekPayload.ToolDefinition> getToolDefinitions() {
        return toolDefinitions;
    }

    /**
     * 处理模型返回的工具调用列表，当前只识别 function 类型的调用。
     * 如果命中 backend_api，则进入后端接口代理流程。
     */
    public AgentChatResponse handleToolCalls(DeepSeekResponse body,
                                             List<DeepSeekResponse.ToolCall> toolCalls,
                                             String authorization) {
        if (CollectionUtils.isEmpty(toolCalls)) {
            return null;
        }
        for (DeepSeekResponse.ToolCall call : toolCalls) {
            if (call == null || call.getFunction() == null) {
                continue;
            }
            if (!"function".equalsIgnoreCase(call.getType())) {
                continue;
            }
            DeepSeekResponse.ToolFunction function = call.getFunction();
            if (BACKEND_API_TOOL_NAME.equals(function.getName())) {
                return handleBackendApi(body, function.getArguments(), authorization);
            }
        }
        return null;
    }

    /**
     * 处理 backend_api 工具：反序列化参数 → 查询路由配置 → 调用真实接口 →
     * 将结果包装为统一的回复对象，同时附加“展示指引”。
     */
    private AgentChatResponse handleBackendApi(DeepSeekResponse body, String arguments, String authorization) {
        BackendApiArgs args = parseBackendArgs(arguments);
        if (args == null || !StringUtils.hasText(args.getResource()) || !StringUtils.hasText(args.getAction())) {
            return buildAgentResponse(body, "backend_api missing resource/action.");
        }

        ApiRoute route = apiRouteService.findEnabledRoute(args.getResource(), args.getAction());
        if (route == null) {
            return buildAgentResponse(body, "No enabled route matches resource=" + args.getResource() + ", action=" + args.getAction());
        }
        try {
            Object result = invokeRoute(route, args, authorization);
            Map<String, Object> wrapped = new HashMap<>();
            wrapped.put("presentation_hint", buildPresentationInstruction(route, result));
            wrapped.put("data", result);
            String serialized = objectMapper.writeValueAsString(wrapped);
            return buildAgentResponse(body, serialized);
        } catch (Exception ex) {
            log.warn("backend_api invocation failed", ex);
            return buildAgentResponse(body, "backend_api failed: " + ex.getMessage());
        }
    }

    private Product parseProduct(String arguments) {
        if (!StringUtils.hasText(arguments)) {
            return null;
        }
        try {
            return objectMapper.readValue(arguments, Product.class);
        } catch (Exception ex) {
            log.warn("Failed to parse publish_product arguments: {}", arguments, ex);
            return null;
        }
    }

    /**
     * 构造统一的助手回复对象，并尽量补充模型、请求 ID、用量等元信息。
     */
    private AgentChatResponse buildAgentResponse(DeepSeekResponse body, String reply) {
        AgentChatResponse response = new AgentChatResponse();
        response.setReply(reply);
        if (body != null) {
            response.setModel(body.getModel());
            response.setRequestId(body.getId());
            response.setUsage(body.getUsage());
        }
        return response;
    }

    private Map<String, Object> property(String type, String description) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("description", description);
        return map;
    }

    /**
     * 定义 backend_api 的工具 schema，供模型在需要时选择并填写。
     */
    private DeepSeekPayload.ToolDefinition buildBackendToolDefinition() {
        DeepSeekPayload.ToolDefinition definition = new DeepSeekPayload.ToolDefinition();
        DeepSeekPayload.ToolFunction function = new DeepSeekPayload.ToolFunction();
        function.setName(BACKEND_API_TOOL_NAME);
        function.setDescription("Call a backend API by specifying resource/action plus optional parameters.");

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> props = new HashMap<>();
        props.put("resource", property("string", "Business resource, e.g. product/order/user"));
        props.put("action", property("string", "Action name, e.g. list_all/get_by_id"));
        props.put("pathVariables", property("object", "Path template variables, e.g. {\"id\": 1}"));
        props.put("params", property("object", "Query parameters for GET requests"));
        props.put("payload", property("object", "Request body for POST/PUT requests"));
        props.put("authorization", property("string", "Optional token overriding the default header"));

        schema.put("properties", props);
        schema.put("required", Arrays.asList("resource", "action"));

        function.setParameters(schema);
        definition.setFunction(function);
        return definition;
    }

    /**
     * 解析模型传入的 backend_api 参数（JSON 字符串）。
     */
    private BackendApiArgs parseBackendArgs(String arguments) {
        if (!StringUtils.hasText(arguments)) {
            return null;
        }
        try {
            return objectMapper.readValue(arguments, BackendApiArgs.class);
        } catch (Exception ex) {
            log.warn("Failed to parse backend_api arguments: {}", arguments, ex);
            return null;
        }
    }

    /**
     * 根据路由配置和参数构造 HTTP 请求并发起调用，返回原始响应数据。
     */
    private Object invokeRoute(ApiRoute route, BackendApiArgs args, String authorization) throws Exception {
        String path = buildPath(route, args);
        URI uri = buildUri(path, args.getParams());

        HttpMethod method = HttpMethod.resolve(route.getHttpMethod().toUpperCase());
        if (method == null) {
            throw new IllegalArgumentException("Unsupported http_method: " + route.getHttpMethod());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        String token = StringUtils.hasText(args.getAuthorization()) ? args.getAuthorization() : authorization;
        if (StringUtils.hasText(token)) {
            headers.set("Authorization", token);
        }

        Object payload = args.getPayload();
        if (method == HttpMethod.GET || method == HttpMethod.DELETE) {
            payload = null;
        }

        RequestEntity<Object> requestEntity = new RequestEntity<>(payload, headers, method, uri);
        ResponseEntity<String> response = backendRestTemplate.exchange(requestEntity, String.class);
        String body = response.getBody();
        if (!StringUtils.hasText(body)) {
            return Collections.singletonMap("status", response.getStatusCodeValue());
        }
        return objectMapper.readValue(body, new TypeReference<Object>() {});
    }

    /**
     * 组合完整的 URI，并附加查询参数。
     */
    private URI buildUri(String path, Map<String, Object> params) {
        String base = agentAiProperties.getInternalApiBaseUrl();
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(normalizedBase + normalizedPath);
        if (params != null) {
            params.forEach(builder::queryParam);
        }
        return builder.build(true).toUri();
    }

    /**
     * 根据 pathTemplate 与 pathParams 变量列表，替换路径中的占位符并返回最终路径。
     */
    private String buildPath(ApiRoute route, BackendApiArgs args) throws Exception {
        String path = route.getPathTemplate();
        List<String> required = parsePathParamList(route.getPathParams());
        Map<String, Object> variables = args.getPathVariables();
        if ((variables == null || variables.isEmpty()) && StringUtils.hasText(args.getId())) {
            variables = Collections.singletonMap("id", args.getId());
        }
        if (!CollectionUtils.isEmpty(required)) {
            if (variables == null || !variables.keySet().containsAll(required)) {
                throw new IllegalArgumentException("Missing path variables: " + required);
            }
            for (String key : required) {
                Object val = variables.get(key);
                if (val == null) {
                    throw new IllegalArgumentException("Path variable " + key + " is null");
                }
                path = path.replace("{" + key + "}", encodePathSegment(val.toString()));
            }
        }
        return path;
    }

    /**
     * 解析数据库中存储的路径变量列表（JSON 数组）。
     */
    private List<String> parsePathParamList(String json) throws Exception {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        return objectMapper.readValue(json, new TypeReference<List<String>>() {});
    }

    /**
     * 对单个路径段进行编码，防止特殊字符破坏 URL。
     */
    private String encodePathSegment(String raw) {
        String encoded = UriComponentsBuilder.fromPath("/")
                .pathSegment(raw)
                .build()
                .toUri()
                .getRawPath();
        return encoded != null && encoded.startsWith("/") ? encoded.substring(1) : encoded;
    }

    /**
     * 为返回的数据生成“展示指引”，提示模型应如何组织输出以提高可读性。
     */
    private String buildPresentationInstruction(ApiRoute route, Object result) {
        String title = StringUtils.hasText(route.getDescription()) ? route.getDescription() : route.getAction();
        StringBuilder instruction = new StringBuilder();
        instruction.append("你拿到了接口【").append(title).append("】的原始数据，请结合用户问题和上下文，选择最合适的排版方式输出，确保易读。\n");

        if (result instanceof List) {
            instruction.append("data 是列表：可按编号、项目符号或 Markdown 表格展示，挑出最有代表性的 5 条左右，并给出简短总结。");
        } else if (result instanceof Map) {
            instruction.append("data 是对象：用条目/表格列出关键字段，再补充一句解释或建议。");
        } else if (result == null) {
            instruction.append("data 为空：向用户说明暂无数据。");
        } else {
            instruction.append("data 为标量：直接解释数据含义并给建议即可。");
        }
        return instruction.toString();
    }

    private static class BackendApiArgs {
        private String resource;
        private String action;
        private String id;
        private Map<String, Object> pathVariables;
        private Map<String, Object> params;
        private Map<String, Object> payload;
        private String authorization;

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, Object> getPathVariables() {
            return pathVariables;
        }

        public void setPathVariables(Map<String, Object> pathVariables) {
            this.pathVariables = pathVariables;
        }

        public Map<String, Object> getParams() {
            return params;
        }

        public void setParams(Map<String, Object> params) {
            this.params = params;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }

        public void setPayload(Map<String, Object> payload) {
            this.payload = payload;
        }

        public String getAuthorization() {
            return authorization;
        }

        public void setAuthorization(String authorization) {
            this.authorization = authorization;
        }
    }
}
