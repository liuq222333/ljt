package com.example.demo.demosAdmin.AdminNeighborSupportTask.Controller;

import com.example.demo.demos.LocalActive.Pojo.NeighborSupportTask;
import com.example.demo.demos.generic.Resp;
import com.example.demo.demosAdmin.AdminNeighborSupportTask.Service.AdminNeighborSupportTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/neighbor-tasks")
@RequiredArgsConstructor
public class AdminNeighborSupportTaskController {

    private final AdminNeighborSupportTaskService adminService;

    @GetMapping("/reviews")
    public Resp<List<NeighborSupportTask>> listReviews(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size
    ) {
        return Resp.success(adminService.listReviews(status, keyword, page, size));
    }

    @PostMapping("/reviews/{id}/approve")
    public Resp<Long> approve(@PathVariable("id") Long id) {
        return Resp.success(adminService.approve(id));
    }

    @PostMapping("/reviews/{id}/reject")
    public Resp<Void> reject(@PathVariable("id") Long id) {
        adminService.reject(id);
        return Resp.success();
    }
}
