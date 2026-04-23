package com.example.demo.demos.Agent.Runtime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.demos.Agent.Dao.ApiRouteMapper;
import com.example.demo.demos.Agent.Entity.ApiRoute;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class LocalActivityRouteBootstrap {

    @Bean
    public ApplicationRunner localActivityRouteInitializer(ApiRouteMapper apiRouteMapper) {
        return args -> {
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "list",
                    "READ",
                    "GET",
                    "/api/local-act/activities/list",
                    "查询当前已发布活动列表",
                    "{\"status\":\"PUBLISHED\",\"page\":1,\"size\":10}",
                    null);
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "nearby",
                    "READ",
                    "GET",
                    "/api/local-act/activities/nearby",
                    "查询附近活动",
                    "{\"lat\":\"double\",\"lon\":\"double\",\"radiusKm\":20,\"size\":10}",
                    null);
            ensureRoute(apiRouteMapper,
                    "local_activity",
                    "create",
                    "CREATE",
                    "POST",
                    "/api/local-act/activities",
                    "创建并发布活动",
                    null,
                    "{\"username\":\"string\",\"title\":\"string\",\"category\":\"string\",\"date\":\"yyyy-MM-dd\",\"timeStart\":\"HH:mm\",\"timeEnd\":\"HH:mm\",\"location\":\"string\",\"description\":\"string\",\"status\":\"PUBLISHED\"}");
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
                             String bodySchema) {
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
        route.setCreatedAt(LocalDateTime.now());
        route.setUpdatedAt(LocalDateTime.now());
        apiRouteMapper.insert(route);
    }
}
