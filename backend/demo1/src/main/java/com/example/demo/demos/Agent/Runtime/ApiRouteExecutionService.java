package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Entity.ApiRoute;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Runtime.adapter.RouteResultAdapter;
import com.example.demo.demos.Agent.Service.BackendApiProxyService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ApiRouteExecutionService {

    private final ApiRouteIntentMatcher apiRouteIntentMatcher;
    private final RouteParamBuilder routeParamBuilder;
    private final BackendApiProxyService backendApiProxyService;
    private final List<RouteResultAdapter> routeResultAdapters;

    public ApiRouteExecutionService(ApiRouteIntentMatcher apiRouteIntentMatcher,
                                    RouteParamBuilder routeParamBuilder,
                                    BackendApiProxyService backendApiProxyService,
                                    List<RouteResultAdapter> routeResultAdapters) {
        this.apiRouteIntentMatcher = apiRouteIntentMatcher;
        this.routeParamBuilder = routeParamBuilder;
        this.backendApiProxyService = backendApiProxyService;
        this.routeResultAdapters = routeResultAdapters == null
                ? Collections.<RouteResultAdapter>emptyList()
                : routeResultAdapters;
    }

    public RouteExecutionResult executeReadRoute(AgentChatRequest request,
                                                 AgentChatMessage latestMessage,
                                                 ParsedIntent parsedIntent,
                                                 String authorization) {
        RouteMatchResult matchResult = apiRouteIntentMatcher == null
                ? RouteMatchResult.noMatch("route_matcher_missing")
                : apiRouteIntentMatcher.matchReadRoute(latestMessage, parsedIntent);
        if (matchResult == null || !matchResult.isMatched() && !matchResult.isRequiresClarification()) {
            return RouteExecutionResult.notMatched(matchResult);
        }
        if (matchResult.isRequiresClarification()) {
            return RouteExecutionResult.clarification(matchResult, matchResult.getClarificationPrompt());
        }

        ApiRoute route = matchResult.getRoute();
        if (route == null) {
            return RouteExecutionResult.notMatched(RouteMatchResult.noMatch("matched_route_missing"));
        }
        if (isProductInternal(route, matchResult)) {
            return RouteExecutionResult.productInternal(matchResult);
        }
        if (Boolean.TRUE.equals(toBoolean(route.getRequireAuthorization())) && !StringUtils.hasText(authorization)) {
            return RouteExecutionResult.clarification(matchResult, "这个查询需要登录态，请先登录后再试。");
        }

        RouteParamBuilder.BuildResult params = routeParamBuilder.build(route, latestMessage, parsedIntent, request);
        if (!CollectionUtils.isEmpty(params.getMissingFields())) {
            return RouteExecutionResult.clarification(matchResult,
                    "要继续查询" + displayEntity(matchResult.getEntityType()) + "，还需要：" + String.join("、", params.getMissingFields()) + "。");
        }

        BackendApiProxyService.InvocationRequest invocationRequest = new BackendApiProxyService.InvocationRequest();
        invocationRequest.setResource(route.getResource());
        invocationRequest.setAction(route.getAction());
        invocationRequest.setAuthorization(authorization);
        invocationRequest.setParams(params.getParams());
        invocationRequest.setPathVariables(params.getPathVariables());
        if (!CollectionUtils.isEmpty(params.getPayload())) {
            invocationRequest.setPayload(params.getPayload());
        }

        BackendApiProxyService.InvocationResult invocationResult = backendApiProxyService.invoke(invocationRequest, authorization);
        if (!isReadInvocationSuccess(invocationResult)) {
            return RouteExecutionResult.failed(matchResult, "route_invocation_failed", invocationResult);
        }
        NormalizedRouteData routeData = adapterFor(route).adapt(route, invocationResult);
        return RouteExecutionResult.success(matchResult, routeData, invocationResult);
    }

    private RouteResultAdapter adapterFor(ApiRoute route) {
        for (RouteResultAdapter adapter : routeResultAdapters) {
            if (adapter != null && adapter.supports(route)) {
                return adapter;
            }
        }
        throw new IllegalStateException("No RouteResultAdapter available");
    }

    private boolean isProductInternal(ApiRoute route, RouteMatchResult matchResult) {
        String entityType = matchResult == null ? null : matchResult.getEntityType();
        if (!StringUtils.hasText(entityType) && route != null) {
            entityType = route.getEntityType();
        }
        if (!"product".equalsIgnoreCase(entityType)) {
            return false;
        }
        String method = route == null ? null : route.getHttpMethod();
        String path = route == null ? null : route.getPathTemplate();
        return "INTERNAL".equalsIgnoreCase(method)
                || StringUtils.hasText(path) && path.toLowerCase(Locale.ROOT).startsWith("internal://");
    }

    private boolean isReadInvocationSuccess(BackendApiProxyService.InvocationResult invocationResult) {
        if (invocationResult == null || !"direct_response".equals(invocationResult.getPresentationHint())) {
            return false;
        }
        Object data = invocationResult.getData();
        if (!(data instanceof Map)) {
            return data != null;
        }
        Map<?, ?> body = (Map<?, ?>) data;
        Object code = body.get("code");
        if (code != null) {
            if (code instanceof Number) {
                return ((Number) code).intValue() == 200;
            }
            return "200".equals(String.valueOf(code));
        }
        Object payload = firstExisting(body, "data", "items", "records", "list", "content");
        if (payload instanceof List) {
            return !((List<?>) payload).isEmpty();
        }
        if (payload instanceof Map) {
            return !((Map<?, ?>) payload).isEmpty();
        }
        return payload != null;
    }

    private Object firstExisting(Map<?, ?> body, String... keys) {
        if (body == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (body.containsKey(key)) {
                return body.get(key);
            }
        }
        return null;
    }

    private Boolean toBoolean(Integer value) {
        if (value == null) {
            return Boolean.FALSE;
        }
        return value.intValue() != 0;
    }

    private String displayEntity(String entityType) {
        if ("event".equalsIgnoreCase(entityType)) {
            return "活动";
        }
        if ("store".equalsIgnoreCase(entityType)) {
            return "门店";
        }
        if ("product".equalsIgnoreCase(entityType)) {
            return "商品";
        }
        return "业务数据";
    }
}
