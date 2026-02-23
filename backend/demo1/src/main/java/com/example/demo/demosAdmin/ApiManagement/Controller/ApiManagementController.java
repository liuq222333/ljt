package com.example.demo.demosAdmin.ApiManagement.Controller;

import com.example.demo.demos.generic.Resp;
import com.example.demo.demosAdmin.ApiManagement.Pojo.ApiRoute;
import com.example.demo.demosAdmin.ApiManagement.Service.ApiManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/api-management")
@RequiredArgsConstructor
public class ApiManagementController {

    private final ApiManagementService apiService;

    @GetMapping("/list")
    public Resp<List<ApiRoute>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return Resp.success(apiService.listRoutes(keyword, page, size));
    }

    @PostMapping("/add")
    public Resp<ApiRoute> add(@RequestBody ApiRoute route) {
        return Resp.success(apiService.addRoute(route));
    }

    @PostMapping("/update")
    public Resp<Void> update(@RequestBody ApiRoute route) {
        apiService.updateRoute(route);
        return Resp.success();
    }

    @PostMapping("/delete/{id}")
    public Resp<Void> delete(@PathVariable("id") Long id) {
        apiService.deleteRoute(id);
        return Resp.success();
    }

    @PostMapping("/enable/{id}")
    public Resp<Void> enable(@PathVariable("id") Long id) {
        apiService.enableRoute(id);
        return Resp.success();
    }

    @PostMapping("/disable/{id}")
    public Resp<Void> disable(@PathVariable("id") Long id) {
        apiService.disableRoute(id);
        return Resp.success();
    }
}
