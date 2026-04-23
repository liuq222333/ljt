package com.example.demo.demosAdmin.Launch.Service;

import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.realtime.config.RealtimeQueryProperties;
import com.example.demo.demos.realtime.gateway.InternalRealtimeGatewayService;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.model.RealtimeResultItem;
import com.example.demo.demos.realtime.service.RealtimeQueryOrchestratorService;
import com.example.demo.demos.sync.SyncAdminService;
import com.example.demo.demosAdmin.Governance.Service.AdminGovernanceService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class AdminLaunchService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ENTRY_LOAD_TEST = "load_test";
    private static final String ENTRY_DRILL = "drill";
    private static final String ENTRY_CHECKLIST_SNAPSHOT = "checklist_snapshot";
    private static final String ENTRY_SIGNOFF = "signoff";
    private static final String ENTRY_DEPENDENCY_CHECK = "dependency_check";
    private static final String ENTRY_LAUNCH_WINDOW = "launch_window";
    private static final String ENTRY_CLOSEOUT = "launch_closeout";

    private final SyncAdminService syncAdminService;
    private final AdminGovernanceService adminGovernanceService;
    private final InternalRealtimeGatewayService internalRealtimeGatewayService;
    private final RealtimeQueryOrchestratorService realtimeQueryOrchestratorService;
    private final RealtimeQueryProperties realtimeQueryProperties;
    private final LaunchAdminStateStore launchAdminStateStore;

    public AdminLaunchService(SyncAdminService syncAdminService,
                              AdminGovernanceService adminGovernanceService,
                              InternalRealtimeGatewayService internalRealtimeGatewayService,
                              RealtimeQueryOrchestratorService realtimeQueryOrchestratorService,
                              RealtimeQueryProperties realtimeQueryProperties,
                              LaunchAdminStateStore launchAdminStateStore) {
        this.syncAdminService = syncAdminService;
        this.adminGovernanceService = adminGovernanceService;
        this.internalRealtimeGatewayService = internalRealtimeGatewayService;
        this.realtimeQueryOrchestratorService = realtimeQueryOrchestratorService;
        this.realtimeQueryProperties = realtimeQueryProperties;
        this.launchAdminStateStore = launchAdminStateStore;
    }

    public Map<String, Object> readinessOverview(int detailLimit,
                                                 int taskLimit,
                                                 double maxDegradedRate,
                                                 double minEvalPassRate,
                                                 Integer maxRunAgeHours) {
        int safeMaxRunAgeHours = sanitizeMaxRunAgeHours(maxRunAgeHours);
        Map<String, Object> syncBaseline = safeSyncBaselineReport();
        Map<String, Object> syncRepairStrategy = safeRepairStrategyReport();
        Map<String, Object> governanceDashboard = safeGovernanceDashboard();
        Map<String, Object> governancePreflight = safeGovernancePreflight(
                detailLimit,
                taskLimit,
                maxDegradedRate,
                minEvalPassRate,
                safeMaxRunAgeHours
        );
        Map<String, Object> gatewayHealth = internalRealtimeGatewayService.health();
        Map<String, Object> realtimeStatus = realtimeQueryOrchestratorService.status();
        List<Map<String, Object>> rollbackCandidates = safeRollbackCandidates();

        boolean syncReady = boolVal(syncBaseline.get("w12Ready"), false);
        boolean governanceReady = boolVal(governancePreflight.get("ready"), false);
        boolean realtimeServiceReady = "UP".equalsIgnoreCase(str(gatewayHealth.get("status")))
                && boolVal(gatewayHealth.get("productProviderReady"), false)
                && boolVal(realtimeStatus.get("enabled"), false)
                && !"OPEN".equalsIgnoreCase(str(asMap(realtimeStatus.get("circuit")).get("state")));
        boolean realtimeLaunchPathReady = boolVal(realtimeStatus.get("gatewayEnabled"), false)
                || ("internal".equalsIgnoreCase(str(gatewayHealth.get("gatewayMode"))) && boolVal(gatewayHealth.get("productProviderReady"), false));
        boolean rollbackReady = hasUsableRollbackCandidate(rollbackCandidates);
        boolean overallReady = syncReady && governanceReady && realtimeServiceReady && realtimeLaunchPathReady && rollbackReady;

        List<String> recommendedActions = new ArrayList<String>();
        appendUniqueStrings(recommendedActions, recommendationReasons(syncBaseline.get("repairRecommendations")));
        appendUniqueStrings(recommendedActions, asStringList(governancePreflight.get("recommended_actions")));
        if (!realtimeServiceReady) {
            recommendedActions.add("Reset realtime circuit or fix realtime service health.");
        }
        if (!realtimeLaunchPathReady) {
            recommendedActions.add("Prepare a usable realtime launch path before launch.");
        }
        if (!rollbackReady) {
            recommendedActions.add("Prepare at least one usable rollback candidate index.");
        }
        if (recommendedActions.isEmpty()) {
            recommendedActions.add("Launch readiness checks passed.");
        }

        return mapOf(
                "stage", "W14",
                "generatedAt", now(),
                "overallReady", overallReady,
                "gates", mapOf(
                        "syncReady", syncReady,
                        "governanceReady", governanceReady,
                        "realtimeServiceReady", realtimeServiceReady,
                        "realtimeLiveGatewayReady", realtimeLaunchPathReady,
                        "rollbackReady", rollbackReady,
                        "overallReady", overallReady
                ),
                "sync", mapOf(
                        "baseline", syncBaseline,
                        "repairStrategy", syncRepairStrategy
                ),
                "governance", mapOf(
                        "dashboard", governanceDashboard,
                        "preflight", governancePreflight,
                        "maxRunAgeHours", safeMaxRunAgeHours
                ),
                "realtime", mapOf(
                        "gatewayHealth", gatewayHealth,
                        "status", realtimeStatus,
                        "mode", realtimeMode(realtimeStatus),
                        "launchPathReady", realtimeLaunchPathReady
                ),
                "rollbackCandidates", rollbackCandidates,
                "recommendedActions", recommendedActions
        );
    }

    public List<Map<String, Object>> checklist(double maxDegradedRate,
                                               double minEvalPassRate,
                                               Integer maxRunAgeHours) {
        Map<String, Object> readiness = readinessOverview(10, 10, maxDegradedRate, minEvalPassRate, maxRunAgeHours);
        Map<String, Object> gates = asMap(readiness.get("gates"));
        Map<String, Object> sync = asMap(readiness.get("sync"));
        Map<String, Object> baseline = asMap(sync.get("baseline"));
        Map<String, Object> products = asMap(baseline.get("products"));
        Map<String, Object> tasks = asMap(baseline.get("tasks"));
        Map<String, Object> realtime = asMap(readiness.get("realtime"));
        Map<String, Object> realtimeStatus = asMap(realtime.get("status"));
        Map<String, Object> circuit = asMap(realtimeStatus.get("circuit"));

        List<Map<String, Object>> checklist = new ArrayList<Map<String, Object>>();
        checklist.add(item(
                "sync-baseline-clean",
                "Sync baseline clean",
                boolVal(gates.get("syncReady"), false),
                "W12 baseline report has no open mismatches or failed tasks.",
                boolVal(gates.get("syncReady"), false) ? "No action needed." : "Run sync repair strategy and clear failed/dead-letter tasks."
        ));
        checklist.add(item(
                "product-index-valid",
                "Product index validated",
                boolVal(asMap(products.get("indexValidation")).get("consistent"), false),
                "Read/write aliases and snapshot reconciliation stay consistent.",
                boolVal(asMap(products.get("indexValidation")).get("consistent"), false) ? "No action needed." : "Validate aliases and rebuild or rollback the product index."
        ));
        checklist.add(item(
                "sync-task-queue-clean",
                "Sync task queue clean",
                intVal(tasks.get("failed")) == 0 && intVal(tasks.get("deadLetterTotal")) == 0,
                "No failed or dead-letter sync tasks remain.",
                "Retry or repair failed sync tasks before launch."
        ));
        checklist.add(item(
                "governance-preflight-ready",
                "Governance preflight ready",
                boolVal(gates.get("governanceReady"), false),
                "Regression freshness, pass rate, and degraded rate all pass launch thresholds.",
                boolVal(gates.get("governanceReady"), false) ? "No action needed." : "Run the bound regression set again and fix failed cases."
        ));
        checklist.add(item(
                "realtime-service-ready",
                "Realtime service ready",
                boolVal(gates.get("realtimeServiceReady"), false),
                "Realtime service is enabled and the circuit is closed.",
                boolVal(gates.get("realtimeServiceReady"), false) ? "No action needed." : "Reset the realtime circuit and verify service health."
        ));
        checklist.add(item(
                "realtime-live-gateway",
                "Realtime launch path ready",
                boolVal(gates.get("realtimeLiveGatewayReady"), false),
                "A usable internal or live realtime gateway is configured for launch.",
                boolVal(gates.get("realtimeLiveGatewayReady"), false) ? "No action needed." : "Configure the internal realtime gateway or bind a live gateway endpoint."
        ));
        checklist.add(item(
                "rollback-candidate-ready",
                "Rollback candidate ready",
                boolVal(gates.get("rollbackReady"), false),
                "At least one usable rollback target index exists.",
                boolVal(gates.get("rollbackReady"), false) ? "No action needed." : "Prepare and validate a rollback candidate index."
        ));
        checklist.add(item(
                "overall-ready",
                "Overall launch ready",
                boolVal(gates.get("overallReady"), false),
                "All launch gates are green.",
                boolVal(gates.get("overallReady"), false) ? "Launch gates passed." : "Address failed gates before launch."
        ));
        checklist.add(mapOf(
                "id", "realtime-circuit-state",
                "label", "Realtime circuit state",
                "ready", !"OPEN".equalsIgnoreCase(str(circuit.get("state"))),
                "severity", "info",
                "detail", "Current circuit state: " + defaultIfBlank(str(circuit.get("state")), "unknown"),
                "action", "Use chaos reset or fix gateway failures if the circuit remains open."
        ));
        return checklist;
    }

    public Map<String, Object> smokeRun(Map<String, Object> requestBody,
                                        int detailLimit,
                                        int taskLimit,
                                        double maxDegradedRate,
                                        double minEvalPassRate,
                                        Integer maxRunAgeHours) {
        Map<String, Object> syncBaseline = safeSyncBaselineReport();
        Map<String, Object> governancePreflight = safeGovernancePreflight(
                detailLimit,
                taskLimit,
                maxDegradedRate,
                minEvalPassRate,
                sanitizeMaxRunAgeHours(maxRunAgeHours)
        );
        Map<String, Object> realtimeStatus = realtimeQueryOrchestratorService.status();
        RealtimeQueryResponse realtimeResponse;
        try {
            realtimeResponse = internalRealtimeGatewayService.query(
                    buildRealtimeRequest(extractRealtimeRequestBody(requestBody))
            );
        } catch (RuntimeException ex) {
            realtimeResponse = new RealtimeQueryResponse();
            realtimeResponse.setRealtimeStatus(RealtimeStatus.FAILED);
            realtimeResponse.setQueryMeta(mapOf(
                    "error", ex.getMessage(),
                    "source", "launch_smoke"
            ));
        }
        Map<String, Object> realtimeSmoke = summarizeResponse(realtimeResponse);
        boolean realtimeSuccess = isSuccessfulRealtimeResponse(realtimeResponse);
        realtimeSmoke.put("success", realtimeSuccess);
        realtimeSmoke.put("gatewayMode", "internal");

        boolean overallSuccess = boolVal(syncBaseline.get("w12Ready"), false)
                && boolVal(governancePreflight.get("ready"), false)
                && realtimeSuccess;

        List<String> actions = new ArrayList<String>();
        if (!boolVal(syncBaseline.get("w12Ready"), false)) {
            actions.add("Fix W12 baseline mismatches.");
        }
        if (!boolVal(governancePreflight.get("ready"), false)) {
            actions.add("Rerun governance preflight and resolve failed regression cases.");
        }
        if (!boolVal(realtimeSmoke.get("success"), false)) {
            actions.add("Fix realtime gateway or fallback path before launch.");
        }
        if (actions.isEmpty()) {
            actions.add("Smoke checks passed.");
        }

        return mapOf(
                "stage", "W14",
                "generatedAt", now(),
                "overallSuccess", overallSuccess,
                "sync", syncBaseline,
                "governance", governancePreflight,
                "realtime", mapOf(
                        "status", realtimeStatus,
                        "smoke", realtimeSmoke
                ),
                "recommendedActions", actions
        );
    }

    public Map<String, Object> forceRealtimeFallback(Map<String, Object> body) {
        Map<String, Object> requestBody = extractRealtimeRequestBody(body);
        int durationSeconds = intVal(body.get("durationSeconds"));
        String reason = defaultIfBlank(str(body.get("reason")), "w14_launch_chaos");
        Map<String, Object> forcedCircuit = realtimeQueryOrchestratorService.forceOpenCircuit(durationSeconds, reason);

        RealtimeQueryRequest request = buildRealtimeRequest(requestBody);
        request.setForceRefresh(true);
        request.setTraceId("launch-chaos-" + System.currentTimeMillis());
        RealtimeQueryResponse response = internalRealtimeGatewayService.query(request);

        boolean fallbackTriggered = boolVal(asMap(response.getQueryMeta()).get("gatewaySkipped"), false)
                || containsDegradedItem(response.getItems())
                || hasFallbackSource(response.getItems());

        return mapOf(
                "generatedAt", now(),
                "forcedCircuit", forcedCircuit,
                "request", summarizeRequest(request),
                "drill", summarizeResponse(response),
                "fallbackTriggered", fallbackTriggered
        );
    }

    public Map<String, Object> recoverRealtimeFallback() {
        return mapOf(
                "generatedAt", now(),
                "circuit", realtimeQueryOrchestratorService.resetCircuit(),
                "status", realtimeQueryOrchestratorService.status()
        );
    }

    public Map<String, Object> recordLoadTest(Map<String, Object> body) {
        Map<String, Object> safeBody = body == null ? Collections.<String, Object>emptyMap() : body;
        Map<String, Object> record = mapOf(
                "name", defaultIfBlank(str(safeBody.get("name")), "launch-load-test"),
                "environment", defaultIfBlank(str(safeBody.get("environment")), "unknown"),
                "operator", defaultIfBlank(str(safeBody.get("operator")), "launch-admin"),
                "passed", resolvePassed(safeBody),
                "status", resolveStatus(safeBody),
                "summary", defaultIfBlank(str(safeBody.get("summary")), "Load test recorded."),
                "qps", safeBody.get("qps"),
                "p95Ms", safeBody.get("p95Ms"),
                "errorRate", safeBody.get("errorRate"),
                "durationMinutes", safeBody.get("durationMinutes"),
                "notes", str(safeBody.get("notes")),
                "metrics", asMap(safeBody.get("metrics"))
        );
        return launchAdminStateStore.saveRecord(ENTRY_LOAD_TEST, "load-test", record);
    }

    public List<Map<String, Object>> listLoadTests(int limit) {
        return launchAdminStateStore.listRecords(ENTRY_LOAD_TEST, normalizeLimit(limit));
    }

    public Map<String, Object> runLoadTest(Map<String, Object> body) {
        Map<String, Object> safeBody = body == null ? Collections.<String, Object>emptyMap() : body;
        String name = defaultIfBlank(str(safeBody.get("name")), "launch-load-test");
        String environment = defaultIfBlank(str(safeBody.get("environment")), "unknown");
        String operator = defaultIfBlank(str(safeBody.get("operator")), "launch-admin");
        int iterations = normalizeIterations(intVal(safeBody.get("iterations")));
        int concurrency = normalizeConcurrency(intVal(safeBody.get("concurrency")));
        double maxDegradedRate = normalizeRate(safeBody.get("maxDegradedRate"), 0.2D);
        int totalRequests = iterations * concurrency;
        RealtimeQueryRequest template = buildRealtimeRequest(extractRealtimeRequestBody(safeBody));

        ExecutorService executorService = Executors.newFixedThreadPool(concurrency);
        List<Future<Map<String, Object>>> futures = new ArrayList<Future<Map<String, Object>>>();
        long startedAt = System.nanoTime();
        try {
            for (int i = 0; i < totalRequests; i++) {
                final int requestIndex = i;
                futures.add(executorService.submit(new Callable<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> call() {
                        RealtimeQueryRequest request = copyRealtimeRequest(template, requestIndex);
                        long callStartedAt = System.nanoTime();
                        try {
                            RealtimeQueryResponse response = internalRealtimeGatewayService.query(request);
                            return mapOf(
                                    "success", isSuccessfulRealtimeResponse(response),
                                    "degraded", isDegradedRealtimeResponse(response),
                                    "latencyMs", elapsedMs(callStartedAt),
                                    "status", response == null || response.getRealtimeStatus() == null
                                            ? "unknown"
                                            : response.getRealtimeStatus().getCode()
                            );
                        } catch (Exception ex) {
                            return mapOf(
                                    "success", false,
                                    "degraded", false,
                                    "latencyMs", elapsedMs(callStartedAt),
                                    "status", "exception",
                                    "error", ex.getMessage()
                            );
                        }
                    }
                }));
            }
        } finally {
            executorService.shutdown();
        }

        List<Long> latencies = new ArrayList<Long>();
        int successCount = 0;
        int failedCount = 0;
        int degradedCount = 0;
        List<String> errors = new ArrayList<String>();
        for (Future<Map<String, Object>> future : futures) {
            try {
                Map<String, Object> result = future.get();
                latencies.add(Long.valueOf(longVal(result.get("latencyMs"), 0L)));
                if (boolVal(result.get("success"), false)) {
                    successCount++;
                } else {
                    failedCount++;
                }
                if (boolVal(result.get("degraded"), false)) {
                    degradedCount++;
                }
                String error = str(result.get("error"));
                if (StringUtils.hasText(error) && errors.size() < 10) {
                    errors.add(error);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                failedCount++;
                if (errors.size() < 10) {
                    errors.add("load test interrupted");
                }
            } catch (ExecutionException ex) {
                failedCount++;
                if (errors.size() < 10) {
                    Throwable cause = ex.getCause();
                    errors.add(cause == null ? ex.getMessage() : cause.getMessage());
                }
            }
        }
        long elapsedMs = elapsedMs(startedAt);
        double degradedRate = totalRequests == 0 ? 0D : ((double) degradedCount / (double) totalRequests);
        boolean passed = failedCount == 0 && degradedRate <= maxDegradedRate;
        Map<String, Object> metrics = mapOf(
                "totalRequests", totalRequests,
                "iterations", iterations,
                "concurrency", concurrency,
                "successCount", successCount,
                "failedCount", failedCount,
                "degradedCount", degradedCount,
                "degradedRate", degradedRate,
                "avgMs", average(latencies),
                "p95Ms", percentile(latencies, 0.95D),
                "minMs", min(latencies),
                "maxMs", max(latencies),
                "elapsedMs", elapsedMs,
                "qps", elapsedMs <= 0 ? totalRequests : round2(totalRequests * 1000D / elapsedMs)
        );
        Map<String, Object> record = mapOf(
                "name", name,
                "environment", environment,
                "operator", operator,
                "passed", passed,
                "status", passed ? "passed" : "failed",
                "summary", "Load test " + (passed ? "passed" : "failed") + ": "
                        + successCount + "/" + totalRequests + " successful, p95=" + metrics.get("p95Ms") + "ms",
                "request", summarizeRequest(template),
                "metrics", metrics,
                "errors", errors,
                "mode", "executed"
        );
        return launchAdminStateStore.saveRecord(ENTRY_LOAD_TEST, "load-test", record);
    }

    public Map<String, Object> recordDrill(Map<String, Object> body) {
        Map<String, Object> safeBody = body == null ? Collections.<String, Object>emptyMap() : body;
        Map<String, Object> record = mapOf(
                "name", defaultIfBlank(str(safeBody.get("name")), "launch-drill"),
                "environment", defaultIfBlank(str(safeBody.get("environment")), "unknown"),
                "operator", defaultIfBlank(str(safeBody.get("operator")), "launch-admin"),
                "drillType", defaultIfBlank(str(safeBody.get("drillType")), "manual"),
                "target", defaultIfBlank(str(safeBody.get("target")), "unknown"),
                "passed", resolvePassed(safeBody),
                "status", resolveStatus(safeBody),
                "summary", defaultIfBlank(str(safeBody.get("summary")), "Launch drill recorded."),
                "recovered", boolVal(safeBody.get("recovered"), false),
                "notes", str(safeBody.get("notes")),
                "evidence", asMap(safeBody.get("evidence"))
        );
        return launchAdminStateStore.saveRecord(ENTRY_DRILL, "drill", record);
    }

    public List<Map<String, Object>> listDrills(int limit) {
        return launchAdminStateStore.listRecords(ENTRY_DRILL, normalizeLimit(limit));
    }

    public Map<String, Object> runDrill(Map<String, Object> body,
                                        int detailLimit,
                                        int taskLimit,
                                        double maxDegradedRate,
                                        double minEvalPassRate,
                                        Integer maxRunAgeHours) {
        Map<String, Object> safeBody = body == null ? Collections.<String, Object>emptyMap() : body;
        String drillType = defaultIfBlank(str(safeBody.get("drillType")), "realtime_fallback");
        String name = defaultIfBlank(str(safeBody.get("name")), "launch-drill-" + drillType);
        String environment = defaultIfBlank(str(safeBody.get("environment")), "unknown");
        String operator = defaultIfBlank(str(safeBody.get("operator")), "launch-admin");
        String target = defaultIfBlank(str(safeBody.get("target")), targetFromDrillType(drillType));

        Map<String, Object> evidence;
        boolean passed;
        boolean recovered = false;
        String summary;
        if ("realtime_fallback".equalsIgnoreCase(drillType)) {
            Map<String, Object> drill = forceRealtimeFallback(safeBody);
            Map<String, Object> recovery = recoverRealtimeFallback();
            evidence = mapOf(
                    "drill", drill,
                    "recovery", recovery
            );
            recovered = !"OPEN".equalsIgnoreCase(str(asMap(recovery.get("circuit")).get("state")));
            passed = boolVal(drill.get("fallbackTriggered"), false) && recovered;
            summary = passed ? "Realtime fallback drill passed." : "Realtime fallback drill failed.";
        } else if ("realtime_smoke".equalsIgnoreCase(drillType)) {
            Map<String, Object> smoke = smokeRun(safeBody, detailLimit, taskLimit, maxDegradedRate, minEvalPassRate, maxRunAgeHours);
            evidence = mapOf("smoke", smoke);
            passed = boolVal(smoke.get("overallSuccess"), false);
            recovered = true;
            summary = passed ? "Realtime smoke drill passed." : "Realtime smoke drill failed.";
        } else if ("sync_baseline".equalsIgnoreCase(drillType)) {
            Map<String, Object> baseline = safeSyncBaselineReport();
            evidence = mapOf("baseline", baseline);
            passed = boolVal(baseline.get("w12Ready"), false);
            recovered = true;
            summary = passed ? "Sync baseline drill passed." : "Sync baseline drill failed.";
        } else if ("governance_preflight".equalsIgnoreCase(drillType)) {
            Map<String, Object> preflight = safeGovernancePreflight(
                    detailLimit,
                    taskLimit,
                    maxDegradedRate,
                    minEvalPassRate,
                    sanitizeMaxRunAgeHours(maxRunAgeHours)
            );
            evidence = mapOf("preflight", preflight);
            passed = boolVal(preflight.get("ready"), false);
            recovered = true;
            summary = passed ? "Governance preflight drill passed." : "Governance preflight drill failed.";
        } else {
            evidence = mapOf(
                    "implemented", false,
                    "reason", "unsupported_drill_type"
            );
            passed = false;
            summary = "Unsupported drill type: " + drillType;
        }

        Map<String, Object> record = mapOf(
                "name", name,
                "environment", environment,
                "operator", operator,
                "drillType", drillType,
                "target", target,
                "passed", passed,
                "status", passed ? "passed" : "failed",
                "summary", summary,
                "recovered", recovered,
                "notes", str(safeBody.get("notes")),
                "evidence", evidence,
                "mode", "executed"
        );
        return launchAdminStateStore.saveRecord(ENTRY_DRILL, "drill", record);
    }

    public Map<String, Object> createChecklistSnapshot(Map<String, Object> body,
                                                       double maxDegradedRate,
                                                       double minEvalPassRate,
                                                       Integer maxRunAgeHours) {
        Map<String, Object> safeBody = body == null ? Collections.<String, Object>emptyMap() : body;
        Map<String, Object> readiness = readinessOverview(10, 10, maxDegradedRate, minEvalPassRate, maxRunAgeHours);
        List<Map<String, Object>> checklist = checklist(maxDegradedRate, minEvalPassRate, maxRunAgeHours);
        Map<String, Object> record = mapOf(
                "name", defaultIfBlank(str(safeBody.get("name")), "launch-checklist"),
                "environment", defaultIfBlank(str(safeBody.get("environment")), "unknown"),
                "operator", defaultIfBlank(str(safeBody.get("operator")), "launch-admin"),
                "passed", boolVal(readiness.get("overallReady"), false),
                "status", boolVal(readiness.get("overallReady"), false) ? "passed" : "failed",
                "summary", defaultIfBlank(str(safeBody.get("summary")), "Launch checklist snapshot generated."),
                "notes", str(safeBody.get("notes")),
                "readiness", readiness,
                "checklist", checklist
        );
        return launchAdminStateStore.saveRecord(ENTRY_CHECKLIST_SNAPSHOT, "checklist", record);
    }

    public List<Map<String, Object>> listChecklistSnapshots(int limit) {
        return launchAdminStateStore.listRecords(ENTRY_CHECKLIST_SNAPSHOT, normalizeLimit(limit));
    }

    public Map<String, Object> finalSummary(int detailLimit,
                                            int taskLimit,
                                            double maxDegradedRate,
                                            double minEvalPassRate,
                                            Integer maxRunAgeHours,
                                            Integer loadTestFreshnessHours,
                                            Integer drillFreshnessHours,
                                            Integer checklistFreshnessHours) {
        Map<String, Object> readiness = readinessOverview(detailLimit, taskLimit, maxDegradedRate, minEvalPassRate, maxRunAgeHours);
        Map<String, Object> latestLoadTest = launchAdminStateStore.latestRecord(ENTRY_LOAD_TEST);
        Map<String, Object> latestDrill = launchAdminStateStore.latestRecord(ENTRY_DRILL);
        Map<String, Object> latestChecklist = launchAdminStateStore.latestRecord(ENTRY_CHECKLIST_SNAPSHOT);

        int safeLoadFreshness = sanitizeFreshnessHours(loadTestFreshnessHours, 72);
        int safeDrillFreshness = sanitizeFreshnessHours(drillFreshnessHours, 72);
        int safeChecklistFreshness = sanitizeFreshnessHours(checklistFreshnessHours, 24);

        boolean loadTestReady = isFreshPassedRecord(latestLoadTest, safeLoadFreshness);
        boolean drillReady = isFreshPassedRecord(latestDrill, safeDrillFreshness);
        boolean checklistReady = isFreshPassedRecord(latestChecklist, safeChecklistFreshness);
        boolean readinessGate = boolVal(readiness.get("overallReady"), false);
        boolean finalReady = readinessGate && loadTestReady && drillReady && checklistReady;

        List<String> recommendedActions = new ArrayList<String>(asStringList(readiness.get("recommendedActions")));
        if (!loadTestReady) {
            recommendedActions.add("Run or refresh a passing load test record before launch.");
        }
        if (!drillReady) {
            recommendedActions.add("Run and record a successful launch drill before launch.");
        }
        if (!checklistReady) {
            recommendedActions.add("Generate a fresh passing launch checklist snapshot.");
        }
        if (recommendedActions.isEmpty()) {
            recommendedActions.add("Launch summary checks passed.");
        }

        return mapOf(
                "generatedAt", now(),
                "finalReady", finalReady,
                "gates", mapOf(
                        "readinessGate", readinessGate,
                        "loadTestReady", loadTestReady,
                        "drillReady", drillReady,
                        "checklistReady", checklistReady,
                        "finalReady", finalReady
                ),
                "freshnessHours", mapOf(
                        "loadTest", safeLoadFreshness,
                        "drill", safeDrillFreshness,
                        "checklist", safeChecklistFreshness
                ),
                "readiness", readiness,
                "latestLoadTest", latestLoadTest,
                "latestDrill", latestDrill,
                "latestChecklistSnapshot", latestChecklist,
                "recommendedActions", recommendedActions
        );
    }

    public Map<String, Object> runbookBundle(int detailLimit,
                                             int taskLimit,
                                             double maxDegradedRate,
                                             double minEvalPassRate,
                                             Integer maxRunAgeHours) {
        return mapOf(
                "generatedAt", now(),
                "finalSummary", finalSummary(detailLimit, taskLimit, maxDegradedRate, minEvalPassRate, maxRunAgeHours, 72, 72, 24),
                "documents", mapOf(
                        "opsRunbook", readLaunchDoc("launch/ops-runbook.md", "运维手册"),
                        "rollbackPlaybook", readLaunchDoc("launch/rollback-playbook.md", "回滚预案"),
                        "goLiveChecklist", readLaunchDoc("launch/go-live-checklist.md", "上线清单")
                )
        );
    }

    public Map<String, Object> handoffSummary(int detailLimit,
                            int taskLimit,
                            double maxDegradedRate,
                            double minEvalPassRate,
                            Integer maxRunAgeHours) {
        Map<String, Object> finalSummary = finalSummary(detailLimit, taskLimit, maxDegradedRate, minEvalPassRate, maxRunAgeHours, 72, 72, 24);
        Map<String, Object> latestLoadTest = launchAdminStateStore.latestRecord(ENTRY_LOAD_TEST);
        Map<String, Object> latestDrill = launchAdminStateStore.latestRecord(ENTRY_DRILL);
        Map<String, Object> latestChecklist = launchAdminStateStore.latestRecord(ENTRY_CHECKLIST_SNAPSHOT);
        List<Map<String, Object>> signoffs = listSignoffs(10);
        List<Map<String, Object>> dependencyChecks = listDependencyChecks(20);
        Map<String, Object> activeWindow = activeLaunchWindow();
        Map<String, Object> gatewayConfig = gatewayConfigSnapshot();
        Map<String, Object> runbookBundle = runbookBundle(detailLimit, taskLimit, maxDegradedRate, minEvalPassRate, maxRunAgeHours);

        return mapOf(
                "generatedAt", now(),
                "finalReady", finalSummary.get("finalReady"),
                "ownerActions", finalSummary.get("recommendedActions"),
                "latestArtifacts", mapOf(
                        "loadTest", latestLoadTest,
                        "drill", latestDrill,
                        "checklistSnapshot", latestChecklist
                ),
                "signoffOverview", signoffOverview(signoffs),
                "signoffs", signoffs,
                "dependencyOverview", dependencyOverview(dependencyChecks),
                "activeLaunchWindow", activeWindow,
                "gatewayConfig", gatewayConfig,
                "documents", asMap(runbookBundle.get("documents")),
                "summary", finalSummary
        );
    }

    public List<Map<String, Object>> timeline(int limit) {
        int safeLimit = limit <= 0 ? 20 : limit;
        List<Map<String, Object>> timeline = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> record : listOrEmpty(launchAdminStateStore.listRecords(ENTRY_LOAD_TEST, safeLimit))) {
            timeline.add(buildTimelineItem("load_test", record, defaultIfBlank(str(record.get("name")), str(record.get("id"))),
                    defaultIfBlank(str(record.get("summary")), "Load test record"),
                    str(record.get("updatedAt"))));
        }
        for (Map<String, Object> record : listOrEmpty(launchAdminStateStore.listRecords(ENTRY_DRILL, safeLimit))) {
            timeline.add(buildTimelineItem("drill", record, defaultIfBlank(str(record.get("name")), str(record.get("id"))),
                    defaultIfBlank(str(record.get("summary")), "Launch drill record"),
                    str(record.get("updatedAt"))));
        }
        for (Map<String, Object> record : listOrEmpty(launchAdminStateStore.listRecords(ENTRY_CHECKLIST_SNAPSHOT, safeLimit))) {
            timeline.add(buildTimelineItem("checklist_snapshot", record, defaultIfBlank(str(record.get("name")), str(record.get("id"))),
                    defaultIfBlank(str(record.get("summary")), "Checklist snapshot"),
                    str(record.get("updatedAt"))));
        }
        for (Map<String, Object> record : listOrEmpty(launchAdminStateStore.listRecords(ENTRY_SIGNOFF, safeLimit))) {
            timeline.add(buildTimelineItem("signoff", record,
                    defaultIfBlank(str(record.get("signoffRole")), str(record.get("id"))),
                    defaultIfBlank(str(record.get("summary")), "Launch signoff"),
                    str(record.get("updatedAt"))));
        }
        for (Map<String, Object> record : listOrEmpty(launchAdminStateStore.listRecords(ENTRY_DEPENDENCY_CHECK, safeLimit))) {
            timeline.add(buildTimelineItem("dependency_check", record,
                    defaultIfBlank(str(record.get("dependencyName")), str(record.get("id"))),
                    defaultIfBlank(str(record.get("summary")), "Dependency check"),
                    str(record.get("updatedAt"))));
        }
        for (Map<String, Object> record : listOrEmpty(launchAdminStateStore.listRecords(ENTRY_LAUNCH_WINDOW, safeLimit))) {
            timeline.add(buildTimelineItem("launch_window", record,
                    defaultIfBlank(str(record.get("windowName")), str(record.get("id"))),
                    defaultIfBlank(str(record.get("summary")), "Launch window"),
                    str(record.get("updatedAt"))));
        }
        for (Map<String, Object> record : listOrEmpty(adminGovernanceService.releaseRecords("", 1, safeLimit))) {
            timeline.add(buildTimelineItem("release_record", record, defaultIfBlank(str(record.get("releaseName")), str(record.get("id"))),
                    defaultIfBlank(str(record.get("releaseStatus")), "release"),
                    defaultIfBlank(str(record.get("updatedAt")), str(record.get("createdAt")))));
        }
        for (Map<String, Object> record : listOrEmpty(adminGovernanceService.evalRuns("", "", null, null, 1, safeLimit))) {
            timeline.add(buildTimelineItem("eval_run", record, "Eval Run #" + str(record.get("id")),
                    "passRate=" + str(record.get("passRate")) + ", failed=" + str(record.get("failedTotal")),
                    defaultIfBlank(str(record.get("createdAt")), str(record.get("updatedAt")))));
        }
        Collections.sort(timeline, new java.util.Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                return defaultIfBlank(str(right.get("timestamp")), "").compareTo(defaultIfBlank(str(left.get("timestamp")), ""));
            }
        });
        if (timeline.size() > safeLimit) {
            return new ArrayList<Map<String, Object>>(timeline.subList(0, safeLimit));
        }
        return timeline;
    }

    public Map<String, Object> launchPackage(int detailLimit,
                                             int taskLimit,
                                             double maxDegradedRate,
                                             double minEvalPassRate,
                                             Integer maxRunAgeHours,
                                             int timelineLimit) {
        return mapOf(
                "generatedAt", now(),
                "readiness", readinessOverview(detailLimit, taskLimit, maxDegradedRate, minEvalPassRate, maxRunAgeHours),
                "checklist", checklist(maxDegradedRate, minEvalPassRate, maxRunAgeHours),
                "finalSummary", finalSummary(detailLimit, taskLimit, maxDegradedRate, minEvalPassRate, maxRunAgeHours, 72, 72, 24),
                "handoffSummary", handoffSummary(detailLimit, taskLimit, maxDegradedRate, minEvalPassRate, maxRunAgeHours),
                "runbookBundle", runbookBundle(detailLimit, taskLimit, maxDegradedRate, minEvalPassRate, maxRunAgeHours),
                "gatewayConfig", gatewayConfigSnapshot(),
                "dependencyChecks", listDependencyChecks(timelineLimit),
                "activeLaunchWindow", activeLaunchWindow(),
                "timeline", timeline(timelineLimit)
        );
    }

    public Map<String, Object> gatewayConfigSnapshot() {
        return mapOf(
                "gatewayMode", "internal",
                "internalGatewayHealth", internalRealtimeGatewayService.health(),
                "enabled", realtimeQueryProperties.isEnabled(),
                "gatewayEnabled", realtimeQueryProperties.isGatewayEnabled(),
                "mockGatewayEnabled", realtimeQueryProperties.isMockGatewayEnabled(),
                "gatewayBaseUrl", realtimeQueryProperties.getGatewayBaseUrl(),
                "gatewayQueryPath", realtimeQueryProperties.getGatewayQueryPath(),
                "gatewayConnectTimeoutMs", realtimeQueryProperties.getGatewayConnectTimeoutMs(),
                "gatewayReadTimeoutMs", realtimeQueryProperties.getGatewayReadTimeoutMs(),
                "gatewayRetryCount", realtimeQueryProperties.getGatewayRetryCount(),
                "cacheTtlSeconds", realtimeQueryProperties.getCacheTtlSeconds(),
                "circuitFailureThreshold", realtimeQueryProperties.getCircuitFailureThreshold(),
                "circuitOpenSeconds", realtimeQueryProperties.getCircuitOpenSeconds()
        );
    }

    public List<Map<String, Object>> listSignoffs(int limit) {
        return listOrEmpty(launchAdminStateStore.listRecords(ENTRY_SIGNOFF, limit));
    }

    public Map<String, Object> recordSignoff(Map<String, Object> requestBody) {
        Map<String, Object> body = safeBody(requestBody);
        String signoffRole = defaultIfBlank(str(body.get("signoffRole")), str(body.get("role")));
        if (!StringUtils.hasText(signoffRole)) {
            throw new IllegalArgumentException("signoffRole is required");
        }
        boolean approved = body.get("approved") == null || boolVal(body.get("approved"), false);
        String operator = defaultIfBlank(str(body.get("operator")), "front-admin");
        String notes = str(body.get("notes"));
        Map<String, Object> record = mapOf(
                "signoffRole", signoffRole,
                "operator", operator,
                "approved", approved,
                "status", approved ? "approved" : "rejected",
                "notes", notes,
                "summary", approved
                        ? signoffRole + " signed off by " + operator
                        : signoffRole + " rejected by " + operator
        );
        return launchAdminStateStore.saveRecord(ENTRY_SIGNOFF, "signoff", record);
    }

    public List<Map<String, Object>> listDependencyChecks(int limit) {
        return listOrEmpty(launchAdminStateStore.listRecords(ENTRY_DEPENDENCY_CHECK, limit));
    }

    public Map<String, Object> recordDependencyCheck(Map<String, Object> requestBody) {
        Map<String, Object> body = safeBody(requestBody);
        String dependencyName = defaultIfBlank(str(body.get("dependencyName")), str(body.get("name")));
        if (!StringUtils.hasText(dependencyName)) {
            throw new IllegalArgumentException("dependencyName is required");
        }
        boolean ready = body.get("ready") == null || boolVal(body.get("ready"), false);
        String operator = defaultIfBlank(str(body.get("operator")), "front-admin");
        String notes = str(body.get("notes"));
        Map<String, Object> record = mapOf(
                "dependencyName", dependencyName,
                "operator", operator,
                "ready", ready,
                "status", ready ? "ready" : "blocked",
                "notes", notes,
                "summary", ready
                        ? dependencyName + " checked by " + operator
                        : dependencyName + " blocked by " + operator
        );
        return launchAdminStateStore.saveRecord(ENTRY_DEPENDENCY_CHECK, "dependency", record);
    }

    public List<Map<String, Object>> listLaunchWindows(int limit) {
        return listOrEmpty(launchAdminStateStore.listRecords(ENTRY_LAUNCH_WINDOW, limit));
    }

    public List<Map<String, Object>> listCloseouts(int limit) {
        return listOrEmpty(launchAdminStateStore.listRecords(ENTRY_CLOSEOUT, limit));
    }

    public Map<String, Object> createCloseout(Map<String, Object> body,
                                              int detailLimit,
                                              int taskLimit,
                                              double maxDegradedRate,
                                              double minEvalPassRate,
                                              Integer maxRunAgeHours,
                                              int timelineLimit) {
        Map<String, Object> safeBody = safeBody(body);
        Map<String, Object> finalSummary = finalSummary(
                detailLimit,
                taskLimit,
                maxDegradedRate,
                minEvalPassRate,
                maxRunAgeHours,
                Integer.valueOf(72),
                Integer.valueOf(72),
                Integer.valueOf(24)
        );
        Map<String, Object> handoffSummary = handoffSummary(
                detailLimit,
                taskLimit,
                maxDegradedRate,
                minEvalPassRate,
                maxRunAgeHours
        );
        Map<String, Object> launchPackage = launchPackage(
                detailLimit,
                taskLimit,
                maxDegradedRate,
                minEvalPassRate,
                maxRunAgeHours,
                timelineLimit
        );
        boolean finalReady = boolVal(finalSummary.get("finalReady"), false);
        Map<String, Object> record = mapOf(
                "name", defaultIfBlank(str(safeBody.get("name")), "launch-closeout"),
                "environment", defaultIfBlank(str(safeBody.get("environment")), "unknown"),
                "operator", defaultIfBlank(str(safeBody.get("operator")), "launch-admin"),
                "passed", finalReady,
                "finalReady", finalReady,
                "status", finalReady ? "ready_for_go_live" : "blocked",
                "summary", finalReady ? "Launch closeout package is ready." : "Launch closeout package is blocked.",
                "notes", str(safeBody.get("notes")),
                "finalSummary", finalSummary,
                "handoffSummary", handoffSummary,
                "launchPackage", launchPackage
        );
        return launchAdminStateStore.saveRecord(ENTRY_CLOSEOUT, "closeout", record);
    }

    public Map<String, Object> createLaunchWindow(Map<String, Object> requestBody) {
        Map<String, Object> body = safeBody(requestBody);
        String windowName = defaultIfBlank(str(body.get("windowName")), str(body.get("name")));
        if (!StringUtils.hasText(windowName)) {
            throw new IllegalArgumentException("windowName is required");
        }
        String operator = defaultIfBlank(str(body.get("operator")), "front-admin");
        String startAt = defaultIfBlank(str(body.get("startAt")), now());
        Map<String, Object> record = mapOf(
                "windowName", windowName,
                "operator", operator,
                "startAt", startAt,
                "endAt", str(body.get("endAt")),
                "status", "open",
                "notes", str(body.get("notes")),
                "summary", "Launch window opened by " + operator
        );
        return launchAdminStateStore.saveRecord(ENTRY_LAUNCH_WINDOW, "window", record);
    }

    public Map<String, Object> closeLaunchWindow(Map<String, Object> requestBody) {
        Map<String, Object> body = safeBody(requestBody);
        String id = str(body.get("id"));
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("id is required");
        }
        Map<String, Object> existing = mutableRecord(ENTRY_LAUNCH_WINDOW, id);
        existing.put("status", defaultIfBlank(str(body.get("status")), "closed"));
        existing.put("endAt", defaultIfBlank(str(body.get("endAt")), now()));
        if (body.containsKey("notes")) {
            existing.put("notes", str(body.get("notes")));
        }
        existing.put("summary", "Launch window " + id + " " + str(existing.get("status")));
        return launchAdminStateStore.saveRecord(ENTRY_LAUNCH_WINDOW, "window", existing);
    }

    private Map<String, Object> safeSyncBaselineReport() {
        try {
            return syncAdminService.baselineReport();
        } catch (RuntimeException ex) {
            return mapOf(
                    "w12Ready", false,
                    "error", ex.getMessage(),
                    "products", mapOf("indexValidation", mapOf("consistent", false)),
                    "tasks", mapOf("failed", 1, "deadLetterTotal", 0),
                    "repairRecommendations", Collections.singletonList(mapOf(
                            "reason", "Sync baseline unavailable: " + ex.getMessage(),
                            "action", "Restore search and sync dependencies before launch."
                    ))
            );
        }
    }

    private Map<String, Object> safeRepairStrategyReport() {
        try {
            return syncAdminService.repairStrategyReport();
        } catch (RuntimeException ex) {
            return mapOf(
                    "ready", false,
                    "error", ex.getMessage(),
                    "recommendations", Collections.singletonList(mapOf(
                            "reason", "Repair strategy unavailable: " + ex.getMessage(),
                            "action", "Investigate sync admin dependencies."
                    ))
            );
        }
    }

    private Map<String, Object> safeGovernanceDashboard() {
        try {
            return adminGovernanceService.dashboard(7, 8, 8);
        } catch (RuntimeException ex) {
            return mapOf(
                    "ready", false,
                    "error", ex.getMessage(),
                    "overview", mapOf("stage", "W13", "error", ex.getMessage())
            );
        }
    }

    private Map<String, Object> safeGovernancePreflight(int detailLimit,
                                                        int taskLimit,
                                                        double maxDegradedRate,
                                                        double minEvalPassRate,
                                                        int maxRunAgeHours) {
        try {
            return adminGovernanceService.releasePreflight(
                    detailLimit,
                    taskLimit,
                    maxDegradedRate,
                    null,
                    null,
                    minEvalPassRate,
                    maxRunAgeHours
            );
        } catch (RuntimeException ex) {
            return mapOf(
                    "ready", false,
                    "error", ex.getMessage(),
                    "recommended_actions", Collections.singletonList(
                            "Governance preflight unavailable: " + ex.getMessage()
                    )
            );
        }
    }

    private List<Map<String, Object>> safeRollbackCandidates() {
        try {
            return syncAdminService.listProductRollbackCandidates();
        } catch (RuntimeException ex) {
            return Collections.singletonList(mapOf(
                    "index", "",
                    "exists", false,
                    "documentCount", 0L,
                    "error", ex.getMessage()
            ));
        }
    }

    private Map<String, Object> item(String id, String label, boolean ready, String detail, String action) {
        return mapOf(
                "id", id,
                "label", label,
                "ready", ready,
                "severity", ready ? "info" : "warning",
                "detail", detail,
                "action", action
        );
    }

    private Map<String, Object> buildTimelineItem(String type,
                                                  Map<String, Object> record,
                                                  String title,
                                                  String summary,
                                                  String timestamp) {
        return mapOf(
                "type", type,
                "id", record.get("id"),
                "title", title,
                "summary", summary,
                "status", defaultIfBlank(str(record.get("status")), defaultIfBlank(str(record.get("releaseStatus")),
                        boolVal(record.get("passed"), false) ? "passed" : "pending")),
                "timestamp", timestamp,
                "record", record
        );
    }

    private Map<String, Object> signoffOverview(List<Map<String, Object>> signoffs) {
        Map<String, Object> latestByRole = new LinkedHashMap<String, Object>();
        for (Map<String, Object> signoff : signoffs) {
            String roleKey = defaultIfBlank(str(signoff.get("signoffRole")), str(signoff.get("id")));
            if (StringUtils.hasText(roleKey) && !latestByRole.containsKey(roleKey)) {
                latestByRole.put(roleKey, signoff);
            }
        }
        int approved = 0;
        int rejected = 0;
        for (Object value : latestByRole.values()) {
            Map<String, Object> latest = asMap(value);
            if (boolVal(latest.get("approved"), false)) {
                approved++;
            } else {
                rejected++;
            }
        }
        return mapOf(
                "total", latestByRole.size(),
                "recordTotal", signoffs.size(),
                "approvedTotal", approved,
                "rejectedTotal", rejected,
                "latestByRole", latestByRole
        );
    }

    private Map<String, Object> dependencyOverview(List<Map<String, Object>> checks) {
        Map<String, Object> latestByName = new LinkedHashMap<String, Object>();
        for (Map<String, Object> check : checks) {
            String dependencyKey = defaultIfBlank(str(check.get("dependencyName")), str(check.get("id")));
            if (StringUtils.hasText(dependencyKey) && !latestByName.containsKey(dependencyKey)) {
                latestByName.put(dependencyKey, check);
            }
        }
        int ready = 0;
        int blocked = 0;
        for (Object value : latestByName.values()) {
            Map<String, Object> latest = asMap(value);
            if (boolVal(latest.get("ready"), false)) {
                ready++;
            } else {
                blocked++;
            }
        }
        return mapOf(
                "total", latestByName.size(),
                "recordTotal", checks.size(),
                "readyTotal", ready,
                "blockedTotal", blocked,
                "latestByName", latestByName
        );
    }

    private Map<String, Object> activeLaunchWindow() {
        for (Map<String, Object> item : listLaunchWindows(20)) {
            if ("open".equalsIgnoreCase(str(item.get("status")))) {
                return item;
            }
        }
        return Collections.emptyMap();
    }

    private Map<String, Object> summarizeRequest(RealtimeQueryRequest request) {
        return mapOf(
                "entityType", request.getEntityType(),
                "entityIds", request.getEntityIds(),
                "queryType", request.getQueryType(),
                "date", request.getDate(),
                "timeSlot", request.getTimeSlot(),
                "traceId", request.getTraceId(),
                "forceRefresh", request.isForceRefresh()
        );
    }

    private Map<String, Object> summarizeResponse(RealtimeQueryResponse response) {
        Map<String, Object> queryMeta = asMap(response.getQueryMeta());
        Set<String> sources = new LinkedHashSet<String>();
        int degradedCount = 0;
        for (RealtimeResultItem item : response.getItems()) {
            if (StringUtils.hasText(item.getSource())) {
                sources.add(item.getSource());
            }
            if (item.isDegraded()) {
                degradedCount++;
            }
        }
        return mapOf(
                "realtimeStatus", response.getRealtimeStatus() == null ? null : response.getRealtimeStatus().getCode(),
                "itemCount", response.getItems().size(),
                "degradedCount", degradedCount,
                "partialFailedIds", response.getPartialFailedIds(),
                "sources", new ArrayList<String>(sources),
                "queryMeta", queryMeta
        );
    }

    private boolean containsDegradedItem(List<RealtimeResultItem> items) {
        for (RealtimeResultItem item : items) {
            if (item.isDegraded()) {
                return true;
            }
        }
        return false;
    }

    private boolean isSuccessfulRealtimeResponse(RealtimeQueryResponse response) {
        if (response == null || response.getRealtimeStatus() == null) {
            return false;
        }
        return response.getRealtimeStatus() == com.example.demo.demos.common.enums.RealtimeStatus.SUCCESS
                || response.getRealtimeStatus() == com.example.demo.demos.common.enums.RealtimeStatus.PARTIAL_SUCCESS;
    }

    private boolean hasFallbackSource(List<RealtimeResultItem> items) {
        for (RealtimeResultItem item : items) {
            if (StringUtils.hasText(item.getSource()) && item.getSource().toLowerCase().contains("fallback")) {
                return true;
            }
        }
        return false;
    }

    private RealtimeQueryRequest buildRealtimeRequest(Map<String, Object> body) {
        RealtimeQueryRequest request = new RealtimeQueryRequest();
        request.setEntityType(defaultIfBlank(str(body.get("entityType")), "product"));
        request.setEntityIds(longList(body.get("entityIds")));
        if (CollectionUtils.isEmpty(request.getEntityIds())) {
            request.setEntityIds(Collections.singletonList(1L));
        }
        request.setQueryType(defaultIfBlank(str(body.get("queryType")), "availability"));
        request.setDate(str(body.get("date")));
        request.setTimeSlot(str(body.get("timeSlot")));
        request.setUserId(defaultIfBlank(str(body.get("userId")), "launch-admin"));
        int timeoutMs = intVal(body.get("timeoutMs"));
        request.setTimeoutMs(timeoutMs <= 0 ? null : Integer.valueOf(timeoutMs));
        return request;
    }

    private boolean hasUsableRollbackCandidate(List<Map<String, Object>> rollbackCandidates) {
        for (Map<String, Object> candidate : rollbackCandidates) {
            if (boolVal(candidate.get("exists"), false) && longVal(candidate.get("documentCount"), 0L) > 0L) {
                return true;
            }
        }
        return false;
    }

    private String realtimeMode(Map<String, Object> realtimeStatus) {
        if (!boolVal(realtimeStatus.get("enabled"), false)) {
            return "disabled";
        }
        if (boolVal(realtimeStatus.get("gatewayEnabled"), false)) {
            return "live_gateway";
        }
        if (boolVal(realtimeStatus.get("mockGatewayEnabled"), false)) {
            return "mock_gateway";
        }
        return "fallback_only";
    }

    private void appendUniqueStrings(List<String> target, List<String> additions) {
        for (String value : additions) {
            if (StringUtils.hasText(value) && !target.contains(value)) {
                target.add(value);
            }
        }
    }

    private Map<String, Object> extractRealtimeRequestBody(Map<String, Object> body) {
        Map<String, Object> nested = asMap(body.get("realtimeRequest"));
        return nested.isEmpty() ? body : nested;
    }

    private List<String> asStringList(Object value) {
        if (value instanceof List) {
            List<String> result = new ArrayList<String>();
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private List<String> recommendationReasons(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map) {
                Map<String, Object> recommendation = asMap(item);
                String reason = defaultIfBlank(str(recommendation.get("reason")), str(recommendation.get("action")));
                if (StringUtils.hasText(reason)) {
                    result.add(reason);
                }
            } else if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private int sanitizeMaxRunAgeHours(Integer maxRunAgeHours) {
        return maxRunAgeHours == null || maxRunAgeHours.intValue() <= 0 ? 24 : maxRunAgeHours.intValue();
    }

    private int sanitizeFreshnessHours(Integer value, int fallback) {
        return value == null || value.intValue() <= 0 ? fallback : value.intValue();
    }

    private int normalizeIterations(int value) {
        if (value <= 0) {
            return 5;
        }
        return Math.min(value, 50);
    }

    private int normalizeConcurrency(int value) {
        if (value <= 0) {
            return 2;
        }
        return Math.min(value, 8);
    }

    private double normalizeRate(Object value, double fallback) {
        if (value instanceof Number) {
            return Math.max(0D, ((Number) value).doubleValue());
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            try {
                return Math.max(0D, Double.parseDouble((String) value));
            } catch (NumberFormatException ignore) {
                return fallback;
            }
        }
        return fallback;
    }

    private int normalizeLimit(int limit) {
        return limit <= 0 ? 20 : limit;
    }

    private String targetFromDrillType(String drillType) {
        if ("sync_baseline".equalsIgnoreCase(drillType)) {
            return "sync";
        }
        if ("governance_preflight".equalsIgnoreCase(drillType)) {
            return "governance";
        }
        return "realtime";
    }

    private RealtimeQueryRequest copyRealtimeRequest(RealtimeQueryRequest template, int requestIndex) {
        RealtimeQueryRequest copy = new RealtimeQueryRequest();
        copy.setEntityType(template.getEntityType());
        copy.setEntityIds(template.getEntityIds() == null ? Collections.<Long>emptyList() : new ArrayList<Long>(template.getEntityIds()));
        copy.setQueryType(template.getQueryType());
        copy.setDate(template.getDate());
        copy.setTimeSlot(template.getTimeSlot());
        copy.setUserId(template.getUserId());
        copy.setTimeoutMs(template.getTimeoutMs());
        copy.setForceRefresh(template.isForceRefresh());
        copy.setTraceId("launch-load-" + requestIndex + "-" + System.currentTimeMillis());
        return copy;
    }

    private boolean isDegradedRealtimeResponse(RealtimeQueryResponse response) {
        if (response == null) {
            return false;
        }
        if (response.getRealtimeStatus() == com.example.demo.demos.common.enums.RealtimeStatus.DEGRADED
                || response.getRealtimeStatus() == com.example.demo.demos.common.enums.RealtimeStatus.PARTIAL_SUCCESS) {
            return true;
        }
        return containsDegradedItem(response.getItems()) || hasFallbackSource(response.getItems());
    }

    private long elapsedMs(long startedAtNanos) {
        return Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
    }

    private double average(List<Long> values) {
        if (CollectionUtils.isEmpty(values)) {
            return 0D;
        }
        long total = 0L;
        for (Long value : values) {
            total += value == null ? 0L : value.longValue();
        }
        return round2((double) total / (double) values.size());
    }

    private long percentile(List<Long> values, double percentile) {
        if (CollectionUtils.isEmpty(values)) {
            return 0L;
        }
        List<Long> sorted = new ArrayList<Long>(values);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= sorted.size()) {
            index = sorted.size() - 1;
        }
        return sorted.get(index).longValue();
    }

    private long min(List<Long> values) {
        if (CollectionUtils.isEmpty(values)) {
            return 0L;
        }
        long result = Long.MAX_VALUE;
        for (Long value : values) {
            result = Math.min(result, value == null ? 0L : value.longValue());
        }
        return result == Long.MAX_VALUE ? 0L : result;
    }

    private long max(List<Long> values) {
        if (CollectionUtils.isEmpty(values)) {
            return 0L;
        }
        long result = Long.MIN_VALUE;
        for (Long value : values) {
            result = Math.max(result, value == null ? 0L : value.longValue());
        }
        return result == Long.MIN_VALUE ? 0L : result;
    }

    private double round2(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private Map<String, Object> readLaunchDoc(String classpathLocation, String title) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathLocation);
            String content = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            return mapOf(
                    "title", title,
                    "path", classpathLocation,
                    "content", content
            );
        } catch (Exception ex) {
            return mapOf(
                    "title", title,
                    "path", classpathLocation,
                    "content", "文档读取失败: " + ex.getMessage()
            );
        }
    }

    private boolean resolvePassed(Map<String, Object> body) {
        if (body.containsKey("passed")) {
            return boolVal(body.get("passed"), false);
        }
        String status = str(body.get("status"));
        String result = str(body.get("result"));
        return "passed".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status)
                || "passed".equalsIgnoreCase(result) || "success".equalsIgnoreCase(result);
    }

    private String resolveStatus(Map<String, Object> body) {
        String status = str(body.get("status"));
        if (StringUtils.hasText(status)) {
            return status;
        }
        return resolvePassed(body) ? "passed" : "failed";
    }

    private boolean isFreshPassedRecord(Map<String, Object> record, int freshnessHours) {
        if (record == null || record.isEmpty() || !boolVal(record.get("passed"), false)) {
            return false;
        }
        String updatedAt = defaultIfBlank(str(record.get("updatedAt")), str(record.get("createdAt")));
        if (!StringUtils.hasText(updatedAt)) {
            return false;
        }
        try {
            LocalDateTime timestamp = LocalDateTime.parse(updatedAt, FORMATTER);
            return !timestamp.isBefore(LocalDateTime.now().minusHours(freshnessHours));
        } catch (Exception ignore) {
            return false;
        }
    }

    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.<String, Object>emptyMap();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOrEmpty(Object value) {
        return value instanceof List ? (List<Map<String, Object>>) value : Collections.<Map<String, Object>>emptyList();
    }

    private List<Long> longList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<Long>();
        for (Object item : (List<?>) value) {
            if (item == null) {
                continue;
            }
            if (item instanceof Number) {
                result.add(((Number) item).longValue());
                continue;
            }
            String text = String.valueOf(item).trim();
            if (!StringUtils.hasText(text)) {
                continue;
            }
            try {
                result.add(Long.valueOf(text));
            } catch (NumberFormatException ignore) {
                // Ignore malformed manual drill IDs.
            }
        }
        return result;
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private Map<String, Object> safeBody(Map<String, Object> body) {
        return body == null ? Collections.<String, Object>emptyMap() : body;
    }

    private Map<String, Object> mutableRecord(String entryType, String id) {
        for (Map<String, Object> item : listOrEmpty(launchAdminStateStore.listRecords(entryType, 200))) {
            if (id.equals(str(item.get("id")))) {
                return new LinkedHashMap<String, Object>(item);
            }
        }
        throw new IllegalArgumentException("Record not found: " + id);
    }

    private boolean boolVal(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return fallback;
    }

    private int intVal(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignore) {
                return 0;
            }
        }
        return 0;
    }

    private long longVal(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignore) {
                return fallback;
            }
        }
        return fallback;
    }
}
