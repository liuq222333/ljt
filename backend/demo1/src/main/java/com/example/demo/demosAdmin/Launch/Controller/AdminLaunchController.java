package com.example.demo.demosAdmin.Launch.Controller;

import com.example.demo.demos.generic.Resp;
import com.example.demo.demosAdmin.Launch.Service.AdminLaunchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/launch")
public class AdminLaunchController {

    private final AdminLaunchService adminLaunchService;

    public AdminLaunchController(AdminLaunchService adminLaunchService) {
        this.adminLaunchService = adminLaunchService;
    }

    @GetMapping("/readiness/overview")
    public Resp<Map<String, Object>> readinessOverview(@RequestParam(value = "detailLimit", defaultValue = "10") int detailLimit,
                                                       @RequestParam(value = "taskLimit", defaultValue = "10") int taskLimit,
                                                       @RequestParam(value = "maxDegradedRate", defaultValue = "0.2") double maxDegradedRate,
                                                       @RequestParam(value = "minEvalPassRate", defaultValue = "1") double minEvalPassRate,
                                                       @RequestParam(value = "maxRunAgeHours", defaultValue = "24") int maxRunAgeHours) {
        return Resp.success(adminLaunchService.readinessOverview(detailLimit, taskLimit, maxDegradedRate, minEvalPassRate, maxRunAgeHours));
    }

    @GetMapping("/checklist")
    public Resp<List<Map<String, Object>>> checklist(@RequestParam(value = "maxDegradedRate", defaultValue = "0.2") double maxDegradedRate,
                                                     @RequestParam(value = "minEvalPassRate", defaultValue = "1") double minEvalPassRate,
                                                     @RequestParam(value = "maxRunAgeHours", defaultValue = "24") int maxRunAgeHours) {
        return Resp.success(adminLaunchService.checklist(maxDegradedRate, minEvalPassRate, maxRunAgeHours));
    }

    @GetMapping("/load-tests/list")
    public Resp<List<Map<String, Object>>> listLoadTests(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return Resp.success(adminLaunchService.listLoadTests(limit));
    }

    @PostMapping("/load-tests/record")
    public Resp<Map<String, Object>> recordLoadTest(@RequestBody(required = false) Map<String, Object> body) {
        return Resp.success(adminLaunchService.recordLoadTest(body == null ? Collections.<String, Object>emptyMap() : body));
    }

    @PostMapping("/load-tests/run")
    public Resp<Map<String, Object>> runLoadTest(@RequestBody(required = false) Map<String, Object> body) {
        return Resp.success(adminLaunchService.runLoadTest(body == null ? Collections.<String, Object>emptyMap() : body));
    }

    @GetMapping("/drills/list")
    public Resp<List<Map<String, Object>>> listDrills(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return Resp.success(adminLaunchService.listDrills(limit));
    }

    @PostMapping("/drills/record")
    public Resp<Map<String, Object>> recordDrill(@RequestBody(required = false) Map<String, Object> body) {
        return Resp.success(adminLaunchService.recordDrill(body == null ? Collections.<String, Object>emptyMap() : body));
    }

    @PostMapping("/drills/run")
    public Resp<Map<String, Object>> runDrill(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> safeBody = body == null ? Collections.<String, Object>emptyMap() : body;
        int detailLimit = readInt(safeBody.get("detailLimit"), 10);
        int taskLimit = readInt(safeBody.get("taskLimit"), 10);
        double maxDegradedRate = readDouble(safeBody.get("maxDegradedRate"), 0.2D);
        double minEvalPassRate = readDouble(safeBody.get("minEvalPassRate"), 1D);
        Integer maxRunAgeHours = Integer.valueOf(readInt(safeBody.get("maxRunAgeHours"), 24));
        return Resp.success(adminLaunchService.runDrill(safeBody, detailLimit, taskLimit, maxDegradedRate, minEvalPassRate, maxRunAgeHours));
    }

    @GetMapping("/checklist-snapshots/list")
    public Resp<List<Map<String, Object>>> listChecklistSnapshots(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return Resp.success(adminLaunchService.listChecklistSnapshots(limit));
    }

    @GetMapping("/signoffs/list")
    public Resp<List<Map<String, Object>>> listSignoffs(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return Resp.success(adminLaunchService.listSignoffs(limit));
    }

    @GetMapping("/dependencies/list")
    public Resp<List<Map<String, Object>>> listDependencyChecks(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return Resp.success(adminLaunchService.listDependencyChecks(limit));
    }

    @GetMapping("/windows/list")
    public Resp<List<Map<String, Object>>> listLaunchWindows(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return Resp.success(adminLaunchService.listLaunchWindows(limit));
    }

    @GetMapping("/closeouts/list")
    public Resp<List<Map<String, Object>>> listCloseouts(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return Resp.success(adminLaunchService.listCloseouts(limit));
    }

    @PostMapping("/checklist-snapshots/create")
    public Resp<Map<String, Object>> createChecklistSnapshot(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> safeBody = body == null ? Collections.<String, Object>emptyMap() : body;
        double maxDegradedRate = readDouble(safeBody.get("maxDegradedRate"), 0.2D);
        double minEvalPassRate = readDouble(safeBody.get("minEvalPassRate"), 1D);
        Integer maxRunAgeHours = Integer.valueOf(readInt(safeBody.get("maxRunAgeHours"), 24));
        return Resp.success(adminLaunchService.createChecklistSnapshot(safeBody, maxDegradedRate, minEvalPassRate, maxRunAgeHours));
    }

    @PostMapping("/signoffs/record")
    public Resp<Map<String, Object>> recordSignoff(@RequestBody(required = false) Map<String, Object> body) {
        return Resp.success(adminLaunchService.recordSignoff(body == null ? Collections.<String, Object>emptyMap() : body));
    }

    @PostMapping("/dependencies/record")
    public Resp<Map<String, Object>> recordDependencyCheck(@RequestBody(required = false) Map<String, Object> body) {
        return Resp.success(adminLaunchService.recordDependencyCheck(body == null ? Collections.<String, Object>emptyMap() : body));
    }

    @PostMapping("/windows/create")
    public Resp<Map<String, Object>> createLaunchWindow(@RequestBody(required = false) Map<String, Object> body) {
        return Resp.success(adminLaunchService.createLaunchWindow(body == null ? Collections.<String, Object>emptyMap() : body));
    }

    @PostMapping("/windows/close")
    public Resp<Map<String, Object>> closeLaunchWindow(@RequestBody(required = false) Map<String, Object> body) {
        return Resp.success(adminLaunchService.closeLaunchWindow(body == null ? Collections.<String, Object>emptyMap() : body));
    }

    @PostMapping("/closeouts/create")
    public Resp<Map<String, Object>> createCloseout(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> safeBody = body == null ? Collections.<String, Object>emptyMap() : body;
        int detailLimit = readInt(safeBody.get("detailLimit"), 10);
        int taskLimit = readInt(safeBody.get("taskLimit"), 10);
        double maxDegradedRate = readDouble(safeBody.get("maxDegradedRate"), 0.2D);
        double minEvalPassRate = readDouble(safeBody.get("minEvalPassRate"), 1D);
        Integer maxRunAgeHours = Integer.valueOf(readInt(safeBody.get("maxRunAgeHours"), 24));
        int timelineLimit = readInt(safeBody.get("timelineLimit"), 20);
        return Resp.success(adminLaunchService.createCloseout(
                safeBody,
                detailLimit,
                taskLimit,
                maxDegradedRate,
                minEvalPassRate,
                maxRunAgeHours,
                timelineLimit
        ));
    }

    @GetMapping("/final-summary")
    public Resp<Map<String, Object>> finalSummary(@RequestParam(value = "detailLimit", defaultValue = "10") int detailLimit,
                                                  @RequestParam(value = "taskLimit", defaultValue = "10") int taskLimit,
                                                  @RequestParam(value = "maxDegradedRate", defaultValue = "0.2") double maxDegradedRate,
                                                  @RequestParam(value = "minEvalPassRate", defaultValue = "1") double minEvalPassRate,
                                                  @RequestParam(value = "maxRunAgeHours", defaultValue = "24") int maxRunAgeHours,
                                                  @RequestParam(value = "loadTestFreshnessHours", defaultValue = "72") int loadTestFreshnessHours,
                                                  @RequestParam(value = "drillFreshnessHours", defaultValue = "72") int drillFreshnessHours,
                                                  @RequestParam(value = "checklistFreshnessHours", defaultValue = "24") int checklistFreshnessHours) {
        return Resp.success(adminLaunchService.finalSummary(
                detailLimit,
                taskLimit,
                maxDegradedRate,
                minEvalPassRate,
                Integer.valueOf(maxRunAgeHours),
                Integer.valueOf(loadTestFreshnessHours),
                Integer.valueOf(drillFreshnessHours),
                Integer.valueOf(checklistFreshnessHours)
        ));
    }

    @GetMapping("/runbook/bundle")
    public Resp<Map<String, Object>> runbookBundle(@RequestParam(value = "detailLimit", defaultValue = "10") int detailLimit,
                                                   @RequestParam(value = "taskLimit", defaultValue = "10") int taskLimit,
                                                   @RequestParam(value = "maxDegradedRate", defaultValue = "0.2") double maxDegradedRate,
                                                   @RequestParam(value = "minEvalPassRate", defaultValue = "1") double minEvalPassRate,
                                                   @RequestParam(value = "maxRunAgeHours", defaultValue = "24") int maxRunAgeHours) {
        return Resp.success(adminLaunchService.runbookBundle(
                detailLimit,
                taskLimit,
                maxDegradedRate,
                minEvalPassRate,
                Integer.valueOf(maxRunAgeHours)
        ));
    }

    @GetMapping("/handoff-summary")
    public Resp<Map<String, Object>> handoffSummary(@RequestParam(value = "detailLimit", defaultValue = "10") int detailLimit,
                                                    @RequestParam(value = "taskLimit", defaultValue = "10") int taskLimit,
                                                    @RequestParam(value = "maxDegradedRate", defaultValue = "0.2") double maxDegradedRate,
                                                    @RequestParam(value = "minEvalPassRate", defaultValue = "1") double minEvalPassRate,
                                                    @RequestParam(value = "maxRunAgeHours", defaultValue = "24") int maxRunAgeHours) {
        return Resp.success(adminLaunchService.handoffSummary(
                detailLimit,
                taskLimit,
                maxDegradedRate,
                minEvalPassRate,
                Integer.valueOf(maxRunAgeHours)
        ));
    }

    @GetMapping("/timeline")
    public Resp<List<Map<String, Object>>> timeline(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return Resp.success(adminLaunchService.timeline(limit));
    }

    @GetMapping("/export/package")
    public Resp<Map<String, Object>> launchPackage(@RequestParam(value = "detailLimit", defaultValue = "10") int detailLimit,
                                                   @RequestParam(value = "taskLimit", defaultValue = "10") int taskLimit,
                                                   @RequestParam(value = "maxDegradedRate", defaultValue = "0.2") double maxDegradedRate,
                                                   @RequestParam(value = "minEvalPassRate", defaultValue = "1") double minEvalPassRate,
                                                   @RequestParam(value = "maxRunAgeHours", defaultValue = "24") int maxRunAgeHours,
                                                   @RequestParam(value = "timelineLimit", defaultValue = "20") int timelineLimit) {
        return Resp.success(adminLaunchService.launchPackage(
                detailLimit,
                taskLimit,
                maxDegradedRate,
                minEvalPassRate,
                Integer.valueOf(maxRunAgeHours),
                timelineLimit
        ));
    }

    @GetMapping("/realtime/gateway-config")
    public Resp<Map<String, Object>> realtimeGatewayConfig() {
        return Resp.success(adminLaunchService.gatewayConfigSnapshot());
    }

    @PostMapping("/smoke/run")
    public Resp<Map<String, Object>> smokeRun(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> safeBody = body == null ? Collections.<String, Object>emptyMap() : body;
        int detailLimit = readInt(safeBody.get("detailLimit"), 10);
        int taskLimit = readInt(safeBody.get("taskLimit"), 10);
        double maxDegradedRate = readDouble(safeBody.get("maxDegradedRate"), 0.2D);
        double minEvalPassRate = readDouble(safeBody.get("minEvalPassRate"), 1D);
        Integer maxRunAgeHours = Integer.valueOf(readInt(safeBody.get("maxRunAgeHours"), 24));
        return Resp.success(adminLaunchService.smokeRun(safeBody, detailLimit, taskLimit, maxDegradedRate, minEvalPassRate, maxRunAgeHours));
    }

    @PostMapping("/chaos/realtime/force-fallback")
    public Resp<Map<String, Object>> forceRealtimeFallback(@RequestBody(required = false) Map<String, Object> body) {
        return Resp.success(adminLaunchService.forceRealtimeFallback(body == null ? Collections.<String, Object>emptyMap() : body));
    }

    @PostMapping("/chaos/realtime/recover")
    public Resp<Map<String, Object>> recoverRealtimeFallback() {
        return Resp.success(adminLaunchService.recoverRealtimeFallback());
    }

    private int readInt(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignore) {
                return fallback;
            }
        }
        return fallback;
    }

    private double readDouble(Object value, double fallback) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignore) {
                return fallback;
            }
        }
        return fallback;
    }
}
