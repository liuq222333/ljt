package com.example.demo.demos.Agent.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.demos.Agent.Dao.ApiRouteMapper;
import com.example.demo.demos.Agent.Entity.ApiRoute;
import com.example.demo.demos.Agent.Service.ApiRouteService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * api_routes 的查询实现，基于 MyBatis-Plus。
 */
@Service
public class ApiRouteServiceImpl implements ApiRouteService {

    private final ApiRouteMapper apiRouteMapper;

    public ApiRouteServiceImpl(ApiRouteMapper apiRouteMapper) {
        this.apiRouteMapper = apiRouteMapper;
    }

    /**
     * 根据资源与动作查找启用状态的路由（仅返回一条）。
     */
    @Override
    public ApiRoute findEnabledRoute(String resource, String action) {
        LambdaQueryWrapper<ApiRoute> wrapper = new LambdaQueryWrapper<ApiRoute>()
                .eq(ApiRoute::getResource, resource)
                .eq(ApiRoute::getAction, action)
                .eq(ApiRoute::getEnabled, 1)
                .last("LIMIT 1");
        return apiRouteMapper.selectOne(wrapper);
    }

    /**
     * 列出某资源下启用的路由；可根据操作类型进一步过滤。
     */
    @Override
    public List<ApiRoute> listEnabledRoutes(String resource, String operationType) {
        LambdaQueryWrapper<ApiRoute> wrapper = new LambdaQueryWrapper<ApiRoute>()
                .eq(ApiRoute::getResource, resource)
                .eq(ApiRoute::getEnabled, 1);
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(ApiRoute::getOperationType, operationType);
        }
        List<ApiRoute> list = apiRouteMapper.selectList(wrapper);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public List<ApiRoute> listAllEnabledRoutes() {
        LambdaQueryWrapper<ApiRoute> wrapper = new LambdaQueryWrapper<ApiRoute>()
                .eq(ApiRoute::getEnabled, 1);
        List<ApiRoute> list = apiRouteMapper.selectList(wrapper);
        return list == null ? Collections.emptyList() : list;
    }
}
