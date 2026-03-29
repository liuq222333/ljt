package com.example.demo.demos.Agent.Service;

import com.example.demo.demos.Agent.Entity.ApiRoute;

import java.util.List;

/**
 * 路由配置查询服务：
 * - 根据资源/动作查找启用的接口
 * - 根据操作类型列出启用的接口集合
 */

public interface ApiRouteService {
    ApiRoute findEnabledRoute(String resource, String action);
    List<ApiRoute> listEnabledRoutes(String resource, String operationType);
    List<ApiRoute> listAllEnabledRoutes();
}
