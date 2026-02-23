package com.example.demo.demosAdmin.AdminActivity.Controller;

import com.example.demo.demos.LocalActive.Pojo.LocalActivity;
import com.example.demo.demos.generic.Resp;
import com.example.demo.demosAdmin.AdminActivity.Service.AdminActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/admin/local-act", "/api/local-act/admin"})
@RequiredArgsConstructor
public class AdminActivityController {

    private final AdminActivityService adminActivityService;

    @GetMapping("/reviews")
    public Resp<List<LocalActivity>> listReviews(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size
    ) {
        return Resp.success(adminActivityService.listReviews(status, keyword, page, size));
    }

    @PostMapping("/reviews/{id}/approve")
    public Resp<Long> approve(@PathVariable("id") Long id,
                              @RequestParam(value = "note", required = false) String note) {
        return Resp.success(adminActivityService.approve(id, note));
    }

    @PostMapping("/reviews/{id}/reject")
    public Resp<Void> reject(@PathVariable("id") Long id,
                             @RequestParam(value = "note", required = false) String note) {
        adminActivityService.reject(id, note);
        return Resp.success();
    }
}
