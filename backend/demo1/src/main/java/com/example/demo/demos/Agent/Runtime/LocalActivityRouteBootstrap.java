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
                    200);
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "list",
                    "READ",
                    "GET",
                    "/api/local-act/activities/list",
                    "查询当前已发布活动列表",
                    "{\"type\":\"object\",\"properties\":{\"page\":{\"type\":\"integer\",\"default\":1},\"size\":{\"type\":\"integer\",\"default\":10},\"status\":{\"type\":\"string\",\"default\":\"PUBLISHED\"},\"timeState\":{\"type\":\"string\",\"enum\":[\"ongoing\",\"upcoming\",\"ended\"]}},\"required\":[]}",
                    null,
                    "[\"event_search\"]",
                    "[\"活动\",\"本地活动\",\"社区活动\",\"最近活动\",\"正在进行\",\"进行中\",\"即将开始\"]",
                    "[\"最近有什么活动\",\"当前有哪些社区活动\",\"本周有什么活动\",\"有什么活动正在进行\",\"现在有什么活动正在进行\"]",
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
                    "my_list",
                    "READ",
                    "GET",
                    "/api/local-act/my-activities",
                    "查询当前登录用户发布的活动列表",
                    "{\"type\":\"object\",\"properties\":{\"username\":{\"type\":\"string\"},\"page\":{\"type\":\"integer\",\"default\":1},\"size\":{\"type\":\"integer\",\"default\":10},\"status\":{\"type\":\"string\"},\"timeState\":{\"type\":\"string\",\"enum\":[\"ongoing\",\"upcoming\",\"ended\"]}},\"required\":[\"username\"]}",
                    null,
                    "[\"event_search\"]",
                    "[\"我发布的活动\",\"我的活动\",\"我发起的活动\",\"我组织的活动\",\"发布的活动\"]",
                    "[\"查询我发布的活动\",\"查看我的活动\",\"我发起了哪些活动\",\"我组织的活动有哪些\"]",
                    "event",
                    "READ",
                    0,
                    "activity_cards",
                    140);
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "my_favorites",
                    "READ",
                    "GET",
                    "/api/local-act/favorites",
                    "查询当前登录用户收藏的活动列表",
                    "{\"type\":\"object\",\"properties\":{\"username\":{\"type\":\"string\"},\"page\":{\"type\":\"integer\",\"default\":1},\"size\":{\"type\":\"integer\",\"default\":10}},\"required\":[\"username\"]}",
                    null,
                    null,
                    "[\"我收藏的活动\",\"我的收藏\",\"收藏活动\",\"查看收藏\",\"查询收藏\"]",
                    "[\"查询我收藏的活动\",\"查看我的收藏\",\"我收藏了哪些活动\"]",
                    "event",
                    "READ",
                    0,
                    "activity_cards",
                    135);
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "list_enrollments",
                    "READ_LIST",
                    "GET",
                    "/api/local-act/enrollments",
                    "按用户名、状态、时间范围查询本地活动报名记录",
                    "{\"type\":\"object\",\"properties\":{\"username\":{\"type\":\"string\"},\"status\":{\"type\":\"string\"},\"period\":{\"type\":\"string\",\"enum\":[\"upcoming\",\"past\",\"all\"]},\"keyword\":{\"type\":\"string\"}},\"required\":[\"username\"]}",
                    null,
                    null,
                    "[\"我的报名\",\"报名记录\",\"我报名的活动\",\"参与记录\",\"我的活动报名\"]",
                    "[\"查询我的报名记录\",\"我报名了哪些活动\",\"查看我的活动参与记录\"]",
                    "event",
                    "READ",
                    0,
                    "activity_cards",
                    130);
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "detail",
                    "READ_ONE",
                    "GET",
                    "/api/local-act/activities/{id}",
                    "查询指定活动详情",
                    "{\"type\":\"object\",\"properties\":{\"username\":{\"type\":\"string\"}},\"required\":[]}",
                    null,
                    null,
                    "[\"活动详情\",\"查看活动\",\"这个活动\",\"活动信息\"]",
                    "[\"查看活动详情\",\"查询活动 14 的详情\",\"这个活动的信息\"]",
                    "event",
                    "READ",
                    0,
                    "activity_cards",
                    125,
                    "[\"id\"]");
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "heatmap",
                    "READ",
                    "GET",
                    "/api/local-act/heatmap",
                    "查询附近活动热力点",
                    "{\"type\":\"object\",\"properties\":{\"lat\":{\"type\":\"number\"},\"lon\":{\"type\":\"number\"},\"radiusKm\":{\"type\":\"number\",\"default\":5},\"category\":{\"type\":\"string\"},\"keyword\":{\"type\":\"string\"},\"size\":{\"type\":\"integer\",\"default\":500}},\"required\":[\"lat\",\"lon\"]}",
                    null,
                    null,
                    "[\"活动热力图\",\"热力点\",\"附近热度\",\"活动分布\"]",
                    "[\"查看附近活动热力图\",\"附近活动分布怎么样\"]",
                    "event",
                    "READ",
                    0,
                    "activity_heatmap",
                    100);
            ensureRoute(apiRouteMapper,
                    "local_story",
                    "list",
                    "READ",
                    "GET",
                    "/api/local-act/stories",
                    "查询社区故事列表",
                    "{\"type\":\"object\",\"properties\":{\"keyword\":{\"type\":\"string\"},\"visibility\":{\"type\":\"string\"},\"page\":{\"type\":\"integer\",\"default\":1},\"size\":{\"type\":\"integer\",\"default\":10}},\"required\":[]}",
                    null,
                    null,
                    "[\"社区故事\",\"邻里故事\",\"活动故事\",\"故事\"]",
                    "[\"有什么社区故事\",\"查看邻里故事\",\"活动故事有哪些\"]",
                    "story",
                    "READ",
                    0,
                    "story_cards",
                    120);
            ensureRoute(apiRouteMapper,
                    "local_story",
                    "detail",
                    "READ_ONE",
                    "GET",
                    "/api/local-act/stories/{id}",
                    "查询社区故事详情",
                    "{\"type\":\"object\",\"properties\":{},\"required\":[]}",
                    null,
                    null,
                    "[\"故事详情\",\"查看故事\",\"这个故事\"]",
                    "[\"查看故事详情\",\"查询故事 1 的详情\"]",
                    "story",
                    "READ",
                    0,
                    "story_cards",
                    115,
                    "[\"id\"]");
            ensureRoute(apiRouteMapper,
                    "neighbor_support",
                    "list_tasks",
                    "READ",
                    "GET",
                    "/api/neighbor-support/tasks",
                    "查询邻里互助任务列表",
                    "{\"type\":\"object\",\"properties\":{\"status\":{\"type\":\"string\"}},\"required\":[]}",
                    null,
                    null,
                    "[\"邻里互助\",\"互助任务\",\"志愿服务\",\"求助\",\"帮忙\"]",
                    "[\"有什么邻里互助任务\",\"查看志愿服务任务\",\"附近有什么需要帮忙的\"]",
                    "support",
                    "READ",
                    0,
                    "support_cards",
                    120);
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "enroll",
                    "CREATE",
                    "POST",
                    "/api/local-act/activities/{id}/enroll",
                    "报名指定活动",
                    null,
                    "{\"username\":\"string\"}",
                    "[\"event_action\"]",
                    "[\"报名活动\",\"参加活动\",\"我要报名\",\"参与活动\"]",
                    "[\"报名这个活动\",\"我要参加活动 14\"]",
                    "event",
                    "WRITE_SAFE",
                    1,
                    "direct_text",
                    0,
                    "[\"id\"]");
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "cancel_enrollment",
                    "CREATE",
                    "POST",
                    "/api/local-act/activities/{id}/cancel-enrollment",
                    "取消指定活动报名",
                    null,
                    "{\"username\":\"string\",\"reason\":\"string\"}",
                    "[\"event_action\"]",
                    "[\"取消报名\",\"退出活动\",\"不参加了\"]",
                    "[\"取消这个活动的报名\",\"我不参加活动 14 了\"]",
                    "event",
                    "WRITE_SAFE",
                    1,
                    "direct_text",
                    0,
                    "[\"id\"]");
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "favorite",
                    "CREATE",
                    "POST",
                    "/api/local-act/activities/{id}/favorite",
                    "收藏指定活动",
                    null,
                    "{\"username\":\"string\"}",
                    "[\"event_action\"]",
                    "[\"收藏活动\",\"加入收藏\",\"收藏这个\"]",
                    "[\"收藏这个活动\",\"把活动 14 加入收藏\"]",
                    "event",
                    "WRITE_SAFE",
                    1,
                    "direct_text",
                    0,
                    "[\"id\"]");
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "unfavorite",
                    "CREATE",
                    "POST",
                    "/api/local-act/activities/{id}/unfavorite",
                    "取消收藏指定活动",
                    null,
                    "{\"username\":\"string\"}",
                    "[\"event_action\"]",
                    "[\"取消收藏\",\"移出收藏\",\"不收藏了\"]",
                    "[\"取消收藏这个活动\",\"把活动 14 移出收藏\"]",
                    "event",
                    "WRITE_SAFE",
                    1,
                    "direct_text",
                    0,
                    "[\"id\"]");
            ensureRoute(apiRouteMapper,
                    "local_story",
                    "create",
                    "CREATE",
                    "POST",
                    "/api/local-act/stories",
                    "发布社区故事",
                    null,
                    "{\"username\":\"string\",\"activityId\":\"number\",\"title\":\"string\",\"coverUrl\":\"string\",\"summary\":\"string\",\"content\":\"string\",\"visibility\":\"string\"}",
                    "[\"story_create\"]",
                    "[\"发布故事\",\"创建故事\",\"写故事\"]",
                    "[\"发布一篇社区故事\",\"给活动写故事\"]",
                    "story",
                    "WRITE_SAFE",
                    1,
                    "direct_text",
                    0);
            ensureRoute(apiRouteMapper,
                    "neighbor_support",
                    "create_task",
                    "CREATE",
                    "POST",
                    "/api/neighbor-support/tasks",
                    "发布邻里互助任务",
                    null,
                    "{\"username\":\"string\",\"title\":\"string\",\"category\":\"string\",\"description\":\"string\",\"location\":\"string\",\"startTime\":\"datetime\",\"endTime\":\"datetime\",\"volunteerSlots\":\"number\",\"priority\":\"string\",\"rewardPoints\":\"number\"}",
                    "[\"support_create\"]",
                    "[\"发布互助\",\"创建互助任务\",\"发布志愿任务\"]",
                    "[\"发布一个邻里互助任务\",\"创建志愿服务任务\"]",
                    "support",
                    "WRITE_SAFE",
                    1,
                    "direct_text",
                    0);
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "create_schedule_template",
                    "CREATE",
                    "POST",
                    "/api/local-act/schedule-templates",
                    "创建固定日程模板",
                    null,
                    "{\"username\":\"string\",\"title\":\"string\",\"category\":\"string\",\"weekday\":\"number\",\"startTime\":\"HH:mm\",\"endTime\":\"HH:mm\",\"location\":\"string\",\"recurrenceRule\":\"string\",\"capacity\":\"number\",\"feeType\":\"string\",\"reminderMinutes\":\"number\",\"status\":\"string\"}",
                    "[\"event_action\"]",
                    "[\"创建日程模板\",\"固定日程\",\"周期活动模板\",\"每周活动\"]",
                    "[\"创建每周活动模板\",\"帮我建一个固定日程\"]",
                    "event",
                    "WRITE_SAFE",
                    1,
                    "direct_text",
                    0);
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "create",
                    "CREATE",
                    "POST",
                    "/api/local-act/activities",
                    "创建并发布活动",
                    null,
                    "{\"type\":\"object\",\"properties\":{\"username\":{\"type\":\"string\"},\"title\":{\"type\":\"string\"},\"category\":{\"type\":\"string\"},\"date\":{\"type\":\"string\"},\"timeStart\":{\"type\":\"string\"},\"timeEnd\":{\"type\":\"string\"},\"location\":{\"type\":\"string\"},\"description\":{\"type\":\"string\"},\"coverUrl\":{\"type\":\"string\"},\"status\":{\"type\":\"string\",\"default\":\"PUBLISHED\"}},\"required\":[\"username\",\"title\",\"category\",\"date\",\"timeStart\",\"timeEnd\",\"location\",\"description\",\"coverUrl\"]}",
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
        ensureRoute(apiRouteMapper, resource, action, operationType, httpMethod, pathTemplate, description,
                querySchema, bodySchema, intentTypes, triggerKeywords, triggerExamples, entityType, safetyLevel,
                requireAuthorization, presentationHint, matchPriority, null);
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
                             Integer matchPriority,
                             String pathParams) {
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
            if (changed(existing.getPathParams(), pathParams)) {
                existing.setPathParams(pathParams);
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
        route.setPathParams(pathParams);
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
