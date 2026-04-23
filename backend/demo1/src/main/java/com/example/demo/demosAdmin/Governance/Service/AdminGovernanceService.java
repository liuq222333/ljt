package com.example.demo.demosAdmin.Governance.Service;

import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Runtime.AgentRuntime;
import com.example.demo.demos.Agent.Runtime.SessionState;
import com.example.demo.demos.Agent.Service.QueryParserService;
import com.example.demo.demos.common.enums.AnswerType;
import com.example.demo.demos.common.enums.TaskType;
import com.example.demo.demos.governance.replay.GovernanceReplayCheckpointEntity;
import com.example.demo.demos.governance.replay.GovernanceReplayRecordEntity;
import com.example.demo.demos.governance.replay.GovernanceReplayStore;
import com.example.demo.demos.governance.replay.GovernanceReplayToolIoEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminGovernanceService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String STATE_STORE_KEY = GovernanceAdminStateStore.DEFAULT_STORE_KEY;
    private static final int DEFAULT_MAX_RUN_AGE_HOURS = 24;
    private static final String RELEASE_STATUS_DRAFT = "draft";
    private static final String RELEASE_STATUS_READY = "ready";
    private static final String RELEASE_STATUS_GRAY = "gray";
    private static final String RELEASE_STATUS_RELEASED = "released";
    private static final String RELEASE_STATUS_ROLLED_BACK = "rolled_back";

    private final GovernanceReplayStore replayStore;
    private final ObjectMapper objectMapper;
    private final GovernanceAdminStateStore governanceAdminStateStore;
    private final QueryParserService queryParserService;
    private final AgentRuntime agentRuntime;
    private final Map<String, Map<String, Object>> replayDetailByRequest = new LinkedHashMap<String, Map<String, Object>>();
    private final Map<String, String> replayTraceIndex = new LinkedHashMap<String, String>();
    private final Map<String, String> replaySessionIndex = new LinkedHashMap<String, String>();
    private final List<Map<String, Object>> evalCaseList = new ArrayList<Map<String, Object>>();
    private final List<Map<String, Object>> evalVersionList = new ArrayList<Map<String, Object>>();
    private final Map<Long, Map<String, Object>> versionDetailMap = new LinkedHashMap<Long, Map<String, Object>>();
    private final List<Map<String, Object>> regressionSetList = new ArrayList<Map<String, Object>>();
    private final Map<Long, Map<String, Object>> regressionDetailMap = new LinkedHashMap<Long, Map<String, Object>>();
    private final List<Map<String, Object>> evalRunList = new ArrayList<Map<String, Object>>();
    private final Map<Long, Map<String, Object>> evalRunDetailMap = new LinkedHashMap<Long, Map<String, Object>>();
    private final List<Map<String, Object>> releaseRecordList = new ArrayList<Map<String, Object>>();
    private final Map<Long, List<Map<String, Object>>> releaseEventMap = new LinkedHashMap<Long, List<Map<String, Object>>>();
    private final List<Map<String, Object>> grayConfigList = new ArrayList<Map<String, Object>>();

    public AdminGovernanceService(GovernanceReplayStore replayStore,
                                  ObjectMapper objectMapper,
                                  GovernanceAdminStateStore governanceAdminStateStore,
                                  QueryParserService queryParserService,
                                  AgentRuntime agentRuntime) {
        this.replayStore = replayStore;
        this.objectMapper = objectMapper;
        this.governanceAdminStateStore = governanceAdminStateStore;
        this.queryParserService = queryParserService;
        this.agentRuntime = agentRuntime;
    }

    @PostConstruct
    public void seed() {
        boolean restored = restorePersistedState();
        boolean shouldSeedReplay = replayDetailByRequest.isEmpty();
        if (shouldSeedReplay && replayStore != null) {
            List<GovernanceReplayRecordEntity> recentReplayRecords = replayStore.listRecent("", 1);
            shouldSeedReplay = recentReplayRecords == null || recentReplayRecords.isEmpty();
        }
        if (shouldSeedReplay) {
        seedReplay(
                m("requestId", "req-gov-1001", "traceId", "trace-gov-1001", "sessionId", "sess-gov-2001", "userId", "u-demo-01", "lastUserMessage", "find vegetables in community market today", "taskType", "product_search", "planType", "search_then_answer", "answerType", "search_summary", "degraded", false, "durationMs", 182L, "createdAt", ts(0, 1)),
                m("requestSnapshot", m("requestId", "req-gov-1001", "messages", Collections.singletonList(m("role", "user", "content", "find vegetables in community market today"))),
                        "stateSnapshot", m("route", "market-search", "bucket", "golden"),
                        "checkpoints", list(m("checkpointOrder", 1, "nodeName", "intent-router", "stateSnapshot", m("intent", "product_search"), "createdAt", ts(0, 1)), m("checkpointOrder", 2, "nodeName", "product-retrieval", "stateSnapshot", m("hits", 6), "createdAt", ts(0, 2))),
                        "toolIos", list(m("stepOrder", 1, "stepId", "search-products", "toolName", "backend_api", "purpose", "load products", "outputKey", "products", "optionalStep", false, "executionStatus", "success", "inputPayload", m("resource", "products"), "outputPayload", m("count", 6), "createdAt", ts(0, 2))))
        );
        seedReplay(
                m("requestId", "req-gov-1002", "traceId", "trace-gov-1002", "sessionId", "sess-gov-2001", "userId", "u-demo-01", "lastUserMessage", "summarize the failure reason from yesterday replay", "taskType", "governance_replay", "planType", "replay_diagnosis", "answerType", "diagnostic_summary", "errorCode", "ROUTER_TIMEOUT", "failedNode", "plan-router", "degraded", true, "durationMs", 1260L, "createdAt", ts(0, 3)),
                m("requestSnapshot", m("requestId", "req-gov-1002", "messages", Collections.singletonList(m("role", "user", "content", "summarize the failure reason from yesterday replay"))),
                        "stateSnapshot", m("route", "governance-replay", "fallback", "cached_summary"),
                        "checkpoints", list(m("checkpointOrder", 1, "nodeName", "intent-router", "stateSnapshot", m("intent", "governance_replay"), "createdAt", ts(0, 3)), m("checkpointOrder", 2, "nodeName", "plan-router", "stateSnapshot", m("timeout", true), "createdAt", ts(0, 4))),
                        "toolIos", list(m("stepOrder", 1, "stepId", "load-replay", "toolName", "backend_api", "purpose", "load replay", "outputKey", "replay", "optionalStep", false, "executionStatus", "success", "inputPayload", m("resource", "governance"), "outputPayload", m("requestId", "req-gov-0999"), "createdAt", ts(0, 4))))
        );
        seedReplay(
                m("requestId", "req-gov-1003", "traceId", "trace-gov-1003", "sessionId", "sess-gov-2002", "userId", "u-demo-02", "lastUserMessage", "if knowledge base misses, how should fallback answer behave", "taskType", "knowledge_qa", "planType", "knowledge_then_fallback", "answerType", "text_answer", "degraded", false, "durationMs", 245L, "createdAt", ts(1, 2)),
                m("requestSnapshot", m("requestId", "req-gov-1003", "messages", Collections.singletonList(m("role", "user", "content", "if knowledge base misses, how should fallback answer behave"))),
                        "stateSnapshot", m("route", "knowledge-qa", "bucket", "golden"),
                        "checkpoints", list(m("checkpointOrder", 1, "nodeName", "knowledge-route", "stateSnapshot", m("intent", "knowledge_qa"), "createdAt", ts(1, 2))),
                        "toolIos", list(m("stepOrder", 1, "stepId", "retrieve-knowledge", "toolName", "knowledge_search", "purpose", "retrieve docs", "outputKey", "documents", "optionalStep", false, "executionStatus", "success", "inputPayload", m("topK", 3), "outputPayload", m("hits", 3), "createdAt", ts(1, 2))))
        );
        }
        if (restored) {
            return;
        }
        if (!evalCaseList.isEmpty()) {
            return;
        }


        Map<String, Object> case1 = m("id", 1L, "caseName", "market-search-fresh-vegetables", "queryText", "find vegetables in community market today", "bucket", "golden", "riskLevel", "medium", "enabled", 1, "expectedTaskType", "product_search", "expectedPlanType", "search_then_answer", "expectedAnswerType", "search_summary", "tagsJson", "[\"market\"]", "notes", "seeded case", "sourceType", "seed", "createdAt", ts(0, 5), "updatedAt", ts(0, 5));
        Map<String, Object> case2 = m("id", 2L, "caseName", "replay-diagnosis-timeout", "queryText", "summarize the failure reason from yesterday replay", "bucket", "golden", "riskLevel", "high", "enabled", 1, "expectedTaskType", "governance_replay", "expectedPlanType", "replay_diagnosis", "expectedAnswerType", "diagnostic_summary", "tagsJson", "[\"replay\"]", "notes", "seeded case", "sourceType", "seed", "createdAt", ts(0, 5), "updatedAt", ts(0, 5));
        Map<String, Object> case3 = m("id", 3L, "caseName", "knowledge-fallback-policy", "queryText", "if knowledge base misses, how should fallback answer behave", "bucket", "golden", "riskLevel", "high", "enabled", 1, "expectedTaskType", "knowledge_qa", "expectedPlanType", "knowledge_then_fallback", "expectedAnswerType", "text_answer", "tagsJson", "[\"knowledge\"]", "notes", "seeded case", "sourceType", "seed", "createdAt", ts(1, 5), "updatedAt", ts(1, 5));
        Map<String, Object> case4 = m("id", 4L, "caseName", "activity-nearby-tonight", "queryText", "what nearby activities can I still join tonight", "bucket", "realtime", "riskLevel", "medium", "enabled", 1, "expectedTaskType", "activity_search", "expectedPlanType", "search_then_answer", "expectedAnswerType", "event_summary", "tagsJson", "[\"activity\"]", "notes", "seeded case", "sourceType", "seed", "createdAt", ts(2, 5), "updatedAt", ts(2, 5));
        Collections.addAll(evalCaseList, case1, case2, case3, case4);

        Map<String, Object> run101 = m("id", 101L, "bucket", "golden", "sourceType", "version_snapshot", "versionId", 11L, "total", 3, "passedTotal", 2, "failedTotal", 1, "passRate", 0.6667D, "createdAt", ts(0, 6));
        Map<String, Object> run102 = m("id", 102L, "bucket", "golden", "sourceType", "regression_set", "versionId", 11L, "regressionSetId", 21L, "total", 2, "passedTotal", 2, "failedTotal", 0, "passRate", 1D, "createdAt", ts(0, 7));
        evalRunList.add(run102);
        evalRunList.add(run101);
        evalRunDetailMap.put(101L, m("run", run101, "results", list(m("caseId", 1L, "caseName", case1.get("caseName"), "queryText", case1.get("queryText"), "expectedTaskType", "product_search", "actualTaskType", "product_search", "taskTypeMatched", true, "expectedPlanType", "search_then_answer", "actualPlanType", "search_then_answer", "planTypeMatched", true, "expectedAnswerType", "search_summary", "actualAnswerType", "search_summary", "answerTypeMatched", true, "degraded", false, "passed", true, "reply", "ok"), m("caseId", 2L, "caseName", case2.get("caseName"), "queryText", case2.get("queryText"), "expectedTaskType", "governance_replay", "actualTaskType", "governance_replay", "taskTypeMatched", true, "expectedPlanType", "replay_diagnosis", "actualPlanType", "fallback_route", "planTypeMatched", false, "expectedAnswerType", "diagnostic_summary", "actualAnswerType", "diagnostic_summary", "answerTypeMatched", true, "degraded", true, "passed", false, "reply", "fallback"))));
        evalRunDetailMap.put(102L, m("run", run102, "results", list(m("caseId", 2L, "caseName", case2.get("caseName"), "queryText", case2.get("queryText"), "expectedTaskType", "governance_replay", "actualTaskType", "governance_replay", "taskTypeMatched", true, "expectedPlanType", "replay_diagnosis", "actualPlanType", "replay_diagnosis", "planTypeMatched", true, "expectedAnswerType", "diagnostic_summary", "actualAnswerType", "diagnostic_summary", "answerTypeMatched", true, "degraded", false, "passed", true, "reply", "ok"), m("caseId", 3L, "caseName", case3.get("caseName"), "queryText", case3.get("queryText"), "expectedTaskType", "knowledge_qa", "actualTaskType", "knowledge_qa", "taskTypeMatched", true, "expectedPlanType", "knowledge_then_fallback", "actualPlanType", "knowledge_then_fallback", "planTypeMatched", true, "expectedAnswerType", "text_answer", "actualAnswerType", "text_answer", "answerTypeMatched", true, "degraded", false, "passed", true, "reply", "ok"))));

        Map<String, Object> version11 = m("id", 11L, "versionName", "w11-golden-baseline", "bucket", "golden", "totalCases", 3, "enabledTotal", 3, "notes", "seeded baseline snapshot", "createdBy", "system", "createdAt", ts(0, 8));
        Map<String, Object> version12 = m("id", 12L, "versionName", "w11-live-mixed", "bucket", "all", "totalCases", 4, "enabledTotal", 4, "notes", "mixed snapshot", "createdBy", "system", "createdAt", ts(0, 9));
        evalVersionList.add(version12);
        evalVersionList.add(version11);
        versionDetailMap.put(11L, m("version", version11, "items", list(snapshot(case1), snapshot(case2), snapshot(case3)), "latestRun", run101, "recentRuns", list(run101)));
        versionDetailMap.put(12L, m("version", version12, "items", list(snapshot(case1), snapshot(case2), snapshot(case3), snapshot(case4)), "latestRun", run102, "recentRuns", list(run102, run101)));

        Map<String, Object> regression21 = m("id", 21L, "setName", "w11-high-risk-regression", "bucket", "golden", "riskLevel", "high", "sourceVersionId", 11L, "totalCases", 2, "enabledTotal", 2, "notes", "high risk only", "createdBy", "system", "createdAt", ts(0, 10));
        regressionSetList.add(regression21);
        regressionDetailMap.put(21L, m("regressionSet", regression21, "items", list(snapshot(case2), snapshot(case3)), "latestRun", run102, "recentRuns", list(run102)));

        Map<String, Object> gray401 = m("id", 401L, "configName", "gray-10-percent-search", "queryBucket", "golden", "trafficPercent", 10, "riskLevel", "medium", "enabled", 1, "targetVersionJson", json(m("agent", "w11", "router", "replay-diagnostics", "search", "market-v2")), "notes", "seeded gray policy", "createdAt", ts(0, 11), "updatedAt", ts(0, 11));
        Map<String, Object> gray402 = m("id", 402L, "configName", "gray-25-percent-replay", "queryBucket", "live", "trafficPercent", 25, "riskLevel", "high", "enabled", 1, "targetVersionJson", json(m("agent", "w11", "router", "timeout-guard")), "notes", "replay recovery policy", "createdAt", ts(0, 12), "updatedAt", ts(0, 12));
        grayConfigList.add(gray402);
        grayConfigList.add(gray401);

        Map<String, Object> release301 = m("id", 301L, "releaseName", "w11-governance-console-smoke", "targetScope", "agent/search/governance", "releaseStatus", "ready", "evalCaseVersionId", 11L, "regressionSetId", 21L, "baselineEvalRunId", 101L, "latestEvalRunId", 102L, "grayStrategyJson", json(m("configId", 401L, "trafficPercent", 10, "queryBucket", "golden")), "versionSnapshotJson", json(m("versionId", 11L, "regressionSetId", 21L, "latestRunId", 102L)), "notes", "seeded release", "createdBy", "system", "createdAt", ts(0, 13), "updatedAt", ts(0, 13));
        Map<String, Object> release302 = m("id", 302L, "releaseName", "w11-governance-follow-up", "targetScope", "governance/replay", "releaseStatus", "draft", "evalCaseVersionId", 12L, "regressionSetId", 21L, "baselineEvalRunId", 101L, "grayStrategyJson", json(m("configId", 402L, "trafficPercent", 25, "queryBucket", "live")), "versionSnapshotJson", json(m("versionId", 12L, "regressionSetId", 21L)), "notes", "draft follow-up", "createdBy", "system", "createdAt", ts(0, 14), "updatedAt", ts(0, 14));
        releaseRecordList.add(release302);
        releaseRecordList.add(release301);
        releaseEventMap.put(301L, list(m("id", 901L, "releaseRecordId", 301L, "eventType", "release_created", "toStatus", "ready", "operatorName", "system", "eventDetailJson", json(m("baselineRunId", 101L, "latestRunId", 102L)), "createdAt", ts(0, 13)), m("id", 902L, "releaseRecordId", 301L, "eventType", "gray_config_applied", "fromStatus", "ready", "toStatus", "ready", "operatorName", "system", "eventDetailJson", json(m("grayConfigId", 401L)), "createdAt", ts(0, 12))));
        releaseEventMap.put(302L, list(m("id", 903L, "releaseRecordId", 302L, "eventType", "release_created", "toStatus", "draft", "operatorName", "system", "eventDetailJson", json(m("baselineRunId", 101L)), "createdAt", ts(0, 14))));
        persistGovernanceState();
    }

    public Map<String, Object> dashboard(int days, int releaseLimit, int versionLimit) {
        List<Map<String, Object>> daily = metricsDailyInternal(days);
        int replayTotal = sum(daily, "replayTotal");
        int degradedTotal = sum(daily, "degradedTotal");
        int errorTotal = sum(daily, "errorTotal");
        Map<String, Object> dashboard = new LinkedHashMap<String, Object>();
        dashboard.put("overview", m("days", days, "stage", "W13", "replay_total", replayTotal, "open_failure_total", errorTotal, "w12_ready", true));
        dashboard.put("metrics_summary", m("replay_total", replayTotal, "degraded_total", degradedTotal, "degraded_rate", replayTotal == 0 ? 0D : (double) degradedTotal / replayTotal, "error_total", errorTotal));
        dashboard.put("metrics_trend", daily.stream().map(item -> m("date", item.get("date"), "replay_total", item.get("replayTotal"), "degraded_total", item.get("degradedTotal"), "error_total", item.get("errorTotal"))).collect(Collectors.toList()));
        dashboard.put("error_attribution_summary", m("error_total", errorTotal, "degraded_total", degradedTotal, "error_rate", replayTotal == 0 ? 0D : (double) errorTotal / replayTotal, "degraded_rate", replayTotal == 0 ? 0D : (double) degradedTotal / replayTotal));
        dashboard.put("error_attribution_trend", errorTrend(days));
        dashboard.put("eval_case_stats", evalCaseStats());
        dashboard.put("recent_releases", page(filterRelease(""), 1, releaseLimit));
        dashboard.put("recent_gray_configs", page(filterGray(null), 1, 8));
        dashboard.put("recent_eval_versions", page(filterVersions(""), 1, versionLimit));
        dashboard.put("recent_regression_sets", page(filterRegression("", ""), 1, 8));
        dashboard.put("recent_eval_runs", page(filterRuns("", "", null, null), 1, 8));
        dashboard.put("recent_replays", replayList("", 8));
        return dashboard;
    }

    public List<Map<String, Object>> metricsDaily(int page, int size) {
        return page(metricsDailyInternal(Math.max(size, 7)), page, size);
    }

    public Map<String, Object> metricsDailyDetail(String date) {
        String targetDate = StringUtils.hasText(date) ? dateOnly(date) : LocalDate.now().toString();
        Map<String, Integer> byTaskType = new LinkedHashMap<String, Integer>();
        Map<String, Integer> byAnswerType = new LinkedHashMap<String, Integer>();
        Map<String, Integer> byPlanType = new LinkedHashMap<String, Integer>();
        Map<String, Integer> byFailedNode = new LinkedHashMap<String, Integer>();
        Map<String, Integer> byErrorCode = new LinkedHashMap<String, Integer>();
        Map<String, Integer> byFailureCombo = new LinkedHashMap<String, Integer>();
        List<Map<String, Object>> sampleRequests = new ArrayList<Map<String, Object>>();
        int replayTotal = 0;
        int degradedTotal = 0;
        int errorTotal = 0;
        long durationSum = 0L;
        for (Map<String, Object> item : combinedReplaySummaries()) {
            if (!targetDate.equals(dateOnly(str(item.get("createdAt"))))) {
                continue;
            }
            replayTotal++;
            if (boolVal(item.get("degraded"), false)) {
                degradedTotal++;
            }
            boolean failed = StringUtils.hasText(str(item.get("errorCode"))) || StringUtils.hasText(str(item.get("failedNode")));
            if (failed) {
                errorTotal++;
            }
            durationSum += longVal(item.get("durationMs"), 0L);
            addCount(byTaskType, defaultIfBlank(str(item.get("taskType")), "unknown"));
            addCount(byAnswerType, defaultIfBlank(str(item.get("answerType")), "unknown"));
            addCount(byPlanType, defaultIfBlank(str(item.get("planType")), "unknown"));
            if (StringUtils.hasText(str(item.get("failedNode")))) {
                addCount(byFailedNode, str(item.get("failedNode")));
            }
            if (StringUtils.hasText(str(item.get("errorCode")))) {
                addCount(byErrorCode, str(item.get("errorCode")));
            }
            if (failed) {
                addCount(byFailureCombo, failureCombo(item));
            }
            if (sampleRequests.size() < 10) {
                sampleRequests.add(m(
                        "requestId", item.get("requestId"),
                        "traceId", item.get("traceId"),
                        "sessionId", item.get("sessionId"),
                        "taskType", item.get("taskType"),
                        "planType", item.get("planType"),
                        "answerType", item.get("answerType"),
                        "failedNode", item.get("failedNode"),
                        "errorCode", item.get("errorCode"),
                        "degraded", item.get("degraded"),
                        "durationMs", item.get("durationMs"),
                        "lastUserMessage", item.get("lastUserMessage"),
                        "createdAt", item.get("createdAt")
                ));
            }
        }
        Map<String, Object> dailySummary = metricsDailyInternal(31).stream()
                .filter(item -> targetDate.equals(str(item.get("date"))))
                .findFirst()
                .orElse(m(
                        "date", targetDate,
                        "replayTotal", replayTotal,
                        "degradedTotal", degradedTotal,
                        "errorTotal", errorTotal,
                        "avgDurationMs", replayTotal == 0 ? 0L : durationSum / replayTotal,
                        "topFailedNode", topNode(byFailedNode)
                ));
        return m(
                "date", targetDate,
                "summary", dailySummary,
                "byTaskType", countBreakdown(byTaskType, 10),
                "byAnswerType", countBreakdown(byAnswerType, 10),
                "byPlanType", countBreakdown(byPlanType, 10),
                "byFailedNode", countBreakdown(byFailedNode, 10),
                "byErrorCode", countBreakdown(byErrorCode, 10),
                "topFailureCombos", countBreakdown(byFailureCombo, 10),
                "sampleRequests", sampleRequests
        );
    }

    public List<Map<String, Object>> errorTrend(int days) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : metricsDailyInternal(days)) {
            int replayTotal = intVal(item.get("replayTotal"), 0);
            int errorTotal = intVal(item.get("errorTotal"), 0);
            int degradedTotal = intVal(item.get("degradedTotal"), 0);
            result.add(m("date", item.get("date"), "errorTotal", errorTotal, "degradedTotal", degradedTotal, "errorRate", replayTotal == 0 ? 0D : (double) errorTotal / replayTotal, "degradedRate", replayTotal == 0 ? 0D : (double) degradedTotal / replayTotal, "topFailedNode", item.get("topFailedNode")));
        }
        return result;
    }

    public List<Map<String, Object>> replayList(String keyword, int limit) {
        List<Map<String, Object>> items = combinedReplaySummaries();
        if (StringUtils.hasText(keyword)) {
            items = items.stream().filter(item -> contains(item, keyword)).collect(Collectors.toList());
        }
        sort(items, "createdAt");
        return page(items, 1, limit <= 0 ? 20 : limit);
    }

    public List<Map<String, Object>> replayCandidates(int days, int limit, boolean problematicOnly, boolean excludeExistingQuery) {
        Set<String> existing = evalCaseList.stream().map(item -> key(item.get("queryText"))).collect(Collectors.toCollection(LinkedHashSet::new));
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> summary : combinedReplaySummaries()) {
            boolean degraded = boolVal(summary.get("degraded"), false);
            boolean failed = StringUtils.hasText(str(summary.get("errorCode"))) || StringUtils.hasText(str(summary.get("failedNode")));
            boolean exists = existing.contains(key(summary.get("lastUserMessage")));
            if (!withinDays(str(summary.get("createdAt")), days)) {
                continue;
            }
            if (problematicOnly && !degraded && !failed) {
                continue;
            }
            if (excludeExistingQuery && exists) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>(summary);
            item.put("existingQuery", exists);
            item.put("suggestedRiskLevel", failed ? "high" : degraded ? "medium" : "low");
            item.put("reason", failed ? defaultIfBlank(str(summary.get("failedNode")), str(summary.get("errorCode"))) : degraded ? "degraded_response" : "normal");
            items.add(item);
        }
        sort(items, "createdAt");
        return page(items, 1, limit <= 0 ? 20 : limit);
    }

    public Map<String, Object> replayByRequestId(String requestId) {
        Map<String, Object> actual = actualReplayDetailByRequest(requestId);
        if (actual != null) {
            return actual;
        }
        Map<String, Object> detail = replayDetailByRequest.get(requestId);
        if (detail == null) {
            throw new IllegalArgumentException("Replay request not found: " + requestId);
        }
        return withSessionHistory(detail);
    }

    public Map<String, Object> replayByTraceId(String traceId) {
        Map<String, Object> actual = actualReplayDetailByTrace(traceId);
        if (actual != null) {
            return actual;
        }
        String requestId = replayTraceIndex.get(traceId);
        if (!StringUtils.hasText(requestId)) {
            throw new IllegalArgumentException("Replay trace not found: " + traceId);
        }
        return replayByRequestId(requestId);
    }

    public Map<String, Object> replayBySessionId(String sessionId) {
        Map<String, Object> actual = actualReplayDetailBySession(sessionId);
        if (actual != null) {
            return actual;
        }
        String requestId = replaySessionIndex.get(sessionId);
        if (!StringUtils.hasText(requestId)) {
            throw new IllegalArgumentException("Replay session not found: " + sessionId);
        }
        return replayByRequestId(requestId);
    }

    public Map<String, Object> bootstrapEvalCasesFromReplayBatch(Map<String, Object> requestBody) {
        List<Long> createdIds = new ArrayList<Long>();
        int created = 0;
        int skipped = 0;
        List<?> requestIds = requestBody.get("requestIds") instanceof List ? (List<?>) requestBody.get("requestIds") : Collections.emptyList();
        Set<String> existingQueries = evalCaseList.stream()
                .map(item -> key(item.get("queryText")))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Object rawId : requestIds) {
            String requestId = str(rawId);
            if (!StringUtils.hasText(requestId)) {
                skipped++;
                continue;
            }
            Map<String, Object> detail = replayByRequestId(requestId);
            Map<String, Object> summary = asMap(detail.get("summary"));
            String queryText = defaultIfBlank(str(summary.get("lastUserMessage")), requestId);
            if (existingQueries.contains(key(queryText))) {
                skipped++;
                continue;
            }
            Map<String, Object> createdCase = createEvalCase(m(
                    "caseName", defaultIfBlank(str(summary.get("taskType")), "replay") + "-" + requestId,
                    "queryText", queryText,
                    "bucket", boolVal(summary.get("degraded"), false) ? "regression" : "golden",
                    "riskLevel", StringUtils.hasText(str(summary.get("errorCode"))) || boolVal(summary.get("degraded"), false) ? "high" : "medium",
                    "enabled", 1,
                    "expectedTaskType", str(summary.get("taskType")),
                    "expectedPlanType", str(summary.get("planType")),
                    "expectedAnswerType", str(summary.get("answerType")),
                    "tagsJson", json(Collections.singletonList("replay")),
                    "notes", "bootstrapped from replay " + requestId,
                    "sourceType", "replay_bootstrap"
            ));
            createdIds.add(longVal(createdCase.get("id"), 0L));
            existingQueries.add(key(queryText));
            created++;
        }
        return m("createdCount", created, "skippedCount", skipped, "requestCount", requestIds.size(), "createdIds", createdIds);
    }
    public Map<String, Object> evalCaseStats() {
        Map<String, Integer> byBucket = new LinkedHashMap<String, Integer>();
        Map<String, Integer> byRisk = new LinkedHashMap<String, Integer>();
        int enabled = 0;
        for (Map<String, Object> item : evalCaseList) {
            if (intVal(item.get("enabled"), 1) == 1) {
                enabled++;
            }
            addCount(byBucket, defaultIfBlank(str(item.get("bucket")), "default"));
            addCount(byRisk, defaultIfBlank(str(item.get("riskLevel")), "unknown"));
        }
        return m("total", evalCaseList.size(), "enabled_total", enabled, "disabled_total", evalCaseList.size() - enabled, "by_bucket", byBucket, "by_risk_level", byRisk);
    }

    public List<Map<String, Object>> evalCases(String keyword, String bucket, String riskLevel, Integer enabled, int page, int size) {
        List<Map<String, Object>> items = evalCaseList.stream().filter(item -> !StringUtils.hasText(keyword) || contains(item, keyword)).filter(item -> !StringUtils.hasText(bucket) || bucket.equals(str(item.get("bucket")))).filter(item -> !StringUtils.hasText(riskLevel) || riskLevel.equals(str(item.get("riskLevel")))).filter(item -> enabled == null || intVal(item.get("enabled"), 1) == enabled.intValue()).map(LinkedHashMap::new).collect(Collectors.toList());
        sort(items, "updatedAt");
        return page(items, page, size);
    }

    public List<Map<String, Object>> exportEvalCases(String keyword, String bucket, Integer enabled, int limit) {
        return evalCases(keyword, bucket, "", enabled, 1, limit <= 0 ? 200 : limit);
    }

    public Map<String, Object> createEvalCase(Map<String, Object> requestBody) {
        Map<String, Object> body = safeBody(requestBody);
        String queryText = str(body.get("queryText"));
        if (!StringUtils.hasText(queryText)) {
            throw new IllegalArgumentException("queryText is required");
        }
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("id", nextId(evalCaseList));
        item.put("caseName", defaultIfBlank(str(body.get("caseName")), "case-" + item.get("id")));
        item.put("queryText", queryText);
        item.put("bucket", defaultIfBlank(str(body.get("bucket")), "golden"));
        item.put("riskLevel", defaultIfBlank(str(body.get("riskLevel")), "medium"));
        item.put("enabled", intVal(body.get("enabled"), 1));
        item.put("expectedTaskType", str(body.get("expectedTaskType")));
        item.put("expectedPlanType", str(body.get("expectedPlanType")));
        item.put("expectedAnswerType", str(body.get("expectedAnswerType")));
        item.put("tagsJson", defaultIfBlank(str(body.get("tagsJson")), "[]"));
        item.put("notes", str(body.get("notes")));
        item.put("sourceType", defaultIfBlank(str(body.get("sourceType")), "manual"));
        item.put("createdAt", ts(0, 0));
        item.put("updatedAt", ts(0, 0));
        evalCaseList.add(0, item);
        refreshDerivedCollections();
        persistGovernanceState();
        return new LinkedHashMap<String, Object>(item);
    }

    public Map<String, Object> importEvalCases(Map<String, Object> requestBody) {
        List<?> cases = requestBody.get("cases") instanceof List ? (List<?>) requestBody.get("cases") : Collections.emptyList();
        int inserted = 0;
        int updated = 0;
        for (Object raw : cases) {
            if (!(raw instanceof Map)) {
                continue;
            }
            Map<String, Object> body = asMap(raw);
            Long id = longVal(body.get("id"), 0L);
            if (id > 0L && containsId(evalCaseList, id)) {
                updateEvalCase(body);
                updated++;
            } else {
                createEvalCase(body);
                inserted++;
            }
        }
        persistGovernanceState();
        return m("total", cases.size(), "inserted", inserted, "updated", updated);
    }

    public Map<String, Object> updateEvalCase(Map<String, Object> requestBody) {
        Map<String, Object> body = safeBody(requestBody);
        Long id = longVal(body.get("id"), 0L);
        if (id <= 0L) {
            throw new IllegalArgumentException("id is required");
        }
        Map<String, Object> item = mutableById(evalCaseList, id, "Eval case not found: " + id);
        mergeIfPresent(item, body, "caseName", "queryText", "bucket", "riskLevel", "expectedTaskType", "expectedPlanType", "expectedAnswerType", "tagsJson", "notes", "sourceType");
        if (body.containsKey("enabled")) {
            item.put("enabled", intVal(body.get("enabled"), intVal(item.get("enabled"), 1)));
        }
        item.put("updatedAt", ts(0, 0));
        refreshDerivedCollections();
        persistGovernanceState();
        return new LinkedHashMap<String, Object>(item);
    }

    public Map<String, Object> deleteEvalCase(Long id) {
        if (!removeById(evalCaseList, id)) {
            throw new IllegalArgumentException("Eval case not found: " + id);
        }
        removeCaseFromSnapshots(id);
        refreshDerivedCollections();
        persistGovernanceState();
        return m("deleted", true, "id", id);
    }

    public Map<String, Object> batchToggleEvalCases(Map<String, Object> requestBody) {
        List<Long> ids = longList(requestBody.get("ids"));
        int enabled = intVal(requestBody.get("enabled"), 1);
        int updatedCount = 0;
        for (Long id : ids) {
            Map<String, Object> item = findMutableOrNull(evalCaseList, id);
            if (item == null) {
                continue;
            }
            item.put("enabled", enabled);
            item.put("updatedAt", ts(0, 0));
            updatedCount++;
        }
        refreshDerivedCollections();
        persistGovernanceState();
        return m("updatedCount", updatedCount, "enabled", enabled);
    }

    public List<Map<String, Object>> evalVersions(String bucket, int page, int size) {
        return page(filterVersions(bucket), page, size);
    }

    public Map<String, Object> createEvalCaseVersion(Map<String, Object> requestBody) {
        Map<String, Object> body = safeBody(requestBody);
        String versionName = str(body.get("versionName"));
        if (!StringUtils.hasText(versionName)) {
            throw new IllegalArgumentException("versionName is required");
        }
        String keyword = str(body.get("keyword"));
        String bucket = str(body.get("bucket"));
        Integer enabled = body.containsKey("enabled") ? Integer.valueOf(intVal(body.get("enabled"), 1)) : null;
        int limit = intVal(body.get("limit"), 200);
        List<Map<String, Object>> sourceItems = evalCases(keyword, bucket, "", enabled, 1, limit <= 0 ? 200 : limit);
        Long versionId = nextId(evalVersionList);
        Map<String, Object> version = m(
                "id", versionId,
                "versionName", versionName,
                "bucket", defaultIfBlank(bucket, "all"),
                "totalCases", sourceItems.size(),
                "enabledTotal", countEnabled(sourceItems),
                "notes", str(body.get("notes")),
                "createdBy", defaultIfBlank(str(body.get("createdBy")), "front-admin"),
                "createdAt", ts(0, 0)
        );
        List<Map<String, Object>> items = sourceItems.stream().map(this::snapshotCase).collect(Collectors.toList());
        evalVersionList.add(0, version);
        versionDetailMap.put(versionId, m("version", version, "items", items, "latestRun", null, "recentRuns", Collections.emptyList()));
        refreshDerivedCollections();
        persistGovernanceState();
        return new LinkedHashMap<String, Object>(version);
    }

    public Map<String, Object> evalVersionDetail(Long id) {
        Map<String, Object> detail = versionDetailMap.get(id);
        if (detail == null) {
            throw new IllegalArgumentException("Eval case version not found: " + id);
        }
        return new LinkedHashMap<String, Object>(detail);
    }

    public Map<String, Object> compareEvalCaseVersions(Long baseId, Long targetId) {
        Map<String, Object> base = evalVersionDetail(baseId);
        Map<String, Object> target = evalVersionDetail(targetId);
        Map<String, Map<String, Object>> left = indexItems(listOfMaps(base.get("items")));
        Map<String, Map<String, Object>> right = indexItems(listOfMaps(target.get("items")));
        Set<String> keys = new LinkedHashSet<String>();
        keys.addAll(left.keySet());
        keys.addAll(right.keySet());
        int added = 0;
        int removed = 0;
        int changed = 0;
        int unchanged = 0;
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (String key : keys) {
            Map<String, Object> l = left.get(key);
            Map<String, Object> r = right.get(key);
            String change;
            if (l == null) {
                added++;
                change = "added";
            } else if (r == null) {
                removed++;
                change = "removed";
            } else if (snapshotChanged(l, r)) {
                changed++;
                change = "changed";
            } else {
                unchanged++;
                change = "unchanged";
            }
            Map<String, Object> source = r != null ? r : l;
            items.add(m(
                    "changeType", change,
                    "caseId", source.get("caseId"),
                    "caseName", source.get("caseName"),
                    "queryText", source.get("queryText"),
                    "baseExpectedTaskType", l == null ? null : l.get("expectedTaskType"),
                    "targetExpectedTaskType", r == null ? null : r.get("expectedTaskType"),
                    "baseExpectedPlanType", l == null ? null : l.get("expectedPlanType"),
                    "targetExpectedPlanType", r == null ? null : r.get("expectedPlanType"),
                    "baseExpectedAnswerType", l == null ? null : l.get("expectedAnswerType"),
                    "targetExpectedAnswerType", r == null ? null : r.get("expectedAnswerType"),
                    "baseRiskLevel", l == null ? null : l.get("riskLevel"),
                    "targetRiskLevel", r == null ? null : r.get("riskLevel")
            ));
        }
        return m("baseVersion", base.get("version"), "targetVersion", target.get("version"), "baseTotal", left.size(), "targetTotal", right.size(), "addedCount", added, "removedCount", removed, "changedCount", changed, "unchangedCount", unchanged, "items", items);
    }

    public Map<String, Object> runEvalCaseVersion(Long versionId, Integer limit) {
        Map<String, Object> detail = evalVersionDetail(versionId);
        List<Map<String, Object>> items = listOfMaps(detail.get("items"));
        if (items.isEmpty()) {
            throw new IllegalArgumentException("No eval cases found for version: " + versionId);
        }
        return createEvalRun("version_snapshot", versionId, null, items, limit);
    }

    public Map<String, Object> runEvalCaseVersionBatch(Map<String, Object> requestBody) {
        List<Long> ids = longList(requestBody.get("versionIds"));
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        int success = 0;
        int failed = 0;
        for (Long id : ids) {
            try {
                Map<String, Object> runResult = runEvalCaseVersion(id, intVal(requestBody.get("limit"), 20));
                Map<String, Object> run = asMap(runResult.get("run"));
                items.add(m("versionId", id, "runId", run.get("id"), "passRate", run.get("passRate"), "total", run.get("total"), "passedTotal", run.get("passedTotal"), "failedTotal", run.get("failedTotal")));
                success++;
            } catch (Exception ex) {
                items.add(m("versionId", id, "errorMessage", ex.getMessage()));
                failed++;
            }
        }
        return m("totalVersions", ids.size(), "successCount", success, "failedCount", failed, "items", items);
    }

    public List<Map<String, Object>> evalRuns(String bucket, String sourceType, Long versionId, Long regressionSetId, int page, int size) {
        return page(filterRuns(bucket, sourceType, versionId, regressionSetId), page, size);
    }

    public Map<String, Object> evalRunDetail(Long id) {
        return runResultById(id);
    }

    public Map<String, Object> compareEvalRuns(Long baseId, Long targetId) {
        Map<String, Object> base = runResultById(baseId);
        Map<String, Object> target = runResultById(targetId);
        Map<String, Map<String, Object>> left = indexItems(listOfMaps(base.get("results")));
        Map<String, Map<String, Object>> right = indexItems(listOfMaps(target.get("results")));
        Set<String> keys = new LinkedHashSet<String>();
        keys.addAll(left.keySet());
        keys.addAll(right.keySet());
        int improved = 0;
        int regressed = 0;
        int unchanged = 0;
        int added = 0;
        int removed = 0;
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (String key : keys) {
            Map<String, Object> l = left.get(key);
            Map<String, Object> r = right.get(key);
            String change = "unchanged";
            if (l == null) {
                added++;
                change = "new_case";
            } else if (r == null) {
                removed++;
                change = "removed_case";
            } else {
                boolean lp = boolVal(l.get("passed"), false);
                boolean rp = boolVal(r.get("passed"), false);
                if (!lp && rp) {
                    improved++;
                    change = "improved";
                } else if (lp && !rp) {
                    regressed++;
                    change = "regressed";
                } else if (runCaseChanged(l, r)) {
                    change = "changed";
                } else {
                    unchanged++;
                }
            }
            Map<String, Object> source = r != null ? r : l;
            items.add(m("caseId", source.get("caseId"), "caseName", source.get("caseName"), "queryText", source.get("queryText"), "changeType", change, "basePassed", l == null ? null : l.get("passed"), "targetPassed", r == null ? null : r.get("passed"), "baseActualTaskType", l == null ? null : l.get("actualTaskType"), "targetActualTaskType", r == null ? null : r.get("actualTaskType"), "baseActualPlanType", l == null ? null : l.get("actualPlanType"), "targetActualPlanType", r == null ? null : r.get("actualPlanType"), "baseActualAnswerType", l == null ? null : l.get("actualAnswerType"), "targetActualAnswerType", r == null ? null : r.get("actualAnswerType")));
        }
        Map<String, Object> baseRun = asMap(base.get("run"));
        Map<String, Object> targetRun = asMap(target.get("run"));
        return m("baseRunId", baseId, "targetRunId", targetId, "baseTotal", baseRun.get("total"), "targetTotal", targetRun.get("total"), "basePassRate", baseRun.get("passRate"), "targetPassRate", targetRun.get("passRate"), "passRateDelta", doubleVal(targetRun.get("passRate"), 0D) - doubleVal(baseRun.get("passRate"), 0D), "improvedCount", improved, "regressedCount", regressed, "unchangedCount", unchanged, "newCaseCount", added, "removedCaseCount", removed, "items", items);
    }

    public List<Map<String, Object>> regressionSets(String bucket, String riskLevel, int page, int size) {
        return page(filterRegression(bucket, riskLevel), page, size);
    }

    public Map<String, Object> regressionSetDetail(Long id) {
        Map<String, Object> detail = regressionDetailMap.get(id);
        if (detail == null) {
            throw new IllegalArgumentException("Regression set not found: " + id);
        }
        return new LinkedHashMap<String, Object>(detail);
    }

    public Map<String, Object> createRegressionSet(Map<String, Object> requestBody) {
        Map<String, Object> body = safeBody(requestBody);
        String setName = str(body.get("setName"));
        if (!StringUtils.hasText(setName)) {
            throw new IllegalArgumentException("setName is required");
        }
        Long sourceVersionId = longVal(body.get("sourceVersionId"), 0L);
        String bucket = str(body.get("bucket"));
        String riskLevel = str(body.get("riskLevel"));
        int limit = intVal(body.get("limit"), 100);
        List<Map<String, Object>> sourceItems;
        if (sourceVersionId > 0L) {
            sourceItems = listOfMaps(evalVersionDetail(sourceVersionId).get("items"));
        } else {
            sourceItems = evalCases("", bucket, riskLevel, 1, 1, limit <= 0 ? 100 : limit)
                    .stream()
                    .map(this::snapshotCase)
                    .collect(Collectors.toList());
        }
        if (StringUtils.hasText(riskLevel)) {
            sourceItems = sourceItems.stream()
                    .filter(item -> riskLevel.equals(str(item.get("riskLevel"))))
                    .collect(Collectors.toList());
        }
        if (limit > 0 && sourceItems.size() > limit) {
            sourceItems = new ArrayList<Map<String, Object>>(sourceItems.subList(0, limit));
        }
        Long id = nextId(regressionSetList);
        Map<String, Object> set = m(
                "id", id,
                "setName", setName,
                "bucket", defaultIfBlank(bucket, "all"),
                "riskLevel", defaultIfBlank(riskLevel, "mixed"),
                "sourceVersionId", sourceVersionId == 0L ? null : sourceVersionId,
                "totalCases", sourceItems.size(),
                "enabledTotal", sourceItems.size(),
                "notes", str(body.get("notes")),
                "createdBy", defaultIfBlank(str(body.get("createdBy")), "front-admin"),
                "createdAt", ts(0, 0)
        );
        regressionSetList.add(0, set);
        regressionDetailMap.put(id, m("regressionSet", set, "items", copyList(sourceItems), "latestRun", null, "recentRuns", Collections.emptyList()));
        refreshDerivedCollections();
        persistGovernanceState();
        return new LinkedHashMap<String, Object>(set);
    }

    public Map<String, Object> runRegressionSet(Long id, Integer limit) {
        Map<String, Object> detail = regressionSetDetail(id);
        List<Map<String, Object>> items = listOfMaps(detail.get("items"));
        if (items.isEmpty()) {
            throw new IllegalArgumentException("No eval cases found for regression set: " + id);
        }
        return createEvalRun("regression_set", longVal(asMap(detail.get("regressionSet")).get("sourceVersionId"), 0L), id, items, limit);
    }

    public List<Map<String, Object>> releaseRecords(String status, int page, int size) {
        return page(filterRelease(status), page, size);
    }

    public Map<String, Object> releaseRecord(Long id) {
        return findById(releaseRecordList, id, "Release record not found: " + id);
    }

    public Map<String, Object> releaseGovernanceSummary(Long id, double minEvalPassRate, Integer maxRunAgeHours) {
        Map<String, Object> release = releaseRecord(id);
        Long versionId = longVal(release.get("evalCaseVersionId"), 0L);
        Long regressionSetId = longVal(release.get("regressionSetId"), 0L);
        Long baselineRunId = longVal(release.get("baselineEvalRunId"), 0L);
        Long latestRunId = longVal(release.get("latestEvalRunId"), 0L);
        Map<String, Object> verification = releaseVerification(id, minEvalPassRate, maxRunAgeHours);
        Map<String, Object> preflight = releasePreflight(10, 10, 0.2D,
                versionId == 0L ? null : versionId,
                regressionSetId == 0L ? null : regressionSetId,
                minEvalPassRate,
                maxRunAgeHours);
        Map<String, Object> grayStrategy = parseJsonObject(str(release.get("grayStrategyJson")));
        Long grayConfigId = longVal(grayStrategy.get("configId"), 0L);
        List<String> recommendedActions = new ArrayList<String>();
        recommendedActions.addAll(asListOfStrings(verification.get("recommended_actions")));
        for (String action : asListOfStrings(preflight.get("recommended_actions"))) {
            if (!recommendedActions.contains(action)) {
                recommendedActions.add(action);
            }
        }
        return m(
                "release", release,
                "verification", verification,
                "preflight", preflight,
                "grayStrategy", grayStrategy,
                "grayConfig", grayConfigId > 0L ? grayConfig(grayConfigId) : null,
                "evalCaseVersion", versionId > 0L ? evalVersionDetail(versionId) : null,
                "regressionSet", regressionSetId > 0L ? regressionSetDetail(regressionSetId) : null,
                "latestRun", latestRunId > 0L ? runResultById(latestRunId) : null,
                "baselineRun", baselineRunId > 0L ? runResultById(baselineRunId) : null,
                "events", releaseEvents(id),
                "recommended_actions", recommendedActions
        );
    }

    public Map<String, Object> createReleaseRecord(Map<String, Object> requestBody) {
        Map<String, Object> body = safeBody(requestBody);
        String releaseName = str(body.get("releaseName"));
        if (!StringUtils.hasText(releaseName)) {
            throw new IllegalArgumentException("releaseName is required");
        }
        String requestedStatus = defaultIfBlank(str(body.get("releaseStatus")), RELEASE_STATUS_DRAFT);
        if (!RELEASE_STATUS_DRAFT.equals(requestedStatus)) {
            throw new IllegalArgumentException("releaseStatus must start from draft");
        }
        Long id = nextId(releaseRecordList);
        Long versionId = longVal(body.get("evalCaseVersionId"), 0L);
        Long regressionSetId = longVal(body.get("regressionSetId"), 0L);
        Map<String, Object> record = m(
                "id", id,
                "releaseName", releaseName,
                "targetScope", defaultIfBlank(str(body.get("targetScope")), "agent/governance"),
                "releaseStatus", RELEASE_STATUS_DRAFT,
                "evalCaseVersionId", versionId == 0L ? null : versionId,
                "regressionSetId", regressionSetId == 0L ? null : regressionSetId,
                "baselineEvalRunId", null,
                "latestEvalRunId", null,
                "grayStrategyJson", "",
                "versionSnapshotJson", versionId == 0L ? "" : json(m("versionId", versionId, "regressionSetId", regressionSetId == 0L ? null : regressionSetId)),
                "notes", str(body.get("notes")),
                "createdBy", defaultIfBlank(str(body.get("createdBy")), "front-admin"),
                "createdAt", ts(0, 0),
                "updatedAt", ts(0, 0)
        );
        releaseRecordList.add(0, record);
        appendReleaseEvent(id, "release_created", "", str(record.get("releaseStatus")), m("releaseName", releaseName));
        persistGovernanceState();
        return new LinkedHashMap<String, Object>(record);
    }

    public Map<String, Object> updateReleaseRecord(Map<String, Object> requestBody) {
        Map<String, Object> body = safeBody(requestBody);
        Long id = longVal(body.get("id"), 0L);
        if (id <= 0L) {
            throw new IllegalArgumentException("id is required");
        }
        Map<String, Object> record = mutableById(releaseRecordList, id, "Release record not found: " + id);
        if (body.containsKey("releaseStatus") && !sameValue(body.get("releaseStatus"), record.get("releaseStatus"))) {
            throw new IllegalArgumentException("Use transition API to change releaseStatus");
        }
        mergeIfPresent(record, body, "releaseName", "targetScope", "notes");
        if (body.containsKey("evalCaseVersionId")) {
            Long versionId = longVal(body.get("evalCaseVersionId"), 0L);
            record.put("evalCaseVersionId", versionId == 0L ? null : versionId);
        }
        if (body.containsKey("regressionSetId")) {
            Long regressionSetId = longVal(body.get("regressionSetId"), 0L);
            record.put("regressionSetId", regressionSetId == 0L ? null : regressionSetId);
        }
        record.put("versionSnapshotJson", longVal(record.get("evalCaseVersionId"), 0L) == 0L ? "" : json(m("versionId", record.get("evalCaseVersionId"), "regressionSetId", record.get("regressionSetId"), "latestRunId", record.get("latestEvalRunId"))));
        record.put("updatedAt", ts(0, 0));
        persistGovernanceState();
        return new LinkedHashMap<String, Object>(record);
    }

    public Map<String, Object> releaseVerification(Long id, double minEvalPassRate, Integer maxRunAgeHours) {
        Map<String, Object> release = releaseRecord(id);
        Map<String, Object> latestRun = resolveLatestReleaseRun(release);
        int safeMaxRunAgeHours = sanitizeMaxRunAgeHours(maxRunAgeHours);
        double passRate = latestRun == null ? 0D : doubleVal(latestRun.get("passRate"), 0D);
        boolean runFresh = isRunFresh(latestRun, safeMaxRunAgeHours);
        boolean ready = latestRun != null && runFresh && passRate >= minEvalPassRate;
        List<String> actions = new ArrayList<String>();
        if (latestRun == null) {
            actions.add("Run release eval first.");
        }
        if (latestRun != null && !runFresh) {
            actions.add("Latest run is stale. Re-run release eval.");
        }
        if (passRate < minEvalPassRate) {
            actions.add("Raise pass rate before gray/released.");
        }
        if (!StringUtils.hasText(str(release.get("grayStrategyJson")))) {
            actions.add("Apply gray config before gray transition.");
        }
        if (actions.isEmpty()) {
            actions.add("Verification ready.");
        }
        return m(
                "ready", ready,
                "checks", m(
                        "has_latest_run", latestRun != null,
                        "latest_run_fresh", runFresh,
                        "latest_pass_rate_ok", passRate >= minEvalPassRate,
                        "gray_strategy_bound", StringUtils.hasText(str(release.get("grayStrategyJson")))
                ),
                "recommended_actions", actions,
                "overview", m(
                        "releaseId", id,
                        "releaseStatus", release.get("releaseStatus"),
                        "latestRunId", latestRun == null ? null : latestRun.get("id"),
                        "latestRunCreatedAt", latestRun == null ? null : latestRun.get("createdAt"),
                        "latestPassRate", passRate,
                        "minEvalPassRate", minEvalPassRate,
                        "maxRunAgeHours", safeMaxRunAgeHours
                )
        );
    }

    public Map<String, Object> releasePreflight(int detailLimit, int taskLimit, double maxDegradedRate, Long evalCaseVersionId, Long regressionSetId, double minEvalPassRate, Integer maxRunAgeHours) {
        Map<String, Object> scope = resolveDefaultPreflightScope(evalCaseVersionId, regressionSetId);
        Long resolvedVersionId = positiveLong(scope.get("evalCaseVersionId"));
        Long resolvedRegressionSetId = positiveLong(scope.get("regressionSetId"));
        Map<String, Object> run = latestRelevantRun(resolvedVersionId, resolvedRegressionSetId);
        int safeMaxRunAgeHours = sanitizeMaxRunAgeHours(maxRunAgeHours);
        double passRate = run == null ? 0D : doubleVal(run.get("passRate"), 0D);
        Map<String, Object> runDetail = run == null ? null : runResultById(longVal(run.get("id"), 0L));
        List<Map<String, Object>> failedCases = runDetail == null
                ? Collections.<Map<String, Object>>emptyList()
                : listOfMaps(runDetail.get("results")).stream()
                .filter(item -> !boolVal(item.get("passed"), false))
                .limit(Math.max(detailLimit, 0))
                .collect(Collectors.toList());
        double degradedRate = runDetail == null
                ? 0D
                : calculateDegradedRate(listOfMaps(runDetail.get("results")));
        boolean runFresh = isRunFresh(run, safeMaxRunAgeHours);
        List<String> actions = new ArrayList<String>();
        if (run == null) {
            actions.add("Run version or regression snapshot.");
        }
        if (run != null && !runFresh) {
            actions.add("Latest regression run is stale.");
        }
        if (passRate < minEvalPassRate) {
            actions.add("Pass rate below threshold.");
        }
        if (degradedRate > maxDegradedRate) {
            actions.add("Degraded rate above threshold.");
        }
        if (actions.isEmpty()) {
            actions.add("Preflight looks good.");
        }
        return m(
                "ready", run != null && runFresh && passRate >= minEvalPassRate && degradedRate <= maxDegradedRate,
                "checks", m(
                        "has_recent_run", run != null,
                        "run_fresh_ok", runFresh,
                        "pass_rate_ok", passRate >= minEvalPassRate,
                        "degraded_rate_ok", degradedRate <= maxDegradedRate
                ),
                "recommended_actions", actions,
                "overview", m(
                        "evalCaseVersionId", resolvedVersionId,
                        "regressionSetId", resolvedRegressionSetId,
                        "scopeSource", scope.get("source"),
                        "scopeReleaseId", scope.get("releaseId"),
                        "runId", run == null ? null : run.get("id"),
                        "runCreatedAt", run == null ? null : run.get("createdAt"),
                        "passRate", passRate,
                        "degradedRate", degradedRate,
                        "maxDegradedRate", maxDegradedRate,
                        "maxRunAgeHours", safeMaxRunAgeHours
                ),
                "failed_cases", failedCases,
                "tasks", page(list(m("label", "check-replays", "detail", "Review recent degraded replays"), m("label", "run-regression", "detail", "Run bound regression set"), m("label", "bind-gray", "detail", "Apply gray config if needed")), 1, taskLimit)
        );
    }

    public List<Map<String, Object>> releaseEvents(Long id) {
        return releaseEventMap.containsKey(id) ? copyList(releaseEventMap.get(id)) : Collections.emptyList();
    }

    public Map<String, Object> runReleaseEval(Long id, int limit, boolean setAsBaseline) {
        Map<String, Object> release = releaseRecord(id);
        Long regressionId = longVal(release.get("regressionSetId"), 0L);
        Map<String, Object> result;
        if (regressionId > 0L) {
            result = runRegressionSet(regressionId, limit);
        } else {
            result = runEvalCaseVersion(longVal(release.get("evalCaseVersionId"), 0L), limit);
        }
        Map<String, Object> run = asMap(result.get("run"));
        Map<String, Object> mutable = mutableById(releaseRecordList, id, "Release record not found: " + id);
        mutable.put("latestEvalRunId", run.get("id"));
        if (setAsBaseline) {
            mutable.put("baselineEvalRunId", run.get("id"));
        }
        mutable.put("versionSnapshotJson", longVal(mutable.get("evalCaseVersionId"), 0L) == 0L ? "" : json(m("versionId", mutable.get("evalCaseVersionId"), "regressionSetId", mutable.get("regressionSetId"), "latestRunId", mutable.get("latestEvalRunId"))));
        mutable.put("updatedAt", ts(0, 0));
        appendReleaseEvent(id, "release_eval_run", str(mutable.get("releaseStatus")), str(mutable.get("releaseStatus")), m("runId", run.get("id"), "setAsBaseline", setAsBaseline));
        persistGovernanceState();
        return result;
    }

    public List<Map<String, Object>> grayConfigs(int page, int size, Integer enabled) {
        return page(filterGray(enabled), page, size);
    }

    public Map<String, Object> grayConfig(Long id) {
        return findById(grayConfigList, id, "Gray config not found: " + id);
    }

    public Map<String, Object> createGrayConfig(Map<String, Object> requestBody) {
        Map<String, Object> body = safeBody(requestBody);
        String configName = str(body.get("configName"));
        if (!StringUtils.hasText(configName)) {
            throw new IllegalArgumentException("configName is required");
        }
        Map<String, Object> config = m(
                "id", nextId(grayConfigList),
                "configName", configName,
                "queryBucket", defaultIfBlank(str(body.get("queryBucket")), "all"),
                "trafficPercent", intVal(body.get("trafficPercent"), 10),
                "riskLevel", defaultIfBlank(str(body.get("riskLevel")), "medium"),
                "enabled", intVal(body.get("enabled"), 1),
                "targetVersionJson", defaultIfBlank(str(body.get("targetVersionJson")), "{}"),
                "notes", str(body.get("notes")),
                "createdAt", ts(0, 0),
                "updatedAt", ts(0, 0)
        );
        grayConfigList.add(0, config);
        persistGovernanceState();
        return new LinkedHashMap<String, Object>(config);
    }

    public Map<String, Object> updateGrayConfig(Map<String, Object> requestBody) {
        Map<String, Object> body = safeBody(requestBody);
        Long id = longVal(body.get("id"), 0L);
        if (id <= 0L) {
            throw new IllegalArgumentException("id is required");
        }
        Map<String, Object> config = mutableById(grayConfigList, id, "Gray config not found: " + id);
        mergeIfPresent(config, body, "configName", "queryBucket", "riskLevel", "targetVersionJson", "notes");
        if (body.containsKey("trafficPercent")) {
            config.put("trafficPercent", intVal(body.get("trafficPercent"), intVal(config.get("trafficPercent"), 10)));
        }
        if (body.containsKey("enabled")) {
            config.put("enabled", intVal(body.get("enabled"), intVal(config.get("enabled"), 1)));
        }
        config.put("updatedAt", ts(0, 0));
        persistGovernanceState();
        return new LinkedHashMap<String, Object>(config);
    }

    public Map<String, Object> deleteGrayConfig(Long id) {
        if (!removeById(grayConfigList, id)) {
            throw new IllegalArgumentException("Gray config not found: " + id);
        }
        clearGrayFromReleases(id);
        persistGovernanceState();
        return m("deleted", true, "id", id);
    }

    public Map<String, Object> applyGrayConfigToRelease(Long releaseId, Long configId) {
        Map<String, Object> release = mutableById(releaseRecordList, releaseId, "Release record not found: " + releaseId);
        String currentStatus = defaultIfBlank(str(release.get("releaseStatus")), RELEASE_STATUS_DRAFT);
        if (RELEASE_STATUS_RELEASED.equals(currentStatus) || RELEASE_STATUS_ROLLED_BACK.equals(currentStatus)) {
            throw new IllegalArgumentException("Gray config can only be applied before released or rolled_back");
        }
        Map<String, Object> gray = grayConfig(configId);
        release.put("grayStrategyJson", json(m("configId", configId, "trafficPercent", gray.get("trafficPercent"), "queryBucket", gray.get("queryBucket"), "riskLevel", gray.get("riskLevel"))));
        release.put("updatedAt", ts(0, 0));
        appendReleaseEvent(releaseId, "gray_config_applied", str(release.get("releaseStatus")), str(release.get("releaseStatus")), m("grayConfigId", configId));
        persistGovernanceState();
        return new LinkedHashMap<String, Object>(release);
    }

    public Map<String, Object> transitionRelease(Long releaseId, String targetStatus, Long grayConfigId, double minEvalPassRate, Integer maxRunAgeHours) {
        Map<String, Object> release = mutableById(releaseRecordList, releaseId, "Release record not found: " + releaseId);
        String currentStatus = defaultIfBlank(str(release.get("releaseStatus")), RELEASE_STATUS_DRAFT);
        String normalizedTargetStatus = normalizeReleaseStatus(targetStatus);
        if (currentStatus.equals(normalizedTargetStatus)) {
            return new LinkedHashMap<String, Object>(release);
        }
        assertReleaseTransitionAllowed(currentStatus, normalizedTargetStatus);
        if (RELEASE_STATUS_GRAY.equals(normalizedTargetStatus) && grayConfigId != null && grayConfigId.longValue() > 0L) {
            applyGrayConfigToRelease(releaseId, grayConfigId);
        }
        if ((RELEASE_STATUS_READY.equals(normalizedTargetStatus) || RELEASE_STATUS_GRAY.equals(normalizedTargetStatus) || RELEASE_STATUS_RELEASED.equals(normalizedTargetStatus))
                && !boolVal(releaseVerification(releaseId, minEvalPassRate, maxRunAgeHours).get("ready"), false)) {
            throw new IllegalArgumentException("Release verification is not ready for transition to " + normalizedTargetStatus);
        }
        if ((RELEASE_STATUS_GRAY.equals(normalizedTargetStatus) || RELEASE_STATUS_RELEASED.equals(normalizedTargetStatus))
                && !StringUtils.hasText(str(release.get("grayStrategyJson")))) {
            throw new IllegalArgumentException("Gray config must be applied before gray transition");
        }
        release.put("releaseStatus", normalizedTargetStatus);
        release.put("updatedAt", ts(0, 0));
        appendReleaseEvent(
                releaseId,
                "status_transition",
                currentStatus,
                normalizedTargetStatus,
                m("grayConfigId", grayConfigId, "minEvalPassRate", minEvalPassRate, "maxRunAgeHours", sanitizeMaxRunAgeHours(maxRunAgeHours))
        );
        persistGovernanceState();
        return new LinkedHashMap<String, Object>(release);
    }

    private Map<String, Object> createEvalRun(String sourceType, Long versionId, Long regressionSetId, List<Map<String, Object>> snapshotItems, Integer limit) {
        int safeLimit = limit == null || limit.intValue() <= 0 ? snapshotItems.size() : Math.min(snapshotItems.size(), limit.intValue());
        List<Map<String, Object>> selected = new ArrayList<Map<String, Object>>(snapshotItems.subList(0, safeLimit));
        Long runId = nextId(evalRunList);
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : selected) {
            results.add(executeEvalCase(item));
        }
        int passedTotal = 0;
        int failedTotal = 0;
        for (Map<String, Object> item : results) {
            if (boolVal(item.get("passed"), false)) {
                passedTotal++;
            } else {
                failedTotal++;
            }
        }
        Map<String, Object> run = m(
                "id", runId,
                "bucket", inferRunBucket(selected),
                "sourceType", sourceType,
                "versionId", versionId == null || versionId.longValue() == 0L ? null : versionId,
                "regressionSetId", regressionSetId == null || regressionSetId.longValue() == 0L ? null : regressionSetId,
                "total", selected.size(),
                "passedTotal", passedTotal,
                "failedTotal", failedTotal,
                "passRate", selected.isEmpty() ? 0D : (double) passedTotal / selected.size(),
                "createdAt", ts(0, 0)
        );
        evalRunList.add(0, run);
        evalRunDetailMap.put(runId, m("run", run, "results", results));
        refreshDerivedCollections();
        persistGovernanceState();
        return runResultById(runId);
    }

    private void refreshDerivedCollections() {
        Map<Long, Map<String, Object>> evalCasesById = evalCaseList.stream()
                .collect(Collectors.toMap(item -> longVal(item.get("id"), 0L), item -> item, (left, right) -> left, LinkedHashMap::new));
        for (Map<String, Object> detail : versionDetailMap.values()) {
            List<Map<String, Object>> items = refreshSnapshotItems(listOfMaps(detail.get("items")), evalCasesById);
            detail.put("items", items);
            Map<String, Object> version = asMap(detail.get("version"));
            version.put("totalCases", items.size());
            version.put("enabledTotal", countEnabledBySnapshot(items, evalCasesById));
            List<Map<String, Object>> recentRuns = filterRuns("", "version_snapshot", longVal(version.get("id"), 0L), null);
            detail.put("latestRun", recentRuns.isEmpty() ? null : recentRuns.get(0));
            detail.put("recentRuns", page(recentRuns, 1, 5));
        }
        for (Map<String, Object> detail : regressionDetailMap.values()) {
            List<Map<String, Object>> items = refreshSnapshotItems(listOfMaps(detail.get("items")), evalCasesById);
            detail.put("items", items);
            Map<String, Object> set = asMap(detail.get("regressionSet"));
            set.put("totalCases", items.size());
            set.put("enabledTotal", countEnabledBySnapshot(items, evalCasesById));
            List<Map<String, Object>> recentRuns = filterRuns("", "regression_set", null, longVal(set.get("id"), 0L));
            detail.put("latestRun", recentRuns.isEmpty() ? null : recentRuns.get(0));
            detail.put("recentRuns", page(recentRuns, 1, 5));
        }
        sort(evalVersionList, "createdAt");
        sort(regressionSetList, "createdAt");
        sort(evalRunList, "createdAt");
        sort(releaseRecordList, "updatedAt");
        sort(grayConfigList, "updatedAt");
    }

    private boolean restorePersistedState() {
        if (governanceAdminStateStore == null) {
            return false;
        }
        Map<String, Map<String, Object>> structured = governanceAdminStateStore.loadStructured(STATE_STORE_KEY);
        if (structured != null && !structured.isEmpty()) {
            replaceList(evalCaseList, restoreStructuredList(structured.get("eval_cases")));
            replaceList(evalVersionList, restoreStructuredList(structured.get("eval_versions")));
            replaceList(regressionSetList, restoreStructuredList(structured.get("regression_sets")));
            replaceList(evalRunList, restoreStructuredList(structured.get("eval_runs")));
            replaceList(releaseRecordList, restoreStructuredList(structured.get("release_records")));
            replaceList(grayConfigList, restoreStructuredList(structured.get("gray_configs")));
            replaceKeyedMap(versionDetailMap, restoreStructuredKeyedMap(structured.get("version_details")));
            replaceKeyedMap(regressionDetailMap, restoreStructuredKeyedMap(structured.get("regression_details")));
            replaceKeyedMap(evalRunDetailMap, restoreStructuredKeyedMap(structured.get("eval_run_details")));
            replaceListMap(releaseEventMap, restoreStructuredListMap(structured.get("release_events")));
            refreshDerivedCollections();
            return true;
        }
        Map<String, Object> persisted = governanceAdminStateStore.load(STATE_STORE_KEY);
        if (persisted == null || persisted.isEmpty()) {
            return false;
        }
        replaceList(evalCaseList, listOfMaps(persisted.get("evalCaseList")));
        replaceList(evalVersionList, listOfMaps(persisted.get("evalVersionList")));
        replaceList(regressionSetList, listOfMaps(persisted.get("regressionSetList")));
        replaceList(evalRunList, listOfMaps(persisted.get("evalRunList")));
        replaceList(releaseRecordList, listOfMaps(persisted.get("releaseRecordList")));
        replaceList(grayConfigList, listOfMaps(persisted.get("grayConfigList")));
        replaceKeyedMap(versionDetailMap, mapOfMaps(persisted.get("versionDetailMap")));
        replaceKeyedMap(regressionDetailMap, mapOfMaps(persisted.get("regressionDetailMap")));
        replaceKeyedMap(evalRunDetailMap, mapOfMaps(persisted.get("evalRunDetailMap")));
        replaceListMap(releaseEventMap, mapOfListOfMaps(persisted.get("releaseEventMap")));
        refreshDerivedCollections();
        return true;
    }

    private void persistGovernanceState() {
        if (governanceAdminStateStore == null) {
            return;
        }
        governanceAdminStateStore.saveStructured(STATE_STORE_KEY, snapshotGovernanceStructuredState());
        governanceAdminStateStore.save(STATE_STORE_KEY, snapshotGovernanceState());
    }

    private Map<String, Object> snapshotGovernanceState() {
        return m(
                "evalCaseList", deepCopy(listOfMaps(evalCaseList)),
                "evalVersionList", deepCopy(listOfMaps(evalVersionList)),
                "versionDetailMap", deepCopy(versionDetailMap),
                "regressionSetList", deepCopy(listOfMaps(regressionSetList)),
                "regressionDetailMap", deepCopy(regressionDetailMap),
                "evalRunList", deepCopy(listOfMaps(evalRunList)),
                "evalRunDetailMap", deepCopy(evalRunDetailMap),
                "releaseRecordList", deepCopy(listOfMaps(releaseRecordList)),
                "releaseEventMap", deepCopy(releaseEventMap),
                "grayConfigList", deepCopy(listOfMaps(grayConfigList))
        );
    }

    private Map<String, Map<String, Object>> snapshotGovernanceStructuredState() {
        return m2(
                "eval_cases", listToStructuredMap(evalCaseList),
                "eval_versions", listToStructuredMap(evalVersionList),
                "regression_sets", listToStructuredMap(regressionSetList),
                "eval_runs", listToStructuredMap(evalRunList),
                "release_records", listToStructuredMap(releaseRecordList),
                "gray_configs", listToStructuredMap(grayConfigList),
                "version_details", keyedMapToStructuredMap(versionDetailMap),
                "regression_details", keyedMapToStructuredMap(regressionDetailMap),
                "eval_run_details", keyedMapToStructuredMap(evalRunDetailMap),
                "release_events", listMapToStructuredMap(releaseEventMap)
        );
    }

    private void replaceList(List<Map<String, Object>> target, List<Map<String, Object>> source) {
        target.clear();
        target.addAll(copyList(source));
    }

    private void replaceKeyedMap(Map<Long, Map<String, Object>> target, Map<Long, Map<String, Object>> source) {
        target.clear();
        target.putAll(source);
    }

    private void replaceListMap(Map<Long, List<Map<String, Object>>> target, Map<Long, List<Map<String, Object>>> source) {
        target.clear();
        target.putAll(source);
    }

    private List<Map<String, Object>> restoreStructuredList(Map<String, Object> source) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (source == null || source.isEmpty()) {
            return result;
        }
        List<String> keys = new ArrayList<String>(source.keySet());
        keys.sort(String::compareTo);
        for (String key : keys) {
            result.add(asMap(source.get(key)));
        }
        sort(result, "updatedAt");
        return result;
    }

    private Map<Long, Map<String, Object>> restoreStructuredKeyedMap(Map<String, Object> source) {
        Map<Long, Map<String, Object>> result = new LinkedHashMap<Long, Map<String, Object>>();
        if (source == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            result.put(longVal(entry.getKey(), 0L), asMap(entry.getValue()));
        }
        return result;
    }

    private Map<Long, List<Map<String, Object>>> restoreStructuredListMap(Map<String, Object> source) {
        Map<Long, List<Map<String, Object>>> result = new LinkedHashMap<Long, List<Map<String, Object>>>();
        if (source == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Map<String, Object> wrapper = asMap(entry.getValue());
            result.put(longVal(entry.getKey(), 0L), copyList(listOfMaps(wrapper.get("events"))));
        }
        return result;
    }

    private Map<String, Object> listToStructuredMap(List<Map<String, Object>> source) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map<String, Object> item : source) {
            result.put(String.valueOf(longVal(item.get("id"), 0L)), deepCopy(item));
        }
        return result;
    }

    private Map<String, Object> keyedMapToStructuredMap(Map<Long, Map<String, Object>> source) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<Long, Map<String, Object>> entry : source.entrySet()) {
            result.put(String.valueOf(entry.getKey()), deepCopy(entry.getValue()));
        }
        return result;
    }

    private Map<String, Object> listMapToStructuredMap(Map<Long, List<Map<String, Object>>> source) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<Long, List<Map<String, Object>>> entry : source.entrySet()) {
            result.put(String.valueOf(entry.getKey()), m("events", deepCopy(entry.getValue())));
        }
        return result;
    }

    private void removeCaseFromSnapshots(Long caseId) {
        for (Map<String, Object> detail : versionDetailMap.values()) {
            List<Map<String, Object>> items = listOfMaps(detail.get("items"));
            items.removeIf(item -> longVal(item.get("caseId"), 0L) == caseId.longValue());
            detail.put("items", items);
        }
        for (Map<String, Object> detail : regressionDetailMap.values()) {
            List<Map<String, Object>> items = listOfMaps(detail.get("items"));
            items.removeIf(item -> longVal(item.get("caseId"), 0L) == caseId.longValue());
            detail.put("items", items);
        }
    }

    private void clearGrayFromReleases(Long grayConfigId) {
        for (Map<String, Object> release : releaseRecordList) {
            Map<String, Object> strategy = parseJsonObject(str(release.get("grayStrategyJson")));
            if (longVal(strategy.get("configId"), 0L) == grayConfigId.longValue()) {
                release.put("grayStrategyJson", "");
                release.put("updatedAt", ts(0, 0));
            }
        }
    }

    private void appendReleaseEvent(Long releaseId, String eventType, String fromStatus, String toStatus, Map<String, Object> detail) {
        List<Map<String, Object>> events = releaseEventMap.containsKey(releaseId)
                ? releaseEventMap.get(releaseId)
                : new ArrayList<Map<String, Object>>();
        events.add(0, m(
                "id", nextReleaseEventId(),
                "releaseRecordId", releaseId,
                "eventType", eventType,
                "fromStatus", fromStatus,
                "toStatus", toStatus,
                "operatorName", "front-admin",
                "eventDetailJson", json(detail),
                "createdAt", ts(0, 0)
        ));
        releaseEventMap.put(releaseId, events);
    }

    private List<Map<String, Object>> filterVersions(String bucket) {
        List<Map<String, Object>> items = evalVersionList.stream().filter(item -> !StringUtils.hasText(bucket) || bucket.equals(str(item.get("bucket")))).map(LinkedHashMap::new).collect(Collectors.toList());
        sort(items, "createdAt");
        return items;
    }

    private List<Map<String, Object>> filterRegression(String bucket, String risk) {
        List<Map<String, Object>> items = regressionSetList.stream().filter(item -> !StringUtils.hasText(bucket) || bucket.equals(str(item.get("bucket")))).filter(item -> !StringUtils.hasText(risk) || risk.equals(str(item.get("riskLevel")))).map(LinkedHashMap::new).collect(Collectors.toList());
        sort(items, "createdAt");
        return items;
    }

    private List<Map<String, Object>> filterRuns(String bucket, String sourceType, Long versionId, Long regressionSetId) {
        List<Map<String, Object>> items = evalRunList.stream().filter(item -> !StringUtils.hasText(bucket) || bucket.equals(str(item.get("bucket")))).filter(item -> !StringUtils.hasText(sourceType) || sourceType.equals(str(item.get("sourceType")))).filter(item -> versionId == null || versionId.longValue() == longVal(item.get("versionId"), 0L)).filter(item -> regressionSetId == null || regressionSetId.longValue() == longVal(item.get("regressionSetId"), 0L)).map(LinkedHashMap::new).collect(Collectors.toList());
        sort(items, "createdAt");
        return items;
    }

    private List<Map<String, Object>> filterRelease(String status) {
        List<Map<String, Object>> items = releaseRecordList.stream().filter(item -> !StringUtils.hasText(status) || status.equals(str(item.get("releaseStatus")))).map(LinkedHashMap::new).collect(Collectors.toList());
        sort(items, "updatedAt");
        return items;
    }

    private List<Map<String, Object>> filterGray(Integer enabled) {
        List<Map<String, Object>> items = grayConfigList.stream().filter(item -> enabled == null || intVal(item.get("enabled"), 1) == enabled.intValue()).map(LinkedHashMap::new).collect(Collectors.toList());
        sort(items, "updatedAt");
        return items;
    }

    private Map<String, Object> latestRunForVersion(Long versionId) {
        List<Map<String, Object>> items = filterRuns("", "", versionId, null);
        return items.isEmpty() ? null : items.get(0);
    }

    private Map<String, Object> runResultById(Long id) {
        Map<String, Object> detail = evalRunDetailMap.get(id);
        if (detail == null) {
            throw new IllegalArgumentException("Eval run not found: " + id);
        }
        return new LinkedHashMap<String, Object>(detail);
    }

    private List<Map<String, Object>> metricsDailyInternal(int days) {
        int safeDays = days <= 0 ? 7 : days;
        LocalDate start = LocalDate.now().minusDays(safeDays - 1L);
        Map<String, Map<String, Object>> buckets = new LinkedHashMap<String, Map<String, Object>>();
        Map<String, Map<String, Integer>> nodes = new LinkedHashMap<String, Map<String, Integer>>();
        for (int i = 0; i < safeDays; i++) {
            String date = start.plusDays(i).toString();
            buckets.put(date, m("date", date, "replayTotal", 0, "degradedTotal", 0, "errorTotal", 0, "avgDurationMs", 0L, "topFailedNode", "-", "durationSum", 0L, "durationCount", 0));
        }
        for (Map<String, Object> item : combinedReplaySummaries()) {
            String date = dateOnly(str(item.get("createdAt")));
            if (!buckets.containsKey(date)) {
                continue;
            }
            Map<String, Object> bucket = buckets.get(date);
            bucket.put("replayTotal", intVal(bucket.get("replayTotal"), 0) + 1);
            if (boolVal(item.get("degraded"), false)) {
                bucket.put("degradedTotal", intVal(bucket.get("degradedTotal"), 0) + 1);
            }
            if (StringUtils.hasText(str(item.get("errorCode"))) || StringUtils.hasText(str(item.get("failedNode")))) {
                bucket.put("errorTotal", intVal(bucket.get("errorTotal"), 0) + 1);
            }
            bucket.put("durationSum", longVal(bucket.get("durationSum"), 0L) + longVal(item.get("durationMs"), 0L));
            bucket.put("durationCount", intVal(bucket.get("durationCount"), 0) + 1);
            if (StringUtils.hasText(str(item.get("failedNode")))) {
                Map<String, Integer> counts = nodes.containsKey(date) ? nodes.get(date) : new LinkedHashMap<String, Integer>();
                addCount(counts, str(item.get("failedNode")));
                nodes.put(date, counts);
            }
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> bucket : buckets.values()) {
            int count = intVal(bucket.get("durationCount"), 0);
            bucket.put("avgDurationMs", count == 0 ? 0L : longVal(bucket.get("durationSum"), 0L) / count);
            bucket.put("topFailedNode", topNode(nodes.get(str(bucket.get("date")))));
            bucket.remove("durationSum");
            bucket.remove("durationCount");
            result.add(bucket);
        }
        return result;
    }

    private List<Map<String, Object>> countBreakdown(Map<String, Integer> counts, int limit) {
        if (counts == null || counts.isEmpty()) {
            return Collections.emptyList();
        }
        return counts.entrySet().stream()
                .sorted((left, right) -> {
                    int value = Integer.compare(right.getValue(), left.getValue());
                    return value != 0 ? value : left.getKey().compareTo(right.getKey());
                })
                .limit(limit <= 0 ? counts.size() : limit)
                .map(entry -> m("key", entry.getKey(), "count", entry.getValue()))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> combinedReplaySummaries() {
        Map<String, Map<String, Object>> combined = new LinkedHashMap<String, Map<String, Object>>();
        for (GovernanceReplayRecordEntity entity : replayStore.listRecent("", 50)) {
            combined.put(entity.getRequestId(), m("requestId", entity.getRequestId(), "traceId", entity.getTraceId(), "sessionId", entity.getSessionId(), "userId", entity.getUserId(), "lastUserMessage", entity.getLastUserMessage(), "taskType", entity.getTaskType(), "planType", entity.getPlanType(), "answerType", entity.getAnswerType(), "errorCode", entity.getErrorCode(), "failedNode", entity.getFailedNode(), "degraded", entity.getDegraded() != null && entity.getDegraded().intValue() == 1, "durationMs", entity.getDurationMs(), "createdAt", entity.getCreatedAt() == null ? ts(0, 0) : FORMATTER.format(entity.getCreatedAt())));
        }
        for (Map<String, Object> detail : replayDetailByRequest.values()) {
            Map<String, Object> summary = asMap(detail.get("summary"));
            if (!combined.containsKey(str(summary.get("requestId")))) {
                combined.put(str(summary.get("requestId")), new LinkedHashMap<String, Object>(summary));
            }
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(combined.values());
        sort(result, "createdAt");
        return result;
    }

    private Map<String, Object> actualReplayDetailByRequest(String requestId) {
        GovernanceReplayRecordEntity entity = replayStore.findByRequestId(requestId);
        return entity == null ? null : actualDetail(entity);
    }

    private Map<String, Object> actualReplayDetailByTrace(String traceId) {
        GovernanceReplayRecordEntity entity = replayStore.findByTraceId(traceId);
        return entity == null ? null : actualDetail(entity);
    }

    private Map<String, Object> actualReplayDetailBySession(String sessionId) {
        GovernanceReplayRecordEntity entity = replayStore.findLatestBySessionId(sessionId);
        return entity == null ? null : actualDetail(entity);
    }

    private Map<String, Object> actualDetail(GovernanceReplayRecordEntity entity) {
        return m("summary", m("requestId", entity.getRequestId(), "traceId", entity.getTraceId(), "sessionId", entity.getSessionId(), "userId", entity.getUserId(), "lastUserMessage", entity.getLastUserMessage(), "taskType", entity.getTaskType(), "planType", entity.getPlanType(), "answerType", entity.getAnswerType(), "errorCode", entity.getErrorCode(), "failedNode", entity.getFailedNode(), "degraded", entity.getDegraded() != null && entity.getDegraded().intValue() == 1, "durationMs", entity.getDurationMs(), "createdAt", entity.getCreatedAt() == null ? ts(0, 0) : FORMATTER.format(entity.getCreatedAt())), "requestSnapshot", replayStore.readJsonObject(entity.getRequestJson()), "stateSnapshot", replayStore.readJsonObject(entity.getStateJson()), "checkpoints", replayStore.loadCheckpoints(entity.getId()).stream().map(this::checkpoint).collect(Collectors.toList()), "toolIos", replayStore.loadToolIos(entity.getId()).stream().map(this::toolIo).collect(Collectors.toList()), "sessionHistory", replayStore.listSessionHistory(entity.getSessionId(), 20).stream().map(this::summary).collect(Collectors.toList()));
    }

    private Map<String, Object> withSessionHistory(Map<String, Object> detail) {
        Map<String, Object> copy = new LinkedHashMap<String, Object>(detail);
        String sessionId = str(asMap(detail.get("summary")).get("sessionId"));
        List<Map<String, Object>> sessionHistory = replayDetailByRequest.values().stream().map(item -> asMap(item.get("summary"))).filter(item -> sessionId.equals(str(item.get("sessionId")))).map(LinkedHashMap::new).collect(Collectors.toList());
        sort(sessionHistory, "createdAt");
        copy.put("sessionHistory", sessionHistory);
        return copy;
    }

    private Map<String, Object> checkpoint(GovernanceReplayCheckpointEntity entity) {
        return m("checkpointOrder", entity.getCheckpointOrder(), "nodeName", entity.getNodeName(), "stateSnapshot", replayStore.readJsonObject(entity.getStateSnapshotJson()), "createdAt", entity.getCreatedAt() == null ? ts(0, 0) : FORMATTER.format(entity.getCreatedAt()));
    }

    private Map<String, Object> toolIo(GovernanceReplayToolIoEntity entity) {
        return m("stepOrder", entity.getStepOrder(), "stepId", entity.getStepId(), "toolName", entity.getToolName(), "purpose", entity.getPurpose(), "outputKey", entity.getOutputKey(), "optionalStep", entity.getOptionalStep() != null && entity.getOptionalStep().intValue() == 1, "executionStatus", entity.getExecutionStatus(), "inputPayload", replayStore.readJsonObject(entity.getInputJson()), "outputPayload", replayStore.readJsonObject(entity.getOutputJson()), "createdAt", entity.getCreatedAt() == null ? ts(0, 0) : FORMATTER.format(entity.getCreatedAt()));
    }

    private Map<String, Object> summary(GovernanceReplayRecordEntity entity) {
        return m("requestId", entity.getRequestId(), "traceId", entity.getTraceId(), "sessionId", entity.getSessionId(), "userId", entity.getUserId(), "lastUserMessage", entity.getLastUserMessage(), "taskType", entity.getTaskType(), "planType", entity.getPlanType(), "answerType", entity.getAnswerType(), "errorCode", entity.getErrorCode(), "failedNode", entity.getFailedNode(), "degraded", entity.getDegraded() != null && entity.getDegraded().intValue() == 1, "durationMs", entity.getDurationMs(), "createdAt", entity.getCreatedAt() == null ? ts(0, 0) : FORMATTER.format(entity.getCreatedAt()));
    }

    private void seedReplay(Map<String, Object> summary, Map<String, Object> payload) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("summary", summary);
        detail.put("requestSnapshot", payload.get("requestSnapshot"));
        detail.put("stateSnapshot", payload.get("stateSnapshot"));
        detail.put("checkpoints", payload.get("checkpoints"));
        detail.put("toolIos", payload.get("toolIos"));
        replayDetailByRequest.put(str(summary.get("requestId")), detail);
        replayTraceIndex.put(str(summary.get("traceId")), str(summary.get("requestId")));
        replaySessionIndex.put(str(summary.get("sessionId")), str(summary.get("requestId")));
    }

    private Map<String, Object> snapshot(Map<String, Object> item) {
        return m("caseId", item.get("id"), "caseName", item.get("caseName"), "queryText", item.get("queryText"), "bucket", item.get("bucket"), "riskLevel", item.get("riskLevel"), "expectedTaskType", item.get("expectedTaskType"), "expectedPlanType", item.get("expectedPlanType"), "expectedAnswerType", item.get("expectedAnswerType"));
    }

    private Map<String, Object> snapshotCase(Map<String, Object> item) {
        return m(
                "caseId", item.get("id"),
                "caseName", item.get("caseName"),
                "queryText", item.get("queryText"),
                "bucket", item.get("bucket"),
                "riskLevel", item.get("riskLevel"),
                "expectedTaskType", item.get("expectedTaskType"),
                "expectedPlanType", item.get("expectedPlanType"),
                "expectedAnswerType", item.get("expectedAnswerType")
        );
    }

    private List<Map<String, Object>> refreshSnapshotItems(List<Map<String, Object>> snapshotItems,
                                                           Map<Long, Map<String, Object>> evalCasesById) {
        List<Map<String, Object>> refreshed = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> snapshotItem : snapshotItems) {
            Map<String, Object> evalCase = evalCasesById.get(longVal(snapshotItem.get("caseId"), 0L));
            if (evalCase != null) {
                refreshed.add(new LinkedHashMap<String, Object>(snapshotItem));
            }
        }
        return refreshed;
    }

    private boolean snapshotChanged(Map<String, Object> left, Map<String, Object> right) {
        return !sameValue(left.get("caseName"), right.get("caseName"))
                || !sameValue(left.get("queryText"), right.get("queryText"))
                || !sameValue(left.get("bucket"), right.get("bucket"))
                || !sameValue(left.get("riskLevel"), right.get("riskLevel"))
                || !sameValue(left.get("expectedTaskType"), right.get("expectedTaskType"))
                || !sameValue(left.get("expectedPlanType"), right.get("expectedPlanType"))
                || !sameValue(left.get("expectedAnswerType"), right.get("expectedAnswerType"));
    }

    private boolean runCaseChanged(Map<String, Object> left, Map<String, Object> right) {
        return boolVal(left.get("passed"), false) != boolVal(right.get("passed"), false)
                || !sameValue(left.get("actualTaskType"), right.get("actualTaskType"))
                || !sameValue(left.get("actualPlanType"), right.get("actualPlanType"))
                || !sameValue(left.get("actualAnswerType"), right.get("actualAnswerType"))
                || !sameValue(left.get("reply"), right.get("reply"));
    }

    private Map<String, Object> executeEvalCase(Map<String, Object> item) {
        String queryText = str(item.get("queryText"));
        String expectedTaskType = str(item.get("expectedTaskType"));
        String expectedPlanType = str(item.get("expectedPlanType"));
        String expectedAnswerType = str(item.get("expectedAnswerType"));
        String actualTaskType = "";
        String actualPlanType = "";
        String actualAnswerType = "";
        boolean degraded = false;
        String reply = "";
        String errorMessage = "";
        try {
            actualTaskType = resolveActualTaskType(queryText);
            SessionState runtimeState = executeRuntime(queryText);
            if (runtimeState != null) {
                actualPlanType = resolveActualPlanType(runtimeState);
                actualAnswerType = resolveActualAnswerType(runtimeState);
                degraded = runtimeState.getExecutionMeta() != null && runtimeState.getExecutionMeta().isDegraded();
                reply = runtimeState.getFinalAnswer() == null ? "" : str(runtimeState.getFinalAnswer().getAnswerText());
                if (!StringUtils.hasText(errorMessage) && runtimeState.getExecutionMeta() != null) {
                    errorMessage = str(runtimeState.getExecutionMeta().getErrorCode());
                }
            }
        } catch (Exception ex) {
            errorMessage = ex.getMessage();
            reply = defaultIfBlank(reply, "eval execution failed");
        }
        boolean taskTypeMatched = matchesExpected(expectedTaskType, actualTaskType);
        boolean planTypeMatched = matchesExpected(expectedPlanType, actualPlanType);
        boolean answerTypeMatched = matchesExpected(expectedAnswerType, actualAnswerType);
        boolean passed = taskTypeMatched && planTypeMatched && answerTypeMatched && !StringUtils.hasText(errorMessage);
        return m(
                "caseId", item.get("caseId"),
                "caseName", item.get("caseName"),
                "queryText", queryText,
                "expectedTaskType", expectedTaskType,
                "actualTaskType", actualTaskType,
                "taskTypeMatched", taskTypeMatched,
                "expectedPlanType", expectedPlanType,
                "actualPlanType", actualPlanType,
                "planTypeMatched", planTypeMatched,
                "expectedAnswerType", expectedAnswerType,
                "actualAnswerType", actualAnswerType,
                "answerTypeMatched", answerTypeMatched,
                "degraded", degraded,
                "passed", passed,
                "reply", reply,
                "errorMessage", errorMessage
        );
    }

    private String resolveActualTaskType(String queryText) {
        if (!StringUtils.hasText(queryText) || queryParserService == null) {
            return "";
        }
        try {
            ParsedIntent parsedIntent = queryParserService.parse(queryText, Collections.<AgentChatMessage>emptyList());
            TaskType taskType = parsedIntent == null ? null : parsedIntent.getTaskType();
            return taskType == null ? "" : taskType.getCode();
        } catch (Exception ex) {
            return "";
        }
    }

    private SessionState executeRuntime(String queryText) {
        if (!StringUtils.hasText(queryText) || agentRuntime == null) {
            return null;
        }
        AgentChatMessage message = new AgentChatMessage();
        message.setRole("user");
        message.setContent(queryText);
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("gov-eval-" + UUID.randomUUID().toString().replace("-", ""));
        request.setMessages(Collections.singletonList(message));
        return agentRuntime.run(request, null);
    }

    private String resolveActualPlanType(SessionState runtimeState) {
        if (runtimeState == null) {
            return "";
        }
        Object explicit = runtimeState.getIntermediateData().get("planType");
        if (StringUtils.hasText(str(explicit))) {
            return str(explicit);
        }
        Object metadata = runtimeState.getFinalAnswer() != null && runtimeState.getFinalAnswer().getDebugTrace() != null
                ? runtimeState.getFinalAnswer().getDebugTrace().getMetadata().get("planType")
                : null;
        if (StringUtils.hasText(str(metadata))) {
            return str(metadata);
        }
        List<String> completedNodes = runtimeState.getExecutionMeta() == null
                ? Collections.<String>emptyList()
                : runtimeState.getExecutionMeta().getCompletedNodes();
        return completedNodes.isEmpty() ? "" : str(completedNodes.get(completedNodes.size() - 1));
    }

    private String resolveActualAnswerType(SessionState runtimeState) {
        if (runtimeState == null || runtimeState.getFinalAnswer() == null || runtimeState.getFinalAnswer().getAnswerType() == null) {
            return "";
        }
        AnswerType answerType = runtimeState.getFinalAnswer().getAnswerType();
        return answerType.getCode();
    }

    private boolean matchesExpected(String expected, String actual) {
        return !StringUtils.hasText(expected) || expected.equalsIgnoreCase(defaultIfBlank(actual, ""));
    }

    private boolean sameValue(Object left, Object right) {
        return str(left).equals(str(right));
    }

    private String normalizeReleaseStatus(String status) {
        String normalized = defaultIfBlank(str(status), "");
        if (RELEASE_STATUS_DRAFT.equals(normalized)
                || RELEASE_STATUS_READY.equals(normalized)
                || RELEASE_STATUS_GRAY.equals(normalized)
                || RELEASE_STATUS_RELEASED.equals(normalized)
                || RELEASE_STATUS_ROLLED_BACK.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Unsupported releaseStatus: " + status);
    }

    private void assertReleaseTransitionAllowed(String currentStatus, String targetStatus) {
        if (RELEASE_STATUS_DRAFT.equals(currentStatus)) {
            if (!RELEASE_STATUS_READY.equals(targetStatus)) {
                throw new IllegalArgumentException("draft can only transition to ready");
            }
            return;
        }
        if (RELEASE_STATUS_READY.equals(currentStatus)) {
            if (!RELEASE_STATUS_GRAY.equals(targetStatus)) {
                throw new IllegalArgumentException("ready can only transition to gray");
            }
            return;
        }
        if (RELEASE_STATUS_GRAY.equals(currentStatus)) {
            if (!RELEASE_STATUS_RELEASED.equals(targetStatus) && !RELEASE_STATUS_ROLLED_BACK.equals(targetStatus)) {
                throw new IllegalArgumentException("gray can only transition to released or rolled_back");
            }
            return;
        }
        if (RELEASE_STATUS_RELEASED.equals(currentStatus)) {
            if (!RELEASE_STATUS_ROLLED_BACK.equals(targetStatus)) {
                throw new IllegalArgumentException("released can only transition to rolled_back");
            }
            return;
        }
        throw new IllegalArgumentException("rolled_back does not allow further transitions");
    }

    private int sanitizeMaxRunAgeHours(Integer maxRunAgeHours) {
        return maxRunAgeHours == null || maxRunAgeHours.intValue() <= 0 ? DEFAULT_MAX_RUN_AGE_HOURS : maxRunAgeHours.intValue();
    }

    private boolean isRunFresh(Map<String, Object> run, int maxRunAgeHours) {
        if (run == null) {
            return false;
        }
        LocalDateTime createdAt = parseTimestamp(run.get("createdAt"));
        if (createdAt == null) {
            return false;
        }
        return !createdAt.isBefore(LocalDateTime.now().minusHours(maxRunAgeHours));
    }

    private LocalDateTime parseTimestamp(Object value) {
        if (!StringUtils.hasText(str(value))) {
            return null;
        }
        try {
            return LocalDateTime.parse(str(value), FORMATTER);
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Object> resolveLatestReleaseRun(Map<String, Object> release) {
        Long latestRunId = longVal(release.get("latestEvalRunId"), 0L);
        if (latestRunId > 0L) {
            return asMap(runResultById(latestRunId).get("run"));
        }
        return latestRelevantRun(longVal(release.get("evalCaseVersionId"), 0L), longVal(release.get("regressionSetId"), 0L));
    }

    private Map<String, Object> latestRelevantRun(Long evalCaseVersionId, Long regressionSetId) {
        if (regressionSetId != null && regressionSetId.longValue() > 0L) {
            List<Map<String, Object>> runs = filterRuns("", "regression_set", null, regressionSetId);
            return runs.isEmpty() ? null : runs.get(0);
        }
        if (evalCaseVersionId != null && evalCaseVersionId.longValue() > 0L) {
            List<Map<String, Object>> runs = filterRuns("", "version_snapshot", evalCaseVersionId, null);
            return runs.isEmpty() ? null : runs.get(0);
        }
        return null;
    }

    private Map<String, Object> resolveDefaultPreflightScope(Long evalCaseVersionId, Long regressionSetId) {
        Long resolvedVersionId = positiveLong(evalCaseVersionId);
        Long resolvedRegressionSetId = positiveLong(regressionSetId);
        if (resolvedVersionId != null || resolvedRegressionSetId != null) {
            return m(
                    "source", "explicit",
                    "releaseId", null,
                    "evalCaseVersionId", resolvedVersionId,
                    "regressionSetId", resolvedRegressionSetId
            );
        }
        Map<String, Object> latestRelease = latestLaunchCandidateRelease();
        if (!latestRelease.isEmpty()) {
            return m(
                    "source", "latest_release",
                    "releaseId", latestRelease.get("id"),
                    "evalCaseVersionId", positiveLong(latestRelease.get("evalCaseVersionId")),
                    "regressionSetId", positiveLong(latestRelease.get("regressionSetId"))
            );
        }
        return m(
                "source", "none",
                "releaseId", null,
                "evalCaseVersionId", null,
                "regressionSetId", null
        );
    }

    private Map<String, Object> latestLaunchCandidateRelease() {
        List<Map<String, Object>> releases = filterRelease("");
        for (Map<String, Object> release : releases) {
            if (!isLaunchCandidateStatus(str(release.get("releaseStatus")))) {
                continue;
            }
            if (positiveLong(release.get("evalCaseVersionId")) != null || positiveLong(release.get("regressionSetId")) != null) {
                return release;
            }
        }
        for (Map<String, Object> release : releases) {
            if (positiveLong(release.get("evalCaseVersionId")) != null || positiveLong(release.get("regressionSetId")) != null) {
                return release;
            }
        }
        return Collections.emptyMap();
    }

    private boolean isLaunchCandidateStatus(String status) {
        return RELEASE_STATUS_READY.equals(status)
                || RELEASE_STATUS_GRAY.equals(status)
                || RELEASE_STATUS_RELEASED.equals(status);
    }

    private double calculateDegradedRate(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            return 0D;
        }
        int degradedCount = 0;
        for (Map<String, Object> result : results) {
            if (boolVal(result.get("degraded"), false)) {
                degradedCount++;
            }
        }
        return (double) degradedCount / results.size();
    }

    private Map<String, Map<String, Object>> indexItems(List<Map<String, Object>> items) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<String, Map<String, Object>>();
        for (Map<String, Object> item : items) {
            map.put(key(item.get("caseId")) + ":" + key(item.get("queryText")), item);
        }
        return map;
    }

    private Map<String, Object> findById(List<Map<String, Object>> items, Long id, String error) {
        for (Map<String, Object> item : items) {
            if (longVal(item.get("id"), 0L) == id.longValue()) {
                return new LinkedHashMap<String, Object>(item);
            }
        }
        throw new IllegalArgumentException(error);
    }

    private Map<String, Object> mutableById(List<Map<String, Object>> items, Long id, String error) {
        Map<String, Object> item = findMutableOrNull(items, id);
        if (item == null) {
            throw new IllegalArgumentException(error);
        }
        return item;
    }

    private Map<String, Object> findMutableOrNull(List<Map<String, Object>> items, Long id) {
        for (Map<String, Object> item : items) {
            if (longVal(item.get("id"), 0L) == id.longValue()) {
                return item;
            }
        }
        return null;
    }

    private boolean removeById(List<Map<String, Object>> items, Long id) {
        for (int i = 0; i < items.size(); i++) {
            if (longVal(items.get(i).get("id"), 0L) == id.longValue()) {
                items.remove(i);
                return true;
            }
        }
        return false;
    }

    private boolean containsId(List<Map<String, Object>> items, Long id) {
        return findMutableOrNull(items, id) != null;
    }

    private void mergeIfPresent(Map<String, Object> target, Map<String, Object> source, String... keys) {
        for (String key : keys) {
            if (source.containsKey(key)) {
                target.put(key, source.get(key));
            }
        }
    }

    private Map<String, Object> safeBody(Map<String, Object> body) {
        return body == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(body);
    }

    private long nextId(List<Map<String, Object>> items) {
        long max = 0L;
        for (Map<String, Object> item : items) {
            max = Math.max(max, longVal(item.get("id"), 0L));
        }
        return max + 1L;
    }

    private long nextReleaseEventId() {
        long max = 0L;
        for (List<Map<String, Object>> events : releaseEventMap.values()) {
            for (Map<String, Object> event : events) {
                max = Math.max(max, longVal(event.get("id"), 0L));
            }
        }
        return max + 1L;
    }

    private int countEnabled(List<Map<String, Object>> items) {
        int count = 0;
        for (Map<String, Object> item : items) {
            if (intVal(item.get("enabled"), 1) == 1) {
                count++;
            }
        }
        return count;
    }

    private int countEnabledBySnapshot(List<Map<String, Object>> snapshotItems, Map<Long, Map<String, Object>> evalCasesById) {
        int count = 0;
        for (Map<String, Object> item : snapshotItems) {
            Map<String, Object> evalCase = evalCasesById.get(longVal(item.get("caseId"), 0L));
            if (evalCase != null && intVal(evalCase.get("enabled"), 1) == 1) {
                count++;
            }
        }
        return count;
    }

    private String inferRunBucket(List<Map<String, Object>> items) {
        if (items.isEmpty()) {
            return "all";
        }
        String bucket = str(items.get(0).get("bucket"));
        for (Map<String, Object> item : items) {
            if (!bucket.equals(str(item.get("bucket")))) {
                return "mixed";
            }
        }
        return defaultIfBlank(bucket, "all");
    }

    private Map<String, Object> parseJsonObject(String value) {
        if (!StringUtils.hasText(value)) {
            return new LinkedHashMap<String, Object>();
        }
        try {
            return objectMapper.readValue(value, LinkedHashMap.class);
        } catch (Exception ex) {
            return new LinkedHashMap<String, Object>();
        }
    }

    private void addCount(Map<String, Integer> counts, String key) {
        counts.put(key, counts.containsKey(key) ? counts.get(key) + 1 : 1);
    }

    private int sum(List<Map<String, Object>> items, String key) {
        int total = 0;
        for (Map<String, Object> item : items) {
            total += intVal(item.get(key), 0);
        }
        return total;
    }

    private String topNode(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return "-";
        }
        return counts.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).map(Map.Entry::getKey).orElse("-");
    }

    private String failureCombo(Map<String, Object> item) {
        String failedNode = defaultIfBlank(str(item.get("failedNode")), "-");
        String errorCode = defaultIfBlank(str(item.get("errorCode")), "-");
        return failedNode + " | " + errorCode;
    }

    private boolean contains(Map<String, Object> item, String keyword) {
        String q = key(keyword);
        return key(item.get("requestId")).contains(q) || key(item.get("traceId")).contains(q) || key(item.get("sessionId")).contains(q) || key(item.get("lastUserMessage")).contains(q) || key(item.get("caseName")).contains(q) || key(item.get("queryText")).contains(q) || key(item.get("releaseName")).contains(q) || key(item.get("configName")).contains(q);
    }

    private boolean withinDays(String createdAt, int days) {
        LocalDate d = LocalDate.parse(dateOnly(createdAt));
        return !d.isBefore(LocalDate.now().minusDays(Math.max(0, days - 1)));
    }

    private String dateOnly(String value) {
        return value != null && value.length() >= 10 ? value.substring(0, 10) : LocalDate.now().toString();
    }

    private String key(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toLowerCase();
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int intVal(Object value, int fallback) {
        try {
            return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private long longVal(Object value, long fallback) {
        try {
            return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private Long positiveLong(Object value) {
        long resolved = longVal(value, 0L);
        return resolved > 0L ? Long.valueOf(resolved) : null;
    }

    private double doubleVal(Object value, double fallback) {
        try {
            return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private boolean boolVal(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return "1".equals(String.valueOf(value)) || Boolean.parseBoolean(String.valueOf(value));
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof List)) {
            return new ArrayList<Map<String, Object>>();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> asListOfStrings(Object value) {
        if (!(value instanceof List)) {
            return new ArrayList<String>();
        }
        List<String> result = new ArrayList<String>();
        for (Object item : (List<?>) value) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Map<String, Object>> mapOfMaps(Object value) {
        if (!(value instanceof Map)) {
            return new LinkedHashMap<Long, Map<String, Object>>();
        }
        Map<Long, Map<String, Object>> result = new LinkedHashMap<Long, Map<String, Object>>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            result.put(longVal(entry.getKey(), 0L), asMap(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, List<Map<String, Object>>> mapOfListOfMaps(Object value) {
        if (!(value instanceof Map)) {
            return new LinkedHashMap<Long, List<Map<String, Object>>>();
        }
        Map<Long, List<Map<String, Object>>> result = new LinkedHashMap<Long, List<Map<String, Object>>>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            result.put(longVal(entry.getKey(), 0L), copyList(listOfMaps(entry.getValue())));
        }
        return result;
    }

    private List<Long> longList(Object value) {
        List<Long> result = new ArrayList<Long>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                result.add(longVal(item, 0L));
            }
        }
        return result;
    }

    private <T> List<T> page(List<T> items, int page, int size) {
        int safePage = page <= 0 ? 1 : page;
        int safeSize = size <= 0 ? 20 : size;
        int from = Math.min(items.size(), (safePage - 1) * safeSize);
        int to = Math.min(items.size(), from + safeSize);
        return new ArrayList<T>(items.subList(from, to));
    }

    private void sort(List<Map<String, Object>> items, String key) {
        items.sort((a, b) -> str(b.get(key)).compareTo(str(a.get(key))));
    }

    private String ts(int daysAgo, int hoursAgo) {
        return FORMATTER.format(LocalDateTime.now().minusDays(daysAgo).minusHours(hoursAgo));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private Map<String, Object> m(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    private Map<String, Map<String, Object>> m2(Object... kv) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<String, Map<String, Object>>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            @SuppressWarnings("unchecked")
            Map<String, Object> value = (Map<String, Object>) kv[i + 1];
            map.put(String.valueOf(kv[i]), value);
        }
        return map;
    }

    @SafeVarargs
    private final List<Map<String, Object>> list(Map<String, Object>... items) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Collections.addAll(result, items);
        return result;
    }

    private List<Map<String, Object>> copyList(List<Map<String, Object>> source) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (source != null) {
            for (Map<String, Object> item : source) {
                result.add(new LinkedHashMap<String, Object>(item));
            }
        }
        return result;
    }

    private Object deepCopy(Object value) {
        return objectMapper.convertValue(value, Object.class);
    }
}

