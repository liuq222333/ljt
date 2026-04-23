package com.example.demo.demosAdmin.Governance.Service;

import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Runtime.AgentRuntime;
import com.example.demo.demos.Agent.Runtime.SessionState;
import com.example.demo.demos.Agent.Service.QueryParserService;
import com.example.demo.demos.common.enums.AnswerType;
import com.example.demo.demos.common.enums.TaskType;
import com.example.demo.demos.common.schema.FinalAnswer;
import com.example.demo.demos.governance.replay.GovernanceReplayStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminGovernanceServiceTest {

    @Mock
    private GovernanceReplayStore replayStore;

    @Mock
    private GovernanceAdminStateStore governanceAdminStateStore;

    @Mock
    private QueryParserService queryParserService;

    @Mock
    private AgentRuntime agentRuntime;

    private AdminGovernanceService service;

    @BeforeEach
    void setUp() {
        when(governanceAdminStateStore.loadStructured("admin_governance")).thenReturn(Collections.emptyMap());
        when(governanceAdminStateStore.load("admin_governance")).thenReturn(null);
        service = new AdminGovernanceService(replayStore, new ObjectMapper(), governanceAdminStateStore, queryParserService, agentRuntime);
        service.seed();
        clearInvocations(governanceAdminStateStore, queryParserService, agentRuntime, replayStore);
    }

    @Test
    void dashboardShouldExposeCurrentGovernanceStage() {
        Map<String, Object> dashboard = service.dashboard(7, 8, 8);

        assertEquals("W13", asMap(dashboard.get("overview")).get("stage"));
    }

    @Test
    void metricsDailyDetailShouldExposeBreakdownsForSpecificDate() {
        Map<String, Object> detail = service.metricsDailyDetail(java.time.LocalDate.now().toString());

        Map<String, Object> summary = asMap(detail.get("summary"));
        assertEquals(java.time.LocalDate.now().toString(), detail.get("date"));
        assertTrue(intVal(summary.get("replayTotal")) >= 2);
        assertFalse(asList(detail.get("byTaskType")).isEmpty());
        assertFalse(asList(detail.get("sampleRequests")).isEmpty());
        assertEquals("plan-router", asMap(asList(detail.get("byFailedNode")).get(0)).get("key"));
    }

    @Test
    void restoredStateShouldStillKeepReplayFallbackData() {
        when(governanceAdminStateStore.loadStructured("admin_governance")).thenReturn(Collections.emptyMap());
        when(governanceAdminStateStore.load("admin_governance")).thenReturn(Collections.singletonMap("evalCaseList", Collections.emptyList()));
        when(replayStore.listRecent(anyString(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(Collections.emptyList());

        AdminGovernanceService restoredService = new AdminGovernanceService(replayStore, new ObjectMapper(), governanceAdminStateStore, queryParserService, agentRuntime);
        restoredService.seed();

        assertFalse(restoredService.replayList("", 10).isEmpty());
    }

    @Test
    void restoreShouldPreferStructuredEntriesWhenPresent() {
        Map<String, Map<String, Object>> structured = new LinkedHashMap<String, Map<String, Object>>();
        structured.put("eval_cases", Collections.singletonMap("9", map(
                "id", 9L,
                "caseName", "structured-case",
                "queryText", "structured replay",
                "bucket", "golden",
                "riskLevel", "medium",
                "enabled", 1,
                "updatedAt", "2026-04-05 12:00:00"
        )));
        when(governanceAdminStateStore.loadStructured("admin_governance")).thenReturn(structured);

        AdminGovernanceService restoredService = new AdminGovernanceService(replayStore, new ObjectMapper(), governanceAdminStateStore, queryParserService, agentRuntime);
        restoredService.seed();

        List<Map<String, Object>> items = restoredService.evalCases("structured-case", "", "", null, 1, 10);
        assertEquals(1, items.size());
        assertEquals(9L, longVal(items.get(0).get("id")));
        assertTrue(restoredService.evalCases("legacy-case", "", "", null, 1, 10).isEmpty());
    }

    @Test
    void createUpdateDeleteAndToggleEvalCaseShouldMutateState() {
        Map<String, Object> created = service.createEvalCase(map(
                "caseName", "manual-governance-case",
                "queryText", "妫€鏌ユ不鐞嗘帴鍙ｅ啓璺緞",
                "bucket", "golden",
                "riskLevel", "medium",
                "enabled", 1
        ));
        Long caseId = longVal(created.get("id"));
        assertTrue(caseId > 0L);

        Map<String, Object> updated = service.updateEvalCase(map(
                "id", caseId,
                "riskLevel", "high",
                "notes", "updated"
        ));
        assertEquals("high", updated.get("riskLevel"));
        assertEquals("updated", updated.get("notes"));

        Map<String, Object> toggled = service.batchToggleEvalCases(map(
                "ids", Collections.singletonList(caseId),
                "enabled", 0
        ));
        assertEquals(1, toggled.get("updatedCount"));

        List<Map<String, Object>> items = service.evalCases("manual-governance-case", "", "", null, 1, 10);
        assertEquals(1, items.size());
        assertEquals(0, intVal(items.get(0).get("enabled")));

        Map<String, Object> deleted = service.deleteEvalCase(caseId);
        assertEquals(Boolean.TRUE, deleted.get("deleted"));
        assertTrue(service.evalCases("manual-governance-case", "", "", null, 1, 10).isEmpty());
    }

    @Test
    void createVersionAndRunShouldProduceNewPersistedRun() {
        service.createEvalCase(map(
                "caseName", "manual-runtime-eval",
                "queryText", "smoke search vegetables",
                "bucket", "golden",
                "riskLevel", "medium",
                "enabled", 1,
                "expectedTaskType", "product_search",
                "expectedPlanType", "runtime_fallback",
                "expectedAnswerType", "text_answer"
        ));
        when(queryParserService.parse(anyString(), anyList())).thenReturn(parsedIntent(TaskType.PRODUCT_SEARCH));
        when(agentRuntime.run(any(), isNull())).thenReturn(runtimeState(AnswerType.NO_RESULT, false, "runtime_fallback", "fallback"));

        Map<String, Object> version = service.createEvalCaseVersion(map(
                "versionName", "manual-version",
                "keyword", "manual-runtime-eval",
                "bucket", "golden",
                "enabled", 1,
                "limit", 5
        ));
        Long versionId = longVal(version.get("id"));
        assertTrue(versionId > 0L);
        assertEquals("manual-version", version.get("versionName"));
        assertTrue(intVal(version.get("totalCases")) > 0);

        Map<String, Object> runResult = service.runEvalCaseVersion(versionId, 2);
        Map<String, Object> run = asMap(runResult.get("run"));
        assertEquals("version_snapshot", run.get("sourceType"));
        assertEquals(versionId, longVal(run.get("versionId")));
        assertTrue(longVal(run.get("id")) > 0L);
        assertEquals(0, intVal(run.get("passedTotal")));
        assertEquals(1, intVal(run.get("failedTotal")));

        List<Map<String, Object>> results = asList(runResult.get("results"));
        assertEquals("product_search", results.get(0).get("actualTaskType"));
        assertEquals("runtime_fallback", results.get(0).get("actualPlanType"));
        assertEquals("no_result", results.get(0).get("actualAnswerType"));
        assertEquals(Boolean.FALSE, results.get(0).get("passed"));

        Map<String, Object> detail = service.evalVersionDetail(versionId);
        assertNotNull(detail.get("latestRun"));
        assertFalse(asList(detail.get("recentRuns")).isEmpty());
    }

    @Test
    void createRegressionAndRunShouldBindToSourceVersion() {
        Map<String, Object> version = service.createEvalCaseVersion(map(
                "versionName", "regression-source",
                "bucket", "golden",
                "limit", 3
        ));
        Long versionId = longVal(version.get("id"));

        Map<String, Object> regression = service.createRegressionSet(map(
                "setName", "high-risk-regression",
                "sourceVersionId", versionId,
                "riskLevel", "high",
                "limit", 2
        ));
        Long regressionId = longVal(regression.get("id"));
        assertTrue(regressionId > 0L);

        Map<String, Object> runResult = service.runRegressionSet(regressionId, 2);
        Map<String, Object> run = asMap(runResult.get("run"));
        assertEquals("regression_set", run.get("sourceType"));
        assertEquals(regressionId, longVal(run.get("regressionSetId")));
    }

    @Test
    void releaseAndGrayFlowShouldPersistAndTransition() {
        service.createEvalCase(map(
                "caseName", "release-pass-case",
                "queryText", "release pass smoke query",
                "bucket", "golden",
                "riskLevel", "medium",
                "enabled", 1,
                "expectedTaskType", "product_search",
                "expectedPlanType", "runtime_fallback",
                "expectedAnswerType", "no_result"
        ));
        when(queryParserService.parse(anyString(), anyList())).thenReturn(parsedIntent(TaskType.PRODUCT_SEARCH));
        when(agentRuntime.run(any(), isNull())).thenReturn(runtimeState(AnswerType.NO_RESULT, false, "runtime_fallback", "fallback"));

        Map<String, Object> version = service.createEvalCaseVersion(map(
                "versionName", "release-version",
                "keyword", "release-pass-case",
                "bucket", "golden",
                "limit", 5
        ));
        Long versionId = longVal(version.get("id"));

        Map<String, Object> release = service.createReleaseRecord(map(
                "releaseName", "governance-release",
                "targetScope", "agent/governance",
                "evalCaseVersionId", versionId
        ));
        Long releaseId = longVal(release.get("id"));
        assertEquals("draft", release.get("releaseStatus"));

        Map<String, Object> gray = service.createGrayConfig(map(
                "configName", "gray-10",
                "queryBucket", "golden",
                "trafficPercent", 10,
                "riskLevel", "medium",
                "enabled", 1
        ));
        Long grayId = longVal(gray.get("id"));
        assertTrue(grayId > 0L);

        Map<String, Object> applied = service.applyGrayConfigToRelease(releaseId, grayId);
        assertTrue(String.valueOf(applied.get("grayStrategyJson")).contains("\"configId\":" + grayId));

        Map<String, Object> runResult = service.runReleaseEval(releaseId, 2, true);
        Map<String, Object> run = asMap(runResult.get("run"));
        assertTrue(longVal(run.get("id")) > 0L);
        assertEquals(1D, ((Number) run.get("passRate")).doubleValue(), 0.0001D);

        Map<String, Object> ready = service.transitionRelease(releaseId, "ready", null, 1D, 24);
        assertEquals("ready", ready.get("releaseStatus"));

        Map<String, Object> grayTransition = service.transitionRelease(releaseId, "gray", grayId, 1D, 24);
        assertEquals("gray", grayTransition.get("releaseStatus"));

        Map<String, Object> released = service.transitionRelease(releaseId, "released", null, 1D, 24);
        assertEquals("released", released.get("releaseStatus"));
        assertFalse(service.releaseEvents(releaseId).isEmpty());

        Map<String, Object> governanceSummary = service.releaseGovernanceSummary(releaseId, 1D, 24);
        assertEquals(releaseId, longVal(asMap(governanceSummary.get("release")).get("id")));
        assertNotNull(governanceSummary.get("verification"));
        assertNotNull(governanceSummary.get("preflight"));
        assertNotNull(governanceSummary.get("grayConfig"));
        assertNotNull(governanceSummary.get("evalCaseVersion"));
        assertNotNull(governanceSummary.get("latestRun"));
        assertFalse(asListOfStrings(governanceSummary.get("recommended_actions")).isEmpty());
    }

    @Test
    void importEvalCasesShouldSplitInsertAndUpdate() {
        Map<String, Object> created = service.createEvalCase(map(
                "caseName", "import-target",
                "queryText", "import old query",
                "bucket", "golden"
        ));
        Long existingId = longVal(created.get("id"));

        Map<String, Object> result = service.importEvalCases(map(
                "cases", Arrays.asList(
                        map("id", existingId, "caseName", "import-target-updated", "queryText", "import old query updated", "bucket", "golden"),
                        map("caseName", "import-new", "queryText", "import new query", "bucket", "regression")
                )
        ));

        assertEquals(2, result.get("total"));
        assertEquals(1, result.get("inserted"));
        assertEquals(1, result.get("updated"));
        assertEquals("import-target-updated", service.evalCases("import-target-updated", "", "", null, 1, 10).get(0).get("caseName"));
        assertEquals(1, service.evalCases("import-new", "", "", null, 1, 10).size());
    }

    @Test
    void releasePreflightShouldNotCreateEvalRunWhenOnlyReading() {
        service.createEvalCase(map(
                "caseName", "preflight-readonly-case",
                "queryText", "preflight readonly query",
                "bucket", "golden",
                "expectedTaskType", "product_search",
                "expectedPlanType", "runtime_fallback",
                "expectedAnswerType", "no_result"
        ));
        Map<String, Object> version = service.createEvalCaseVersion(map(
                "versionName", "preflight-readonly-version",
                "keyword", "preflight-readonly-case",
                "bucket", "golden",
                "limit", 5
        ));
        int before = service.evalRuns("", "", null, null, 1, 100).size();

        Map<String, Object> preflight = service.releasePreflight(10, 10, 0.2D, longVal(version.get("id")), null, 1D, 24);

        assertEquals(before, service.evalRuns("", "", null, null, 1, 100).size());
        assertEquals(Boolean.FALSE, preflight.get("ready"));
        verify(queryParserService, org.mockito.Mockito.never()).parse(anyString(), anyList());
        verify(agentRuntime, org.mockito.Mockito.never()).run(any(), isNull());
    }

    @Test
    void releasePreflightShouldDefaultToLatestLaunchReleaseScopeWhenIdsMissing() {
        service.createEvalCase(map(
                "caseName", "default-preflight-case",
                "queryText", "default preflight query",
                "bucket", "golden",
                "expectedTaskType", "product_search",
                "expectedPlanType", "runtime_fallback",
                "expectedAnswerType", "no_result"
        ));
        when(queryParserService.parse(anyString(), anyList())).thenReturn(parsedIntent(TaskType.PRODUCT_SEARCH));
        when(agentRuntime.run(any(), isNull())).thenReturn(runtimeState(AnswerType.NO_RESULT, false, "runtime_fallback", "fallback"));

        Map<String, Object> version = service.createEvalCaseVersion(map(
                "versionName", "default-preflight-version",
                "keyword", "default-preflight-case",
                "bucket", "golden",
                "limit", 5
        ));
        Long versionId = longVal(version.get("id"));

        Map<String, Object> release = service.createReleaseRecord(map(
                "releaseName", "default-preflight-release",
                "evalCaseVersionId", versionId
        ));
        Long releaseId = longVal(release.get("id"));
        service.runReleaseEval(releaseId, 5, true);
        service.transitionRelease(releaseId, "ready", null, 1D, 24);

        Map<String, Object> preflight = service.releasePreflight(10, 10, 0.2D, null, null, 1D, 24);
        Map<String, Object> overview = asMap(preflight.get("overview"));

        assertEquals(Boolean.TRUE, preflight.get("ready"));
        assertEquals("latest_release", overview.get("scopeSource"));
        assertEquals(releaseId, longVal(overview.get("scopeReleaseId")));
        assertEquals(versionId, longVal(overview.get("evalCaseVersionId")));
    }

    @Test
    void releaseTransitionShouldRejectBypassingStateMachineAndUpdateShouldNotMutateStatusDirectly() {
        Map<String, Object> release = service.createReleaseRecord(map(
                "releaseName", "state-machine-release",
                "targetScope", "agent/governance"
        ));
        Long releaseId = longVal(release.get("id"));

        IllegalArgumentException updateError = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateReleaseRecord(map("id", releaseId, "releaseStatus", "released"))
        );
        assertTrue(updateError.getMessage().contains("transition API"));

        IllegalArgumentException transitionError = assertThrows(
                IllegalArgumentException.class,
                () -> service.transitionRelease(releaseId, "released", null, 1D, 24)
        );
        assertTrue(transitionError.getMessage().contains("draft can only transition to ready"));
    }

    @Test
    void staleRunShouldFailVerificationAndPreflightFreshnessChecks() throws Exception {
        service.createEvalCase(map(
                "caseName", "stale-run-case",
                "queryText", "stale run query",
                "bucket", "golden",
                "expectedTaskType", "product_search",
                "expectedPlanType", "runtime_fallback",
                "expectedAnswerType", "no_result"
        ));
        when(queryParserService.parse(anyString(), anyList())).thenReturn(parsedIntent(TaskType.PRODUCT_SEARCH));
        when(agentRuntime.run(any(), isNull())).thenReturn(runtimeState(AnswerType.NO_RESULT, false, "runtime_fallback", "fallback"));

        Map<String, Object> version = service.createEvalCaseVersion(map(
                "versionName", "stale-run-version",
                "keyword", "stale-run-case",
                "bucket", "golden",
                "limit", 5
        ));
        Long versionId = longVal(version.get("id"));

        Map<String, Object> release = service.createReleaseRecord(map(
                "releaseName", "stale-run-release",
                "evalCaseVersionId", versionId
        ));
        Long releaseId = longVal(release.get("id"));

        Map<String, Object> runResult = service.runReleaseEval(releaseId, 5, false);
        Long runId = longVal(asMap(runResult.get("run")).get("id"));
        markRunCreatedAt(runId, "2000-01-01 00:00:00");

        Map<String, Object> verification = service.releaseVerification(releaseId, 1D, 24);
        assertEquals(Boolean.FALSE, verification.get("ready"));
        assertEquals(Boolean.FALSE, asMap(verification.get("checks")).get("latest_run_fresh"));
        assertTrue(asListOfStrings(verification.get("recommended_actions")).contains("Latest run is stale. Re-run release eval."));

        Map<String, Object> preflight = service.releasePreflight(10, 10, 0.2D, versionId, null, 1D, 24);
        assertEquals(Boolean.FALSE, preflight.get("ready"));
        assertEquals(Boolean.FALSE, asMap(preflight.get("checks")).get("run_fresh_ok"));
        assertTrue(asListOfStrings(preflight.get("recommended_actions")).contains("Latest regression run is stale."));
    }

    @Test
    void compareEvalCaseVersionsShouldTreatExpectationChangesAsChanged() {
        Map<String, Object> created = service.createEvalCase(map(
                "caseName", "version-change-case",
                "queryText", "same question for version compare",
                "bucket", "golden",
                "riskLevel", "medium",
                "expectedTaskType", "product_search",
                "expectedPlanType", "search_then_answer",
                "expectedAnswerType", "search_summary"
        ));
        Long caseId = longVal(created.get("id"));
        Map<String, Object> baseVersion = service.createEvalCaseVersion(map(
                "versionName", "version-base",
                "keyword", "version-change-case",
                "bucket", "golden",
                "limit", 5
        ));

        service.updateEvalCase(map(
                "id", caseId,
                "riskLevel", "high",
                "expectedPlanType", "knowledge_then_fallback",
                "expectedAnswerType", "text_answer"
        ));

        Map<String, Object> targetVersion = service.createEvalCaseVersion(map(
                "versionName", "version-target",
                "keyword", "version-change-case",
                "bucket", "golden",
                "limit", 5
        ));

        Map<String, Object> comparison = service.compareEvalCaseVersions(longVal(baseVersion.get("id")), longVal(targetVersion.get("id")));
        assertEquals(1, intVal(comparison.get("changedCount")));
        assertEquals(0, intVal(comparison.get("unchangedCount")));
    }

    @Test
    void compareEvalRunsShouldReportNewAndRemovedCases() {
        service.createEvalCase(map(
                "caseName", "run-compare-case-a",
                "queryText", "闂A",
                "bucket", "golden",
                "expectedTaskType", "product_search",
                "expectedPlanType", "runtime_fallback",
                "expectedAnswerType", "no_result"
        ));
        service.createEvalCase(map(
                "caseName", "run-compare-case-b",
                "queryText", "闂B",
                "bucket", "golden",
                "expectedTaskType", "product_search",
                "expectedPlanType", "runtime_fallback",
                "expectedAnswerType", "no_result"
        ));
        when(queryParserService.parse(anyString(), anyList())).thenReturn(parsedIntent(TaskType.PRODUCT_SEARCH));
        when(agentRuntime.run(any(), isNull())).thenReturn(runtimeState(AnswerType.NO_RESULT, false, "runtime_fallback", "fallback"));

        Map<String, Object> baseVersion = service.createEvalCaseVersion(map(
                "versionName", "run-base",
                "keyword", "run-compare-case-a",
                "bucket", "golden",
                "limit", 5
        ));
        Map<String, Object> targetVersion = service.createEvalCaseVersion(map(
                "versionName", "run-target",
                "keyword", "run-compare-case",
                "bucket", "golden",
                "limit", 5
        ));

        Map<String, Object> baseRun = service.runEvalCaseVersion(longVal(baseVersion.get("id")), 5);
        Map<String, Object> targetRun = service.runEvalCaseVersion(longVal(targetVersion.get("id")), 5);

        Map<String, Object> comparison = service.compareEvalRuns(longVal(asMap(baseRun.get("run")).get("id")), longVal(asMap(targetRun.get("run")).get("id")));
        assertEquals(1, intVal(comparison.get("newCaseCount")));
        assertEquals(0, intVal(comparison.get("removedCaseCount")));
    }

    @Test
    void updatingEvalCaseShouldKeepExistingSnapshotsStableUntilRebuilt() {
        Map<String, Object> created = service.createEvalCase(map(
                "caseName", "snapshot-refresh-case",
                "queryText", "snapshot refresh query",
                "bucket", "golden",
                "riskLevel", "medium",
                "expectedTaskType", "product_search",
                "expectedPlanType", "runtime_fallback",
                "expectedAnswerType", "no_result"
        ));
        Long caseId = longVal(created.get("id"));

        Map<String, Object> version = service.createEvalCaseVersion(map(
                "versionName", "snapshot-refresh-version",
                "keyword", "snapshot-refresh-case",
                "bucket", "golden",
                "limit", 5
        ));
        Long versionId = longVal(version.get("id"));

        Map<String, Object> regression = service.createRegressionSet(map(
                "setName", "snapshot-refresh-regression",
                "sourceVersionId", versionId,
                "riskLevel", "medium",
                "limit", 5
        ));
        Long regressionId = longVal(regression.get("id"));

        service.updateEvalCase(map(
                "id", caseId,
                "expectedPlanType", "single_tool"
        ));

        when(queryParserService.parse(anyString(), anyList())).thenReturn(parsedIntent(TaskType.PRODUCT_SEARCH));
        when(agentRuntime.run(any(), isNull())).thenReturn(runtimeState(AnswerType.NO_RESULT, false, "single_tool", "ok"));

        Map<String, Object> versionRun = service.runEvalCaseVersion(versionId, 5);
        Map<String, Object> regressionRun = service.runRegressionSet(regressionId, 5);
        Map<String, Object> originalVersionDetail = service.evalVersionDetail(versionId);
        Map<String, Object> originalRegressionDetail = service.regressionSetDetail(regressionId);

        assertEquals(0D, ((Number) asMap(versionRun.get("run")).get("passRate")).doubleValue(), 0.0001D);
        assertEquals(0D, ((Number) asMap(regressionRun.get("run")).get("passRate")).doubleValue(), 0.0001D);
        assertEquals("runtime_fallback", asMap(asList(originalVersionDetail.get("items")).get(0)).get("expectedPlanType"));
        assertEquals("runtime_fallback", asMap(asList(originalRegressionDetail.get("items")).get(0)).get("expectedPlanType"));

        Map<String, Object> rebuiltVersion = service.createEvalCaseVersion(map(
                "versionName", "snapshot-refresh-version-rebuilt",
                "keyword", "snapshot-refresh-case",
                "bucket", "golden",
                "limit", 5
        ));
        Long rebuiltVersionId = longVal(rebuiltVersion.get("id"));
        Map<String, Object> rebuiltRegression = service.createRegressionSet(map(
                "setName", "snapshot-refresh-regression-rebuilt",
                "sourceVersionId", rebuiltVersionId,
                "riskLevel", "medium",
                "limit", 5
        ));
        Long rebuiltRegressionId = longVal(rebuiltRegression.get("id"));

        Map<String, Object> rebuiltVersionRun = service.runEvalCaseVersion(rebuiltVersionId, 5);
        Map<String, Object> rebuiltRegressionRun = service.runRegressionSet(rebuiltRegressionId, 5);

        assertEquals(1D, ((Number) asMap(rebuiltVersionRun.get("run")).get("passRate")).doubleValue(), 0.0001D);
        assertEquals(1D, ((Number) asMap(rebuiltRegressionRun.get("run")).get("passRate")).doubleValue(), 0.0001D);
        assertEquals("single_tool", asMap(asList(rebuiltVersionRun.get("results")).get(0)).get("actualPlanType"));
        assertEquals("single_tool", asMap(asList(rebuiltRegressionRun.get("results")).get(0)).get("actualPlanType"));
    }

    private Map<String, Object> map(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.<String, Object>emptyMap();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object value) {
        return value instanceof List ? (List<Map<String, Object>>) value : Collections.<Map<String, Object>>emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<String> asListOfStrings(Object value) {
        return value instanceof List ? (List<String>) value : Collections.<String>emptyList();
    }

    private long longVal(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(String.valueOf(value));
    }

    private int intVal(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(String.valueOf(value));
    }

    private ParsedIntent parsedIntent(TaskType taskType) {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(taskType);
        return parsedIntent;
    }

    private SessionState runtimeState(AnswerType answerType, boolean degraded, String completedNode, String reply) {
        SessionState state = new SessionState();
        state.getExecutionMeta().setDegraded(degraded);
        state.getExecutionMeta().getCompletedNodes().add(completedNode);
        FinalAnswer finalAnswer = new FinalAnswer();
        finalAnswer.setAnswerType(answerType);
        finalAnswer.setAnswerText(reply);
        state.setFinalAnswer(finalAnswer);
        return state;
    }

    @SuppressWarnings("unchecked")
    private void markRunCreatedAt(Long runId, String createdAt) throws Exception {
        Field evalRunListField = AdminGovernanceService.class.getDeclaredField("evalRunList");
        evalRunListField.setAccessible(true);
        List<Map<String, Object>> evalRunList = (List<Map<String, Object>>) evalRunListField.get(service);
        for (Map<String, Object> run : evalRunList) {
            if (longVal(run.get("id")) == runId.longValue()) {
                run.put("createdAt", createdAt);
            }
        }
        Field evalRunDetailMapField = AdminGovernanceService.class.getDeclaredField("evalRunDetailMap");
        evalRunDetailMapField.setAccessible(true);
        Map<Long, Map<String, Object>> evalRunDetailMap = (Map<Long, Map<String, Object>>) evalRunDetailMapField.get(service);
        Map<String, Object> detail = evalRunDetailMap.get(runId);
        if (detail != null) {
            asMap(detail.get("run")).put("createdAt", createdAt);
        }
    }
}

