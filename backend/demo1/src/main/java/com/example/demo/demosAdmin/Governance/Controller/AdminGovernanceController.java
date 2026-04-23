package com.example.demo.demosAdmin.Governance.Controller;

import com.example.demo.demos.generic.Resp;
import com.example.demo.demosAdmin.Governance.Service.AdminGovernanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/admin/governance")
public class AdminGovernanceController {

    private final AdminGovernanceService service;

    public AdminGovernanceController(AdminGovernanceService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public Resp<Map<String, Object>> dashboard(@RequestParam(value = "days", defaultValue = "7") int days, @RequestParam(value = "releaseLimit", defaultValue = "8") int releaseLimit, @RequestParam(value = "versionLimit", defaultValue = "8") int versionLimit) {
        return ok(() -> service.dashboard(days, releaseLimit, versionLimit));
    }

    @GetMapping("/metrics/daily/list")
    public Resp<List<Map<String, Object>>> metricsDaily(@RequestParam(value = "page", defaultValue = "1") int page, @RequestParam(value = "size", defaultValue = "7") int size) {
        return ok(() -> service.metricsDaily(page, size));
    }

    @GetMapping("/metrics/daily/detail")
    public Resp<Map<String, Object>> metricsDailyDetail(@RequestParam(value = "date", required = false) String date) {
        return ok(() -> service.metricsDailyDetail(date));
    }

    @GetMapping("/metrics/error-attribution/trend")
    public Resp<List<Map<String, Object>>> errorTrend(@RequestParam(value = "days", defaultValue = "7") int days) {
        return ok(() -> service.errorTrend(days));
    }

    @GetMapping("/replay/list")
    public Resp<List<Map<String, Object>>> replayList(@RequestParam(value = "keyword", required = false) String keyword, @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ok(() -> service.replayList(keyword, limit));
    }

    @GetMapping("/replay/candidates")
    public Resp<List<Map<String, Object>>> replayCandidates(@RequestParam(value = "days", defaultValue = "7") int days, @RequestParam(value = "limit", defaultValue = "20") int limit, @RequestParam(value = "problematicOnly", defaultValue = "true") boolean problematicOnly, @RequestParam(value = "excludeExistingQuery", defaultValue = "true") boolean excludeExistingQuery) {
        return ok(() -> service.replayCandidates(days, limit, problematicOnly, excludeExistingQuery));
    }

    @GetMapping("/replay/request/{requestId}")
    public Resp<Map<String, Object>> replayByRequest(@PathVariable("requestId") String requestId) {
        return ok(() -> service.replayByRequestId(requestId));
    }

    @GetMapping("/replay/trace/{traceId}")
    public Resp<Map<String, Object>> replayByTrace(@PathVariable("traceId") String traceId) {
        return ok(() -> service.replayByTraceId(traceId));
    }

    @GetMapping("/replay/session/{sessionId}")
    public Resp<Map<String, Object>> replayBySession(@PathVariable("sessionId") String sessionId) {
        return ok(() -> service.replayBySessionId(sessionId));
    }

    @PostMapping("/eval-cases/bootstrap-batch-from-replay")
    public Resp<Map<String, Object>> bootstrapReplay(@RequestBody(required = false) Map<String, Object> body) {
        return ok(() -> service.bootstrapEvalCasesFromReplayBatch(safeBody(body)));
    }

    @GetMapping("/eval-cases/stats")
    public Resp<Map<String, Object>> evalCaseStats() {
        return ok(service::evalCaseStats);
    }

    @GetMapping("/eval-cases/list")
    public Resp<List<Map<String, Object>>> evalCases(@RequestParam(value = "keyword", required = false) String keyword, @RequestParam(value = "bucket", required = false) String bucket, @RequestParam(value = "riskLevel", required = false) String riskLevel, @RequestParam(value = "enabled", required = false) Integer enabled, @RequestParam(value = "page", defaultValue = "1") int page, @RequestParam(value = "size", defaultValue = "30") int size) {
        return ok(() -> service.evalCases(keyword, bucket, riskLevel, enabled, page, size));
    }

    @GetMapping("/eval-cases/export")
    public Resp<List<Map<String, Object>>> exportEvalCases(@RequestParam(value = "keyword", required = false) String keyword, @RequestParam(value = "bucket", required = false) String bucket, @RequestParam(value = "enabled", required = false) Integer enabled, @RequestParam(value = "limit", defaultValue = "200") int limit) {
        return ok(() -> service.exportEvalCases(keyword, bucket, enabled, limit));
    }

    @PostMapping("/eval-cases/add")
    public Resp<Map<String, Object>> addEvalCase(@RequestBody(required = false) Map<String, Object> body) {
        return ok(() -> service.createEvalCase(safeBody(body)));
    }

    @PostMapping("/eval-cases/import")
    public Resp<Map<String, Object>> importEvalCases(@RequestBody(required = false) Map<String, Object> body) {
        return ok(() -> service.importEvalCases(safeBody(body)));
    }

    @PostMapping("/eval-cases/update")
    public Resp<Map<String, Object>> updateEvalCase(@RequestBody(required = false) Map<String, Object> body) {
        return ok(() -> service.updateEvalCase(safeBody(body)));
    }

    @PostMapping("/eval-cases/delete/{id}")
    public Resp<Map<String, Object>> deleteEvalCase(@PathVariable("id") Long id) {
        return ok(() -> service.deleteEvalCase(id));
    }

    @PostMapping("/eval-cases/batch-enable")
    public Resp<Map<String, Object>> batchEnable(@RequestBody(required = false) Map<String, Object> body) {
        return ok(() -> service.batchToggleEvalCases(safeBody(body)));
    }

    @GetMapping("/eval-case-versions/list")
    public Resp<List<Map<String, Object>>> evalVersions(@RequestParam(value = "bucket", required = false) String bucket, @RequestParam(value = "page", defaultValue = "1") int page, @RequestParam(value = "size", defaultValue = "20") int size) {
        return ok(() -> service.evalVersions(bucket, page, size));
    }

    @PostMapping("/eval-case-versions/create")
    public Resp<Map<String, Object>> createEvalVersion(@RequestBody(required = false) Map<String, Object> body) {
        return ok(() -> service.createEvalCaseVersion(safeBody(body)));
    }

    @GetMapping("/eval-case-versions/{versionId}")
    public Resp<Map<String, Object>> evalVersionDetail(@PathVariable("versionId") Long versionId) {
        return ok(() -> service.evalVersionDetail(versionId));
    }

    @GetMapping("/eval-case-versions/compare")
    public Resp<Map<String, Object>> compareVersions(@RequestParam("baseVersionId") Long baseVersionId, @RequestParam("targetVersionId") Long targetVersionId) {
        return ok(() -> service.compareEvalCaseVersions(baseVersionId, targetVersionId));
    }

    @PostMapping("/eval-case-versions/{versionId}/run")
    public Resp<Map<String, Object>> runVersion(@PathVariable("versionId") Long versionId, @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return ok(() -> service.runEvalCaseVersion(versionId, limit));
    }

    @PostMapping("/eval-case-versions/run-batch")
    public Resp<Map<String, Object>> runVersionBatch(@RequestBody(required = false) Map<String, Object> body) {
        return ok(() -> service.runEvalCaseVersionBatch(safeBody(body)));
    }

    @GetMapping("/eval-runs/list")
    public Resp<List<Map<String, Object>>> evalRuns(@RequestParam(value = "bucket", required = false) String bucket, @RequestParam(value = "sourceType", required = false) String sourceType, @RequestParam(value = "versionId", required = false) Long versionId, @RequestParam(value = "regressionSetId", required = false) Long regressionSetId, @RequestParam(value = "page", defaultValue = "1") int page, @RequestParam(value = "size", defaultValue = "20") int size) {
        return ok(() -> service.evalRuns(bucket, sourceType, versionId, regressionSetId, page, size));
    }

    @GetMapping("/eval-runs/{runId}")
    public Resp<Map<String, Object>> evalRunDetail(@PathVariable("runId") Long runId) {
        return ok(() -> service.evalRunDetail(runId));
    }

    @GetMapping("/eval-runs/compare")
    public Resp<Map<String, Object>> compareRuns(@RequestParam("baseRunId") Long baseRunId, @RequestParam("targetRunId") Long targetRunId) {
        return ok(() -> service.compareEvalRuns(baseRunId, targetRunId));
    }
    @GetMapping("/regression-sets/list")
    public Resp<List<Map<String, Object>>> regressionSets(@RequestParam(value = "bucket", required = false) String bucket, @RequestParam(value = "riskLevel", required = false) String riskLevel, @RequestParam(value = "page", defaultValue = "1") int page, @RequestParam(value = "size", defaultValue = "20") int size) {
        return ok(() -> service.regressionSets(bucket, riskLevel, page, size));
    }

    @GetMapping("/regression-sets/{id}")
    public Resp<Map<String, Object>> regressionDetail(@PathVariable("id") Long id) {
        return ok(() -> service.regressionSetDetail(id));
    }

    @PostMapping("/regression-sets/create")
    public Resp<Map<String, Object>> createRegression(@RequestBody(required = false) Map<String, Object> body) {
        return ok(() -> service.createRegressionSet(safeBody(body)));
    }

    @PostMapping("/regression-sets/{id}/run")
    public Resp<Map<String, Object>> runRegression(@PathVariable("id") Long id, @RequestParam(value = "limit", required = false) Integer limit) {
        return ok(() -> service.runRegressionSet(id, limit));
    }

    @GetMapping("/release-records/list")
    public Resp<List<Map<String, Object>>> releaseRecords(@RequestParam(value = "status", required = false) String status, @RequestParam(value = "page", defaultValue = "1") int page, @RequestParam(value = "size", defaultValue = "20") int size) {
        return ok(() -> service.releaseRecords(status, page, size));
    }

    @PostMapping("/release-records/add")
    public Resp<Map<String, Object>> createRelease(@RequestBody(required = false) Map<String, Object> body) {
        return ok(() -> service.createReleaseRecord(safeBody(body)));
    }

    @PostMapping("/release-records/update")
    public Resp<Map<String, Object>> updateRelease(@RequestBody(required = false) Map<String, Object> body) {
        return ok(() -> service.updateReleaseRecord(safeBody(body)));
    }

    @GetMapping("/release-records/{id}")
    public Resp<Map<String, Object>> releaseRecord(@PathVariable("id") Long id) {
        return ok(() -> service.releaseRecord(id));
    }

    @GetMapping("/release-records/{id}/verification")
    public Resp<Map<String, Object>> releaseVerification(@PathVariable("id") Long id,
                                                         @RequestParam(value = "minEvalPassRate", defaultValue = "1") double minEvalPassRate,
                                                         @RequestParam(value = "maxRunAgeHours", defaultValue = "24") int maxRunAgeHours) {
        return ok(() -> service.releaseVerification(id, minEvalPassRate, maxRunAgeHours));
    }

    @GetMapping("/release-records/{id}/governance-summary")
    public Resp<Map<String, Object>> releaseGovernanceSummary(@PathVariable("id") Long id,
                                                              @RequestParam(value = "minEvalPassRate", defaultValue = "1") double minEvalPassRate,
                                                              @RequestParam(value = "maxRunAgeHours", defaultValue = "24") int maxRunAgeHours) {
        return ok(() -> service.releaseGovernanceSummary(id, minEvalPassRate, maxRunAgeHours));
    }

    @GetMapping("/release/preflight")
    public Resp<Map<String, Object>> releasePreflight(@RequestParam(value = "detailLimit", defaultValue = "10") int detailLimit,
                                                      @RequestParam(value = "taskLimit", defaultValue = "10") int taskLimit,
                                                      @RequestParam(value = "maxDegradedRate", defaultValue = "0.2") double maxDegradedRate,
                                                      @RequestParam(value = "evalCaseVersionId", required = false) Long evalCaseVersionId,
                                                      @RequestParam(value = "regressionSetId", required = false) Long regressionSetId,
                                                      @RequestParam(value = "minEvalPassRate", defaultValue = "1") double minEvalPassRate,
                                                      @RequestParam(value = "maxRunAgeHours", defaultValue = "24") int maxRunAgeHours) {
        return ok(() -> service.releasePreflight(detailLimit, taskLimit, maxDegradedRate, evalCaseVersionId, regressionSetId, minEvalPassRate, maxRunAgeHours));
    }

    @GetMapping("/release-records/{id}/events")
    public Resp<List<Map<String, Object>>> releaseEvents(@PathVariable("id") Long id) {
        return ok(() -> service.releaseEvents(id));
    }

    @PostMapping("/release-records/{id}/run-eval")
    public Resp<Map<String, Object>> runReleaseEval(@PathVariable("id") Long id, @RequestParam(value = "limit", defaultValue = "20") int limit, @RequestParam(value = "setAsBaseline", defaultValue = "false") boolean setAsBaseline) {
        return ok(() -> service.runReleaseEval(id, limit, setAsBaseline));
    }

    @GetMapping("/gray-configs/list")
    public Resp<List<Map<String, Object>>> grayConfigs(@RequestParam(value = "page", defaultValue = "1") int page, @RequestParam(value = "size", defaultValue = "20") int size, @RequestParam(value = "enabled", required = false) Integer enabled) {
        return ok(() -> service.grayConfigs(page, size, enabled));
    }

    @GetMapping("/gray-configs/{id}")
    public Resp<Map<String, Object>> grayConfig(@PathVariable("id") Long id) {
        return ok(() -> service.grayConfig(id));
    }

    @PostMapping("/gray-configs/add")
    public Resp<Map<String, Object>> createGray(@RequestBody(required = false) Map<String, Object> body) {
        return ok(() -> service.createGrayConfig(safeBody(body)));
    }

    @PostMapping("/gray-configs/update")
    public Resp<Map<String, Object>> updateGray(@RequestBody(required = false) Map<String, Object> body) {
        return ok(() -> service.updateGrayConfig(safeBody(body)));
    }

    @PostMapping("/gray-configs/delete/{id}")
    public Resp<Map<String, Object>> deleteGray(@PathVariable("id") Long id) {
        return ok(() -> service.deleteGrayConfig(id));
    }

    @PostMapping("/release-records/{releaseId}/apply-gray-config/{configId}")
    public Resp<Map<String, Object>> applyGray(@PathVariable("releaseId") Long releaseId, @PathVariable("configId") Long configId) {
        return ok(() -> service.applyGrayConfigToRelease(releaseId, configId));
    }

    @PostMapping("/release-records/{releaseId}/transition")
    public Resp<Map<String, Object>> transition(@PathVariable("releaseId") Long releaseId,
                                                @RequestParam("targetStatus") String targetStatus,
                                                @RequestParam(value = "grayConfigId", required = false) Long grayConfigId,
                                                @RequestParam(value = "minEvalPassRate", defaultValue = "1") double minEvalPassRate,
                                                @RequestParam(value = "maxRunAgeHours", defaultValue = "24") int maxRunAgeHours) {
        return ok(() -> service.transitionRelease(releaseId, targetStatus, grayConfigId, minEvalPassRate, maxRunAgeHours));
    }

    private Map<String, Object> safeBody(Map<String, Object> body) {
        return body == null ? new LinkedHashMap<String, Object>() : body;
    }

    private <T> Resp<T> ok(Supplier<T> supplier) {
        try {
            return Resp.success(supplier.get());
        } catch (IllegalArgumentException ex) {
            return Resp.error(400, ex.getMessage());
        } catch (Exception ex) {
            return Resp.error(500, ex.getMessage() == null ? "governance action failed" : ex.getMessage());
        }
    }
}
