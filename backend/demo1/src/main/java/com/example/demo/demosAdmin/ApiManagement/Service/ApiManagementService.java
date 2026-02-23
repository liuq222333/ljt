package com.example.demo.demosAdmin.ApiManagement.Service;

import com.example.demo.demosAdmin.ApiManagement.Pojo.ApiRoute;
import java.util.List;

public interface ApiManagementService {
    List<ApiRoute> listRoutes(String keyword, int page, int size);
    ApiRoute addRoute(ApiRoute route);
    void deleteRoute(Long id);
    void enableRoute(Long id);
    void disableRoute(Long id);
    void updateRoute(ApiRoute route);
}
