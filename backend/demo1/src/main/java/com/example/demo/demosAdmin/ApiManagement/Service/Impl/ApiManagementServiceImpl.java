package com.example.demo.demosAdmin.ApiManagement.Service.Impl;

import com.example.demo.demosAdmin.ApiManagement.Dao.ApiManagementMapper;
import com.example.demo.demosAdmin.ApiManagement.Pojo.ApiRoute;
import com.example.demo.demosAdmin.ApiManagement.Service.ApiManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiManagementServiceImpl implements ApiManagementService {

    private final ApiManagementMapper apiMapper;

    @Override
    public List<ApiRoute> listRoutes(String keyword, int page, int size) {
        int limit = Math.max(1, size);
        int offset = Math.max(0, (Math.max(1, page) - 1) * limit);
        return apiMapper.listRoutes(keyword, limit, offset);
    }

    @Override
    public ApiRoute addRoute(ApiRoute route) {
        if (route.getEnabled() == null) {
            route.setEnabled(1); // Default to enabled
        }
        apiMapper.insertRoute(route);
        return route;
    }

    @Override
    public void deleteRoute(Long id) {
        apiMapper.deleteRoute(id);
    }

    @Override
    public void enableRoute(Long id) {
        apiMapper.updateStatus(id, 1);
    }

    @Override
    public void disableRoute(Long id) {
        apiMapper.updateStatus(id, 0);
    }

    @Override
    public void updateRoute(ApiRoute route) {
        apiMapper.updateRoute(route);
    }
}
