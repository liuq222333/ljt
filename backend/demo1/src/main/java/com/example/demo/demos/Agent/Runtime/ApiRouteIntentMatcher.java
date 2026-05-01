package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Config.AgentRouteMatcherProperties;
import com.example.demo.demos.Agent.Entity.ApiRoute;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.CandidateSlots;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Service.ApiRouteService;
import com.example.demo.demos.common.enums.TaskType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ApiRouteIntentMatcher {

    private static final List<String> PRODUCT_HINTS = Arrays.asList("商品", "在售", "有货", "水果", "蔬菜", "买", "卖", "价格");
    private static final List<String> EVENT_HINTS = Arrays.asList("活动", "本地活动", "社区活动", "报名", "名额", "市集", "讲座", "观影");
    private static final List<String> STORE_HINTS = Arrays.asList("门店", "店铺", "商家", "营业", "地址", "附近店", "自提点");
    private static final List<String> NEARBY_HINTS = Arrays.asList("附近", "周边", "离我近", "周围");
    private static final List<String> LIST_HINTS = Arrays.asList("有什么", "有哪些", "哪些", "列表", "推荐", "查一下", "查询", "看看", "现在");

    private final ApiRouteService apiRouteService;
    private final AgentRouteMatcherProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiRouteIntentMatcher(ApiRouteService apiRouteService,
                                 AgentRouteMatcherProperties properties) {
        this.apiRouteService = apiRouteService;
        this.properties = properties;
    }

    public RouteMatchResult matchReadRoute(AgentChatMessage latestMessage, ParsedIntent parsedIntent) {
        if (properties != null && !properties.isEnabled()) {
            return RouteMatchResult.noMatch("route_matcher_disabled");
        }
        String text = latestMessage == null ? null : latestMessage.getContent();
        String entityHint = resolveEntityHint(parsedIntent, text);
        List<ApiRoute> routes = safeRoutes(entityHint);
        if (CollectionUtils.isEmpty(routes)) {
            return RouteMatchResult.noMatch("no_enabled_read_routes");
        }

        ScoredRoute best = null;
        ScoredRoute runnerUp = null;
        for (ApiRoute route : routes) {
            ScoredRoute scored = scoreRoute(route, parsedIntent, text, entityHint);
            if (scored == null) {
                continue;
            }
            if (best == null || scored.score > best.score) {
                runnerUp = best;
                best = scored;
            } else if (runnerUp == null || scored.score > runnerUp.score) {
                runnerUp = scored;
            }
        }

        if (best == null) {
            return RouteMatchResult.noMatch("no_route_passed_filters");
        }

        double runnerUpScore = runnerUp == null ? 0D : runnerUp.score;
        int autoThreshold = properties == null ? 72 : properties.getAutoExecuteThreshold();
        int clarifyThreshold = properties == null ? 55 : properties.getClarifyThreshold();
        int closeGap = properties == null ? 8 : properties.getCloseScoreGap();
        if (best.score >= autoThreshold && (runnerUp == null || best.score - runnerUpScore >= closeGap)) {
            return RouteMatchResult.matched(best.route, best.score, runnerUpScore, best.entityType, best.reasons);
        }
        if (best.score >= autoThreshold && runnerUp != null) {
            return RouteMatchResult.clarification(
                    best.route,
                    best.score,
                    runnerUpScore,
                    best.entityType,
                    "我不太确定你想查" + displayEntity(best.entityType) + "还是其他内容，可以再说具体一点吗？",
                    best.reasons
            );
        }
        if (best.score >= clarifyThreshold) {
            return RouteMatchResult.clarification(
                    best.route,
                    best.score,
                    runnerUpScore,
                    best.entityType,
                    "我理解你想查" + displayEntity(best.entityType) + "，还需要你补充更明确的条件。",
                    best.reasons
            );
        }
        return RouteMatchResult.noMatch("route_score_below_threshold");
    }

    private ScoredRoute scoreRoute(ApiRoute route,
                                   ParsedIntent parsedIntent,
                                   String text,
                                   String entityHint) {
        if (route == null || route.getEnabled() == null || route.getEnabled() != 1) {
            return null;
        }
        if (!isReadOperation(route.getOperationType())) {
            return null;
        }
        if (StringUtils.hasText(route.getSafetyLevel()) && !"READ".equalsIgnoreCase(route.getSafetyLevel())) {
            return null;
        }
        String routeEntity = resolveRouteEntity(route);
        if (StringUtils.hasText(entityHint)
                && StringUtils.hasText(routeEntity)
                && !entityHint.equalsIgnoreCase(routeEntity)) {
            return null;
        }

        String taskCode = taskCode(parsedIntent);
        List<String> intentTypes = parseList(route.getIntentTypes());
        if (!intentTypes.isEmpty() && !containsIgnoreCase(intentTypes, taskCode)) {
            return null;
        }

        ScoredRoute scored = new ScoredRoute();
        scored.route = route;
        scored.entityType = StringUtils.hasText(routeEntity) ? routeEntity : entityHint;

        if (!intentTypes.isEmpty()) {
            scored.add(35D, "intent_types");
        } else {
            scored.add(6D, "legacy_route_without_intent_types");
        }

        if (StringUtils.hasText(entityHint) && entityHint.equalsIgnoreCase(scored.entityType)) {
            scored.add(32D, "entity_type");
        }
        scored.add(resourceScore(route, entityHint), "resource_entity_hint");

        int keywordHits = countHits(text, parseList(route.getTriggerKeywords()));
        if (keywordHits == 0) {
            keywordHits = countHits(text, legacyKeywords(scored.entityType, route));
        }
        if (keywordHits > 0) {
            double rawWeight = properties == null ? 0.08D : properties.getRawKeywordWeight();
            scored.add(Math.min(18D, keywordHits * rawWeight * 100D), "trigger_keywords:" + keywordHits);
        }

        int exampleHits = countExampleHits(text, parseList(route.getTriggerExamples()));
        if (exampleHits > 0) {
            scored.add(Math.min(12D, exampleHits * 6D), "trigger_examples:" + exampleHits);
        }

        if (containsAny(text, NEARBY_HINTS)) {
            if (isNearbyRoute(route)) {
                scored.add(24D, "nearby_action");
            } else if ("event".equalsIgnoreCase(scored.entityType) || "store".equalsIgnoreCase(scored.entityType)) {
                scored.add(-12D, "nearby_mismatch");
            }
        } else if (containsAny(text, LIST_HINTS) && isListRoute(route)) {
            scored.add(12D, "list_action");
        }

        if (route.getMatchPriority() != null && route.getMatchPriority() > 0) {
            scored.add(Math.min(12D, route.getMatchPriority() / 10D), "match_priority");
        }

        if (StringUtils.hasText(route.getQuerySchema())) {
            scored.add(4D, "query_schema");
        }
        return scored.score > 0D ? scored : null;
    }

    private List<ApiRoute> safeRoutes(String entityHint) {
        List<ApiRoute> routes = Collections.emptyList();
        try {
            routes = apiRouteService == null ? Collections.<ApiRoute>emptyList() : apiRouteService.listAllEnabledRoutes();
        } catch (RuntimeException ignore) {
            routes = Collections.emptyList();
        }
        if (!CollectionUtils.isEmpty(routes)) {
            return routes;
        }
        String resource = resourceForEntity(entityHint);
        if (!StringUtils.hasText(resource)) {
            return routes;
        }
        try {
            List<ApiRoute> fallback = apiRouteService.listEnabledRoutes(resource, "READ");
            return fallback == null ? Collections.<ApiRoute>emptyList() : fallback;
        } catch (RuntimeException ignore) {
            return Collections.emptyList();
        }
    }

    private boolean isReadOperation(String operationType) {
        if (!StringUtils.hasText(operationType)) {
            return false;
        }
        String normalized = operationType.trim().toUpperCase(Locale.ROOT);
        return "READ".equals(normalized) || "READ_LIST".equals(normalized) || "READ_ONE".equals(normalized);
    }

    private String resolveEntityHint(ParsedIntent parsedIntent, String text) {
        CandidateSlots slots = parsedIntent == null ? null : parsedIntent.getCandidateSlots();
        if (slots != null && StringUtils.hasText(slots.getEntityType())) {
            return normalizeEntity(slots.getEntityType());
        }
        String textEntity = entityFromText(text);
        if (StringUtils.hasText(textEntity)) {
            return textEntity;
        }
        TaskType taskType = parsedIntent == null ? null : parsedIntent.getTaskType();
        if (taskType == TaskType.EVENT_SEARCH) {
            return "event";
        }
        if (taskType == TaskType.STORE_SEARCH) {
            return "store";
        }
        if (taskType == TaskType.PRODUCT_SEARCH
                || taskType == TaskType.MIXED_SEARCH_KNOWLEDGE
                || taskType == TaskType.MIXED_SEARCH_REALTIME
                || taskType == TaskType.REALTIME_QUERY) {
            return "product";
        }
        return null;
    }

    private String entityFromText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if (containsAny(text, EVENT_HINTS)) {
            return "event";
        }
        if (containsAny(text, STORE_HINTS)) {
            return "store";
        }
        if (containsAny(text, PRODUCT_HINTS)) {
            return "product";
        }
        return null;
    }

    private String resolveRouteEntity(ApiRoute route) {
        if (route == null) {
            return null;
        }
        if (StringUtils.hasText(route.getEntityType())) {
            return normalizeEntity(route.getEntityType());
        }
        return entityFromResource(route.getResource());
    }

    private String entityFromResource(String resource) {
        if (!StringUtils.hasText(resource)) {
            return null;
        }
        String normalized = resource.toLowerCase(Locale.ROOT);
        if (normalized.contains("activity") || normalized.contains("event")) {
            return "event";
        }
        if (normalized.contains("store") || normalized.contains("shop") || normalized.contains("merchant")) {
            return "store";
        }
        if (normalized.contains("product") || normalized.contains("goods")) {
            return "product";
        }
        return null;
    }

    private String normalizeEntity(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("activity".equals(normalized) || "local_activity".equals(normalized)) {
            return "event";
        }
        if ("shop".equals(normalized) || "merchant".equals(normalized)) {
            return "store";
        }
        if ("goods".equals(normalized)) {
            return "product";
        }
        return normalized;
    }

    private double resourceScore(ApiRoute route, String entityHint) {
        String routeEntity = resolveRouteEntity(route);
        if (StringUtils.hasText(entityHint) && entityHint.equalsIgnoreCase(routeEntity)) {
            return 28D;
        }
        if (!StringUtils.hasText(entityHint) && StringUtils.hasText(routeEntity)) {
            return 8D;
        }
        return 0D;
    }

    private List<String> legacyKeywords(String entityType, ApiRoute route) {
        Set<String> values = new LinkedHashSet<String>();
        if ("event".equalsIgnoreCase(entityType)) {
            values.addAll(EVENT_HINTS);
        } else if ("store".equalsIgnoreCase(entityType)) {
            values.addAll(STORE_HINTS);
        } else if ("product".equalsIgnoreCase(entityType)) {
            values.addAll(PRODUCT_HINTS);
        }
        if (route != null && StringUtils.hasText(route.getDescription())) {
            values.add(route.getDescription());
        }
        if (route != null && StringUtils.hasText(route.getAction())) {
            values.add(route.getAction());
        }
        return new ArrayList<String>(values);
    }

    private boolean isNearbyRoute(ApiRoute route) {
        String action = route == null ? null : route.getAction();
        String path = route == null ? null : route.getPathTemplate();
        return containsIgnoreCase(Arrays.asList(action, path), "nearby")
                || containsIgnoreCase(Arrays.asList(action, path), "附近");
    }

    private boolean isListRoute(ApiRoute route) {
        String action = route == null ? null : route.getAction();
        if (!StringUtils.hasText(action)) {
            return false;
        }
        String normalized = action.toLowerCase(Locale.ROOT);
        return normalized.contains("list") || normalized.contains("search") || normalized.contains("query");
    }

    private String taskCode(ParsedIntent parsedIntent) {
        TaskType taskType = parsedIntent == null || parsedIntent.getTaskType() == null
                ? TaskType.CHITCHAT : parsedIntent.getTaskType();
        return taskType.getCode();
    }

    private String resourceForEntity(String entityHint) {
        if ("event".equalsIgnoreCase(entityHint)) {
            return "local_activity";
        }
        if ("store".equalsIgnoreCase(entityHint)) {
            return "store";
        }
        if ("product".equalsIgnoreCase(entityHint)) {
            return "product";
        }
        return null;
    }

    private List<String> parseList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        String trimmed = raw.trim();
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            List<String> values = new ArrayList<String>();
            if (node.isArray()) {
                for (JsonNode child : node) {
                    if (child != null && child.isValueNode() && StringUtils.hasText(child.asText())) {
                        values.add(child.asText().trim());
                    }
                }
                return values;
            }
        } catch (Exception ignore) {
            // Fall through to delimiter parsing.
        }
        String[] parts = trimmed.split("[,，;；|\\s]+");
        List<String> values = new ArrayList<String>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                values.add(part.trim());
            }
        }
        return values;
    }

    private int countHits(String text, List<String> keywords) {
        if (!StringUtils.hasText(text) || CollectionUtils.isEmpty(keywords)) {
            return 0;
        }
        int hits = 0;
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (!StringUtils.hasText(keyword)) {
                continue;
            }
            String safe = keyword.trim().toLowerCase(Locale.ROOT);
            if (normalized.contains(safe) || safe.contains(normalized)) {
                hits++;
            }
        }
        return hits;
    }

    private int countExampleHits(String text, List<String> examples) {
        if (!StringUtils.hasText(text) || CollectionUtils.isEmpty(examples)) {
            return 0;
        }
        int hits = 0;
        String normalizedText = compact(text);
        for (String example : examples) {
            String normalizedExample = compact(example);
            if (!StringUtils.hasText(normalizedExample)) {
                continue;
            }
            if (normalizedText.contains(normalizedExample) || normalizedExample.contains(normalizedText)) {
                hits++;
            }
        }
        return hits;
    }

    private String compact(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[,，。！？.!?、；;：:\\s]+", "");
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (!StringUtils.hasText(text) || CollectionUtils.isEmpty(keywords)) {
            return false;
        }
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(List<String> values, String expected) {
        if (CollectionUtils.isEmpty(values) || !StringUtils.hasText(expected)) {
            return false;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)
                    && value.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
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

    private static class ScoredRoute {
        private ApiRoute route;
        private String entityType;
        private double score;
        private final List<String> reasons = new ArrayList<String>();

        private void add(double value, String reason) {
            if (value == 0D) {
                return;
            }
            score += value;
            reasons.add(reason + "=" + value);
        }
    }
}
