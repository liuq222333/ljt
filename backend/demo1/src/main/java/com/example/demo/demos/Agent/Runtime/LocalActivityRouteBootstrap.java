package com.example.demo.demos.Agent.Runtime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.demos.Agent.Dao.ApiRouteMapper;
import com.example.demo.demos.Agent.Entity.ApiRoute;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Configuration
public class LocalActivityRouteBootstrap {

    @Bean
    public ApplicationRunner localActivityRouteInitializer(ApiRouteMapper apiRouteMapper, JdbcTemplate jdbcTemplate) {
        return args -> {
            ensureSemanticColumns(jdbcTemplate);
            ensureRoute(apiRouteMapper,
                    "product",
                    "search",
                    "READ",
                    "INTERNAL",
                    "internal://product-snapshot-search",
                    "按关键词、类目、价格等条件查询在售商品列表；执行层使用商品快照搜索，不走 HTTP 代理",
                    "{\"type\":\"object\",\"properties\":{\"page\":{\"type\":\"integer\",\"default\":1},\"size\":{\"type\":\"integer\",\"default\":10},\"searchableOnly\":{\"type\":\"boolean\",\"default\":true},\"keyword\":{\"type\":\"string\"},\"maxPrice\":{\"type\":\"number\"},\"minPrice\":{\"type\":\"number\"}},\"required\":[]}",
                    null,
                    "[\"product_search\",\"mixed_search_knowledge\",\"mixed_search_realtime\"]",
                    "[\"商品\",\"在售\",\"有货\",\"卖\",\"买\",\"搜索\",\"推荐\",\"水果\",\"蔬菜\"]",
                    "[\"有什么商品在售\",\"现在有什么水果可以买\",\"推荐几个社区市场里的蔬菜\",\"有没有100元以内的商品\"]",
                    "product",
                    "READ",
                    0,
                    "product_snapshot",
                    100);
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "list",
                    "READ",
                    "GET",
                    "/api/local-act/activities/list",
                    "查询当前已发布活动列表",
                    "{\"type\":\"object\",\"properties\":{\"page\":{\"type\":\"integer\",\"default\":1},\"size\":{\"type\":\"integer\",\"default\":10},\"status\":{\"type\":\"string\",\"default\":\"PUBLISHED\"}},\"required\":[]}",
                    null,
                    "[\"event_search\"]",
                    "[\"活动\",\"本地活动\",\"社区活动\",\"最近活动\"]",
                    "[\"最近有什么活动\",\"当前有哪些社区活动\",\"本周有什么活动\"]",
                    "event",
                    "READ",
                    0,
                    "activity_cards",
                    90);
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "nearby",
                    "READ",
                    "GET",
                    "/api/local-act/activities/nearby",
                    "查询附近活动",
                    "{\"type\":\"object\",\"properties\":{\"lat\":{\"type\":\"number\"},\"lon\":{\"type\":\"number\"},\"radiusKm\":{\"type\":\"number\",\"default\":20},\"size\":{\"type\":\"integer\",\"default\":10}},\"required\":[\"lat\",\"lon\"]}",
                    null,
                    "[\"event_search\"]",
                    "[\"附近活动\",\"周边活动\",\"离我近\",\"附近\"]",
                    "[\"我附近有什么活动\",\"周边有什么本地活动\"]",
                    "event",
                    "READ",
                    0,
                    "activity_cards",
                    110);
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "create",
                    "CREATE",
                    "POST",
                    "/api/local-act/activities",
                    "创建并发布活动",
                    null,
                    "{\"username\":\"string\",\"title\":\"string\",\"category\":\"string\",\"date\":\"yyyy-MM-dd\",\"timeStart\":\"HH:mm\",\"timeEnd\":\"HH:mm\",\"location\":\"string\",\"description\":\"string\",\"status\":\"PUBLISHED\"}",
                    "[\"event_create\"]",
                    "[\"创建活动\",\"发布活动\",\"发起活动\"]",
                    "[\"帮我创建一个活动\",\"发布一场社区活动\"]",
                    "event",
                    "WRITE_SAFE",
                    1,
                    "direct_text",
                    0);
        };
    }

    private void ensureRoute(ApiRouteMapper apiRouteMapper,
                             String resource,
                             String action,
                             String operationType,
                             String httpMethod,
                             String pathTemplate,
                             String description,
                             String querySchema,
                             String bodySchema,
                             String intentTypes,
                             String triggerKeywords,
                             String triggerExamples,
                             String entityType,
                             String safetyLevel,
                             Integer requireAuthorization,
                             String presentationHint,
                             Integer matchPriority) {
        ApiRoute existing = apiRouteMapper.selectOne(new LambdaQueryWrapper<ApiRoute>()
                .eq(ApiRoute::getResource, resource)
                .eq(ApiRoute::getAction, action)
                .last("LIMIT 1"));
        if (existing != null) {
            boolean changed = false;
            if (!httpMethod.equalsIgnoreCase(existing.getHttpMethod())) {
                existing.setHttpMethod(httpMethod);
                changed = true;
            }
            if (!pathTemplate.equals(existing.getPathTemplate())) {
                existing.setPathTemplate(pathTemplate);
                changed = true;
            }
            if (!operationType.equalsIgnoreCase(existing.getOperationType())) {
                existing.setOperationType(operationType);
                changed = true;
            }
            if (!description.equals(existing.getDescription())) {
                existing.setDescription(description);
                changed = true;
            }
            if (existing.getEnabled() == null || existing.getEnabled() != 1) {
                existing.setEnabled(1);
                changed = true;
            }
            if (querySchema == null ? existing.getQuerySchema() != null : !querySchema.equals(existing.getQuerySchema())) {
                existing.setQuerySchema(querySchema);
                changed = true;
            }
            if (bodySchema == null ? existing.getBodySchema() != null : !bodySchema.equals(existing.getBodySchema())) {
                existing.setBodySchema(bodySchema);
                changed = true;
            }
            if (changed(existing.getIntentTypes(), intentTypes)) {
                existing.setIntentTypes(intentTypes);
                changed = true;
            }
            if (changed(existing.getTriggerKeywords(), triggerKeywords)) {
                existing.setTriggerKeywords(triggerKeywords);
                changed = true;
            }
            if (changed(existing.getTriggerExamples(), triggerExamples)) {
                existing.setTriggerExamples(triggerExamples);
                changed = true;
            }
            if (changed(existing.getEntityType(), entityType)) {
                existing.setEntityType(entityType);
                changed = true;
            }
            if (changed(existing.getSafetyLevel(), safetyLevel)) {
                existing.setSafetyLevel(safetyLevel);
                changed = true;
            }
            if (existing.getRequireAuthorization() == null
                    || !existing.getRequireAuthorization().equals(requireAuthorization)) {
                existing.setRequireAuthorization(requireAuthorization);
                changed = true;
            }
            if (changed(existing.getPresentationHint(), presentationHint)) {
                existing.setPresentationHint(presentationHint);
                changed = true;
            }
            if (existing.getMatchPriority() == null || !existing.getMatchPriority().equals(matchPriority)) {
                existing.setMatchPriority(matchPriority);
                changed = true;
            }
            if (changed) {
                existing.setUpdatedAt(LocalDateTime.now());
                apiRouteMapper.updateById(existing);
            }
            return;
        }

        ApiRoute route = new ApiRoute();
        route.setResource(resource);
        route.setAction(action);
        route.setHttpMethod(httpMethod);
        route.setPathTemplate(pathTemplate);
        route.setOperationType(operationType);
        route.setDescription(description);
        route.setEnabled(1);
        route.setQuerySchema(querySchema);
        route.setBodySchema(bodySchema);
        route.setIntentTypes(intentTypes);
        route.setTriggerKeywords(triggerKeywords);
        route.setTriggerExamples(triggerExamples);
        route.setEntityType(entityType);
        route.setSafetyLevel(safetyLevel);
        route.setRequireAuthorization(requireAuthorization);
        route.setPresentationHint(presentationHint);
        route.setMatchPriority(matchPriority);
        route.setCreatedAt(LocalDateTime.now());
        route.setUpdatedAt(LocalDateTime.now());
        apiRouteMapper.insert(route);
    }

    private void ensureSemanticColumns(JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate == null) {
            return;
        }
        addColumnIfMissing(jdbcTemplate, "intent_types", "JSON NULL COMMENT '可命中的意图类型'");
        addColumnIfMissing(jdbcTemplate, "trigger_keywords", "JSON NULL COMMENT '触发关键词'");
        addColumnIfMissing(jdbcTemplate, "trigger_examples", "JSON NULL COMMENT '触发例句'");
        addColumnIfMissing(jdbcTemplate, "entity_type", "VARCHAR(50) NULL COMMENT '返回实体类型 product/event/store'");
        addColumnIfMissing(jdbcTemplate, "safety_level", "VARCHAR(20) NOT NULL DEFAULT 'READ' COMMENT 'READ/WRITE_SAFE/WRITE_DANGEROUS'");
        addColumnIfMissing(jdbcTemplate, "require_authorization", "TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否要求 Authorization'");
        addColumnIfMissing(jdbcTemplate, "presentation_hint", "VARCHAR(50) NULL COMMENT 'product_cards/activity_cards/store_cards/direct_text'");
        addColumnIfMissing(jdbcTemplate, "match_priority", "INT NOT NULL DEFAULT 0 COMMENT '同分时优先级'");
    }

    private void addColumnIfMissing(JdbcTemplate jdbcTemplate, String columnName, String columnDefinition) {
        try {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList("SHOW COLUMNS FROM api_routes LIKE ?", columnName);
            if (columns == null || columns.isEmpty()) {
                jdbcTemplate.execute("ALTER TABLE api_routes ADD COLUMN " + columnName + " " + columnDefinition);
            }
        } catch (RuntimeException ignore) {
            // Keep startup tolerant; environments without this table can apply the SQL migration manually.
        }
    }

    private boolean changed(String existing, String next) {
        return existing == null ? next != null : !existing.equals(next);
    }
}
