package com.example.demo.demos.LocalActive.Controller;

import com.example.demo.demos.LocalActive.DTO.LocalActCreateRequest;
import com.example.demo.demos.LocalActive.DTO.LocalActCreateResponse;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentListResponse;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentQuery;
import com.example.demo.demos.LocalActive.DTO.LocalActScheduleTemplateRequest;
import com.example.demo.demos.LocalActive.DTO.LocalActScheduleTemplateResponse;
import com.example.demo.demos.LocalActive.DTO.NearbyActivityDTO;
import com.example.demo.demos.LocalActive.Service.LocalActEnrollmentService;
import com.example.demo.demos.LocalActive.Service.LocalActivityService;
import com.example.demo.demos.LocalActive.Service.LocalScheduleTemplateService;
import com.example.demo.demos.LocalActive.Service.LocalActivitySearchService;
import com.example.demo.demos.generic.Resp;
import com.example.demo.demos.LocalActive.Pojo.LocalActivity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/local-act")
@RequiredArgsConstructor
public class LocalActEnrollmentController {

    private final LocalActEnrollmentService enrollmentService;
    private final LocalActivityService activityService;
    private final LocalScheduleTemplateService scheduleTemplateService;
    private final LocalActivitySearchService searchService;
    private final StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "获取用户报名记录")
    @GetMapping("/enrollments")
    public LocalActEnrollmentListResponse getUserEnrollments(
            @RequestParam("username") String username,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        LocalActEnrollmentQuery query = new LocalActEnrollmentQuery();
        query.setUsername(username);
        query.setStatus(status);
        query.setPeriod(period);
        query.setKeyword(keyword);
        return enrollmentService.getUserEnrollments(query);
    }

    @Operation(summary = "创建活动到管理员审核表")
    @PostMapping("/activities")
    public Resp<LocalActCreateResponse> createActivity(@RequestBody LocalActCreateRequest request) {
        return Resp.success(activityService.createActivity(request));
    }

    @Operation(summary = "创建固定日程模板")
    @PostMapping("/schedule-templates")
    public Resp<LocalActScheduleTemplateResponse> createScheduleTemplate(@RequestBody LocalActScheduleTemplateRequest request) {
        return Resp.success(scheduleTemplateService.createTemplate(request));
    }

    @Operation(summary = "查询活动列表")
    @GetMapping("/activities/list")
    public Resp<java.util.List<LocalActivity>> listActivities(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return Resp.success(activityService.listActivities(status, page, size));
    }

    @Operation(summary = "附近活动查询（使用 RediSearch）")
    @GetMapping("/activities/nearby")
    public Resp<java.util.List<NearbyActivityDTO>> searchNearby(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon,
            @RequestParam(value = "radiusKm", defaultValue = "50") double radiusKm,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        return Resp.success(searchService.searchNearby(lat, lon, radiusKm, category, keyword, size));
    }

    @Operation(summary = "批量同步活动到 Redis 索引")
    @PostMapping("/activities/sync-redis")
    public Resp<Integer> syncActivitiesToRedis() {
        return Resp.success(searchService.syncFromDb());
    }

    @Operation(summary = "Redis 连接测试")
    @GetMapping("/redis/ping")
    public Resp<String> redisPing() {
        try {
            String key = "test:local-act:ping";
            stringRedisTemplate.opsForValue().set(key, "pong", java.time.Duration.ofSeconds(30));
            String val = stringRedisTemplate.opsForValue().get(key);
            return Resp.success("write/read ok, value=" + val);
        } catch (Exception e) {
            return Resp.error(500, "redis error: " + e.getMessage());
        }
    }
}
