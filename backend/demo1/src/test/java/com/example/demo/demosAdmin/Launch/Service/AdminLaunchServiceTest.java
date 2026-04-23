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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class AdminLaunchServiceTest {

    @Mock
    private SyncAdminService syncAdminService;

    @Mock
    private AdminGovernanceService adminGovernanceService;

    @Mock
    private RealtimeQueryOrchestratorService realtimeQueryOrchestratorService;

    @Mock
    private InternalRealtimeGatewayService internalRealtimeGatewayService;

    @Mock
    private LaunchAdminStateStore launchAdminStateStore;

    @Mock
    private RealtimeQueryProperties realtimeQueryProperties;

    private AdminLaunchService service;

    @BeforeEach
    void setUp() {
        service = new AdminLaunchService(
                syncAdminService,
                adminGovernanceService,
                internalRealtimeGatewayService,
                realtimeQueryOrchestratorService,
                realtimeQueryProperties,
                launchAdminStateStore
        );
    }

    @Test
    void readinessOverviewShouldAggregateSubsystemReadiness() {
        mockHealthyInternalGateway();
        when(syncAdminService.baselineReport()).thenReturn(map(
                "w12Ready", true,
                "products", map("indexValidation", map("consistent", true)),
                "tasks", map("failed", 0, "deadLetterTotal", 0),
                "repairRecommendations", Collections.emptyList()
        ));
        when(syncAdminService.repairStrategyReport()).thenReturn(map("recommendations", Collections.emptyList()));
        when(syncAdminService.listProductRollbackCandidates()).thenReturn(Collections.singletonList(map(
                "index", "product_search_v2",
                "exists", true,
                "documentCount", 12L
        )));
        when(adminGovernanceService.dashboard(7, 8, 8)).thenReturn(map("overview", map("stage", "W13", "replay_total", 10)));
        when(adminGovernanceService.releasePreflight(10, 10, 0.2D, null, null, 1D, 24)).thenReturn(map(
                "ready", true,
                "recommended_actions", Collections.singletonList("Preflight looks good.")
        ));
        when(realtimeQueryOrchestratorService.status()).thenReturn(map(
                "enabled", true,
                "gatewayEnabled", true,
                "mockGatewayEnabled", false,
                "circuit", map("state", "CLOSED")
        ));

        Map<String, Object> overview = service.readinessOverview(10, 10, 0.2D, 1D, 24);

        Map<String, Object> gates = asMap(overview.get("gates"));
        assertEquals("W14", overview.get("stage"));
        assertEquals(Boolean.TRUE, overview.get("overallReady"));
        assertEquals(Boolean.TRUE, gates.get("syncReady"));
        assertEquals(Boolean.TRUE, gates.get("governanceReady"));
        assertEquals(Boolean.TRUE, gates.get("realtimeLiveGatewayReady"));
        assertEquals(Boolean.TRUE, gates.get("rollbackReady"));
        assertFalse(asList(overview.get("recommendedActions")).isEmpty());
    }

    @Test
    void readinessOverviewShouldTreatInternalGatewayAsLaunchReadyWhenProductProviderIsHealthy() {
        when(internalRealtimeGatewayService.health()).thenReturn(map(
                "status", "UP",
                "gatewayMode", "internal",
                "productProviderReady", true
        ));
        when(syncAdminService.baselineReport()).thenReturn(map(
                "w12Ready", true,
                "products", map("indexValidation", map("consistent", true)),
                "tasks", map("failed", 0, "deadLetterTotal", 0),
                "repairRecommendations", Collections.emptyList()
        ));
        when(syncAdminService.repairStrategyReport()).thenReturn(map("recommendations", Collections.emptyList()));
        when(syncAdminService.listProductRollbackCandidates()).thenReturn(Collections.singletonList(map(
                "index", "product_search_v1",
                "exists", true,
                "documentCount", 12L
        )));
        when(adminGovernanceService.dashboard(7, 8, 8)).thenReturn(map("overview", map("stage", "W13")));
        when(adminGovernanceService.releasePreflight(10, 10, 0.2D, null, null, 1D, 24)).thenReturn(map(
                "ready", true,
                "recommended_actions", Collections.singletonList("Preflight looks good.")
        ));
        when(realtimeQueryOrchestratorService.status()).thenReturn(map(
                "enabled", true,
                "gatewayEnabled", false,
                "mockGatewayEnabled", false,
                "circuit", map("state", "CLOSED")
        ));

        Map<String, Object> overview = service.readinessOverview(10, 10, 0.2D, 1D, 24);

        Map<String, Object> gates = asMap(overview.get("gates"));
        assertEquals(Boolean.TRUE, overview.get("overallReady"));
        assertEquals(Boolean.TRUE, gates.get("realtimeLiveGatewayReady"));
        assertEquals(Boolean.TRUE, asMap(overview.get("realtime")).get("launchPathReady"));
    }

    @Test
    void readinessOverviewShouldDegradeInsteadOfThrowingWhenSyncFails() {
        mockHealthyInternalGateway();
        doThrow(new IllegalStateException("es unavailable")).when(syncAdminService).baselineReport();
        when(syncAdminService.repairStrategyReport()).thenReturn(map("recommendations", Collections.emptyList()));
        when(adminGovernanceService.dashboard(7, 8, 8)).thenReturn(map("overview", map("stage", "W13")));
        when(adminGovernanceService.releasePreflight(10, 10, 0.2D, null, null, 1D, 24)).thenReturn(map(
                "ready", true,
                "recommended_actions", Collections.emptyList()
        ));
        when(realtimeQueryOrchestratorService.status()).thenReturn(map(
                "enabled", true,
                "gatewayEnabled", true,
                "mockGatewayEnabled", false,
                "circuit", map("state", "CLOSED")
        ));

        Map<String, Object> overview = service.readinessOverview(10, 10, 0.2D, 1D, 24);

        assertEquals(Boolean.FALSE, overview.get("overallReady"));
        assertEquals(Boolean.FALSE, asMap(overview.get("gates")).get("syncReady"));
        assertFalse(asList(overview.get("recommendedActions")).isEmpty());
    }

    @Test
    void checklistShouldExposeFailedItemsWhenSubsystemsAreNotReady() {
        when(internalRealtimeGatewayService.health()).thenReturn(map(
                "status", "DOWN",
                "productProviderReady", false
        ));
        when(syncAdminService.baselineReport()).thenReturn(map(
                "w12Ready", false,
                "products", map("indexValidation", map("consistent", false)),
                "tasks", map("failed", 2, "deadLetterTotal", 1),
                "repairRecommendations", Collections.singletonList("Repair sync tasks.")
        ));
        when(syncAdminService.repairStrategyReport()).thenReturn(map("recommendations", Collections.singletonList("Repair sync tasks.")));
        when(syncAdminService.listProductRollbackCandidates()).thenReturn(Collections.emptyList());
        when(adminGovernanceService.dashboard(7, 8, 8)).thenReturn(map("overview", map("stage", "W13")));
        when(adminGovernanceService.releasePreflight(10, 10, 0.2D, null, null, 1D, 24)).thenReturn(map(
                "ready", false,
                "recommended_actions", Collections.singletonList("Run regression.")
        ));
        when(realtimeQueryOrchestratorService.status()).thenReturn(map(
                "enabled", false,
                "gatewayEnabled", false,
                "mockGatewayEnabled", false,
                "circuit", map("state", "OPEN")
        ));

        List<Map<String, Object>> checklist = service.checklist(0.2D, 1D, 24);

        assertFalse(checklist.isEmpty());
        assertEquals(Boolean.FALSE, findItem(checklist, "sync-baseline-clean").get("ready"));
        assertEquals(Boolean.FALSE, findItem(checklist, "governance-preflight-ready").get("ready"));
        assertEquals(Boolean.FALSE, findItem(checklist, "realtime-live-gateway").get("ready"));
        assertEquals(Boolean.FALSE, findItem(checklist, "rollback-candidate-ready").get("ready"));
    }

    @Test
    void smokeRunShouldCombineSubsystemSmokeSignals() {
        when(syncAdminService.baselineReport()).thenReturn(map("w12Ready", true));
        when(adminGovernanceService.releasePreflight(5, 6, 0.1D, null, null, 0.9D, 12)).thenReturn(map("ready", true));
        when(realtimeQueryOrchestratorService.status()).thenReturn(map(
                "enabled", true,
                "gatewayEnabled", true,
                "mockGatewayEnabled", false,
                "circuit", map("state", "CLOSED")
        ));
        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.SUCCESS);
        response.setItems(Collections.singletonList(item("1", false, "internal_gateway")));
        response.setQueryMeta(new LinkedHashMap<String, Object>());
        when(internalRealtimeGatewayService.query(any())).thenReturn(response);

        Map<String, Object> result = service.smokeRun(map(
                "detailLimit", 5,
                "taskLimit", 6,
                "maxDegradedRate", 0.1D,
                "minEvalPassRate", 0.9D,
                "maxRunAgeHours", 12
        ), 5, 6, 0.1D, 0.9D, 12);

        assertEquals(Boolean.TRUE, result.get("overallSuccess"));
        verify(internalRealtimeGatewayService).query(any());
    }

    @Test
    void forceRealtimeFallbackShouldOpenCircuitAndRunDrill() {
        when(realtimeQueryOrchestratorService.forceOpenCircuit(45, "manual_test")).thenReturn(map(
                "state", "OPEN",
                "forced", true,
                "durationSeconds", 45
        ));
        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.DEGRADED);
        response.setItems(Collections.singletonList(item("11", true, "snapshot_fallback")));
        response.setQueryMeta(new LinkedHashMap<String, Object>());
        when(internalRealtimeGatewayService.query(any())).thenReturn(response);

        Map<String, Object> result = service.forceRealtimeFallback(map(
                "durationSeconds", 45,
                "reason", "manual_test",
                "realtimeRequest", map(
                        "entityIds", Arrays.asList(11L, 12L),
                        "queryType", "availability"
                )
        ));

        assertEquals(Boolean.TRUE, result.get("fallbackTriggered"));
        Map<String, Object> drill = asMap(result.get("drill"));
        assertEquals("degraded", drill.get("realtimeStatus"));
        ArgumentCaptor<RealtimeQueryRequest> captor = ArgumentCaptor.forClass(RealtimeQueryRequest.class);
        verify(internalRealtimeGatewayService).query(captor.capture());
        assertTrue(captor.getValue().isForceRefresh());
        assertEquals(Arrays.asList(11L, 12L), captor.getValue().getEntityIds());
    }

    @Test
    void recordLoadTestShouldPersistNormalizedRecord() {
        when(launchAdminStateStore.saveRecord(org.mockito.ArgumentMatchers.eq("load_test"), org.mockito.ArgumentMatchers.eq("load-test"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));

        Map<String, Object> result = service.recordLoadTest(map(
                "name", "smoke-load",
                "environment", "staging",
                "operator", "qa",
                "passed", true,
                "qps", 50,
                "p95Ms", 1200
        ));

        assertEquals("smoke-load", result.get("name"));
        assertEquals("staging", result.get("environment"));
        assertEquals(Boolean.TRUE, result.get("passed"));
        assertEquals("passed", result.get("status"));
    }

    @Test
    void runLoadTestShouldExecuteGatewayQueriesAndPersistMetrics() {
        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.SUCCESS);
        response.setItems(Collections.singletonList(item("1", false, "internal_gateway")));
        response.setQueryMeta(new LinkedHashMap<String, Object>());
        when(internalRealtimeGatewayService.query(any())).thenReturn(response);
        when(launchAdminStateStore.saveRecord(org.mockito.ArgumentMatchers.eq("load_test"), org.mockito.ArgumentMatchers.eq("load-test"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));

        Map<String, Object> result = service.runLoadTest(map(
                "name", "executed-load",
                "iterations", 2,
                "concurrency", 2,
                "realtimeRequest", map(
                        "entityIds", Arrays.asList(1L),
                        "queryType", "availability"
                )
        ));

        assertEquals("executed-load", result.get("name"));
        assertEquals("executed", result.get("mode"));
        assertEquals(Boolean.TRUE, result.get("passed"));
        assertEquals(4, intValue(asMap(result.get("metrics")).get("totalRequests")));
        verify(internalRealtimeGatewayService, org.mockito.Mockito.times(4)).query(any());
    }

    @Test
    void createChecklistSnapshotShouldPersistReadinessAndChecklist() {
        mockHealthyInternalGateway();
        when(syncAdminService.baselineReport()).thenReturn(map(
                "w12Ready", true,
                "products", map("indexValidation", map("consistent", true)),
                "tasks", map("failed", 0, "deadLetterTotal", 0),
                "repairRecommendations", Collections.emptyList()
        ));
        when(syncAdminService.repairStrategyReport()).thenReturn(map("recommendations", Collections.emptyList()));
        when(syncAdminService.listProductRollbackCandidates()).thenReturn(Collections.singletonList(map(
                "index", "product_search_v2",
                "exists", true,
                "documentCount", 10L
        )));
        when(adminGovernanceService.dashboard(7, 8, 8)).thenReturn(map("overview", map("stage", "W13")));
        when(adminGovernanceService.releasePreflight(10, 10, 0.2D, null, null, 1D, 24)).thenReturn(map(
                "ready", true,
                "recommended_actions", Collections.singletonList("Ready")
        ));
        when(realtimeQueryOrchestratorService.status()).thenReturn(map(
                "enabled", true,
                "gatewayEnabled", true,
                "mockGatewayEnabled", false,
                "circuit", map("state", "CLOSED")
        ));
        when(launchAdminStateStore.saveRecord(org.mockito.ArgumentMatchers.eq("checklist_snapshot"), org.mockito.ArgumentMatchers.eq("checklist"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));

        Map<String, Object> result = service.createChecklistSnapshot(map("name", "go-live-check"), 0.2D, 1D, 24);

        assertEquals("go-live-check", result.get("name"));
        assertEquals(Boolean.TRUE, result.get("passed"));
        assertTrue(result.containsKey("readiness"));
        assertTrue(result.containsKey("checklist"));
    }

    @Test
    void recordSignoffShouldPersistNormalizedRecord() {
        when(launchAdminStateStore.saveRecord(org.mockito.ArgumentMatchers.eq("signoff"), org.mockito.ArgumentMatchers.eq("signoff"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));

        Map<String, Object> result = service.recordSignoff(map(
                "signoffRole", "qa",
                "operator", "tester",
                "approved", true,
                "notes", "ready to launch"
        ));

        assertEquals("qa", result.get("signoffRole"));
        assertEquals("tester", result.get("operator"));
        assertEquals(Boolean.TRUE, result.get("approved"));
        assertEquals("approved", result.get("status"));
    }

    @Test
    void recordDependencyCheckShouldPersistNormalizedRecord() {
        when(launchAdminStateStore.saveRecord(org.mockito.ArgumentMatchers.eq("dependency_check"), org.mockito.ArgumentMatchers.eq("dependency"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));

        Map<String, Object> result = service.recordDependencyCheck(map(
                "dependencyName", "realtime-gateway",
                "operator", "ops",
                "ready", false,
                "notes", "timeout"
        ));

        assertEquals("realtime-gateway", result.get("dependencyName"));
        assertEquals("ops", result.get("operator"));
        assertEquals(Boolean.FALSE, result.get("ready"));
        assertEquals("blocked", result.get("status"));
    }

    @Test
    void runDrillShouldExecuteRealtimeFallbackAndPersistResult() {
        when(realtimeQueryOrchestratorService.forceOpenCircuit(30, "launch_drill")).thenReturn(map(
                "state", "OPEN",
                "forced", true,
                "durationSeconds", 30
        ));
        when(realtimeQueryOrchestratorService.resetCircuit()).thenReturn(map(
                "state", "CLOSED"
        ));
        when(realtimeQueryOrchestratorService.status()).thenReturn(map(
                "enabled", true,
                "gatewayEnabled", false,
                "mockGatewayEnabled", false,
                "circuit", map("state", "CLOSED")
        ));
        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.DEGRADED);
        response.setItems(Collections.singletonList(item("1", true, "snapshot_fallback")));
        response.setQueryMeta(new LinkedHashMap<String, Object>());
        when(internalRealtimeGatewayService.query(any())).thenReturn(response);
        when(launchAdminStateStore.saveRecord(org.mockito.ArgumentMatchers.eq("drill"), org.mockito.ArgumentMatchers.eq("drill"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));

        Map<String, Object> result = service.runDrill(map(
                "name", "fallback-drill",
                "drillType", "realtime_fallback",
                "reason", "launch_drill",
                "durationSeconds", 30,
                "realtimeRequest", map(
                        "entityIds", Arrays.asList(1L),
                        "queryType", "availability"
                )
        ), 10, 10, 0.2D, 1D, 24);

        assertEquals("fallback-drill", result.get("name"));
        assertEquals(Boolean.TRUE, result.get("passed"));
        assertEquals(Boolean.TRUE, result.get("recovered"));
        assertEquals("executed", result.get("mode"));
    }

    @Test
    void gatewayConfigSnapshotShouldExposeRealtimeProperties() {
        mockHealthyInternalGateway();
        when(realtimeQueryProperties.isEnabled()).thenReturn(true);
        when(realtimeQueryProperties.isGatewayEnabled()).thenReturn(true);
        when(realtimeQueryProperties.isMockGatewayEnabled()).thenReturn(false);
        when(realtimeQueryProperties.getGatewayBaseUrl()).thenReturn("http://gateway.internal");
        when(realtimeQueryProperties.getGatewayQueryPath()).thenReturn("/query");
        when(realtimeQueryProperties.getGatewayConnectTimeoutMs()).thenReturn(250);
        when(realtimeQueryProperties.getGatewayReadTimeoutMs()).thenReturn(800);
        when(realtimeQueryProperties.getGatewayRetryCount()).thenReturn(2);
        when(realtimeQueryProperties.getCacheTtlSeconds()).thenReturn(15);
        when(realtimeQueryProperties.getCircuitFailureThreshold()).thenReturn(3);
        when(realtimeQueryProperties.getCircuitOpenSeconds()).thenReturn(30);

        Map<String, Object> result = service.gatewayConfigSnapshot();

        assertEquals("internal", result.get("gatewayMode"));
        assertEquals(Boolean.TRUE, result.get("gatewayEnabled"));
        assertEquals("http://gateway.internal", result.get("gatewayBaseUrl"));
        assertEquals(2, intValue(result.get("gatewayRetryCount")));
    }

    @Test
    void createAndCloseLaunchWindowShouldPersistLifecycle() {
        when(launchAdminStateStore.saveRecord(org.mockito.ArgumentMatchers.eq("launch_window"), org.mockito.ArgumentMatchers.eq("window"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(launchAdminStateStore.listRecords("launch_window", 200)).thenReturn(Collections.singletonList(map(
                "id", "window-1",
                "windowName", "go-live",
                "status", "open",
                "startAt", "2026-04-05 22:30:00"
        )));

        Map<String, Object> created = service.createLaunchWindow(map(
                "windowName", "go-live",
                "operator", "release-manager"
        ));
        Map<String, Object> closed = service.closeLaunchWindow(map(
                "id", "window-1",
                "status", "closed"
        ));

        assertEquals("go-live", created.get("windowName"));
        assertEquals("open", created.get("status"));
        assertEquals("closed", closed.get("status"));
    }

    @Test
    void finalSummaryShouldRequireFreshPassingRecords() {
        mockHealthyInternalGateway();
        String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        when(syncAdminService.baselineReport()).thenReturn(map(
                "w12Ready", true,
                "products", map("indexValidation", map("consistent", true)),
                "tasks", map("failed", 0, "deadLetterTotal", 0),
                "repairRecommendations", Collections.emptyList()
        ));
        when(syncAdminService.repairStrategyReport()).thenReturn(map("recommendations", Collections.emptyList()));
        when(syncAdminService.listProductRollbackCandidates()).thenReturn(Collections.singletonList(map(
                "index", "product_search_v2",
                "exists", true,
                "documentCount", 10L
        )));
        when(adminGovernanceService.dashboard(7, 8, 8)).thenReturn(map("overview", map("stage", "W13")));
        when(adminGovernanceService.releasePreflight(10, 10, 0.2D, null, null, 1D, 24)).thenReturn(map(
                "ready", true,
                "recommended_actions", Collections.emptyList()
        ));
        when(realtimeQueryOrchestratorService.status()).thenReturn(map(
                "enabled", true,
                "gatewayEnabled", true,
                "mockGatewayEnabled", false,
                "circuit", map("state", "CLOSED")
        ));
        when(launchAdminStateStore.latestRecord("load_test")).thenReturn(map(
                "id", "load-1",
                "passed", true,
                "updatedAt", now
        ));
        when(launchAdminStateStore.latestRecord("drill")).thenReturn(map(
                "id", "drill-1",
                "passed", true,
                "updatedAt", now
        ));
        when(launchAdminStateStore.latestRecord("checklist_snapshot")).thenReturn(map(
                "id", "checklist-1",
                "passed", false,
                "updatedAt", now
        ));

        Map<String, Object> result = service.finalSummary(10, 10, 0.2D, 1D, 24, 72, 72, 24);

        Map<String, Object> gates = asMap(result.get("gates"));
        assertEquals(Boolean.FALSE, result.get("finalReady"));
        assertEquals(Boolean.TRUE, gates.get("loadTestReady"));
        assertEquals(Boolean.TRUE, gates.get("drillReady"));
        assertEquals(Boolean.FALSE, gates.get("checklistReady"));
    }

    @Test
    void runbookBundleShouldExposeLaunchDocs() {
        mockHealthyInternalGateway();
        when(syncAdminService.baselineReport()).thenReturn(map(
                "w12Ready", true,
                "products", map("indexValidation", map("consistent", true)),
                "tasks", map("failed", 0, "deadLetterTotal", 0),
                "repairRecommendations", Collections.emptyList()
        ));
        when(syncAdminService.repairStrategyReport()).thenReturn(map("recommendations", Collections.emptyList()));
        when(syncAdminService.listProductRollbackCandidates()).thenReturn(Collections.singletonList(map(
                "index", "product_search_v2",
                "exists", true,
                "documentCount", 10L
        )));
        when(adminGovernanceService.dashboard(7, 8, 8)).thenReturn(map("overview", map("stage", "W13")));
        when(adminGovernanceService.releasePreflight(10, 10, 0.2D, null, null, 1D, 24)).thenReturn(map(
                "ready", true,
                "recommended_actions", Collections.emptyList()
        ));
        when(realtimeQueryOrchestratorService.status()).thenReturn(map(
                "enabled", true,
                "gatewayEnabled", true,
                "mockGatewayEnabled", false,
                "circuit", map("state", "CLOSED")
        ));
        when(launchAdminStateStore.latestRecord("load_test")).thenReturn(Collections.<String, Object>emptyMap());
        when(launchAdminStateStore.latestRecord("drill")).thenReturn(Collections.<String, Object>emptyMap());
        when(launchAdminStateStore.latestRecord("checklist_snapshot")).thenReturn(Collections.<String, Object>emptyMap());

        Map<String, Object> result = service.runbookBundle(10, 10, 0.2D, 1D, 24);

        Map<String, Object> documents = asMap(result.get("documents"));
        assertTrue(documents.containsKey("opsRunbook"));
        assertTrue(documents.containsKey("rollbackPlaybook"));
        assertTrue(documents.containsKey("goLiveChecklist"));
    }

    @Test
    void handoffSummaryShouldExposeLatestArtifacts() {
        mockHealthyInternalGateway();
        when(syncAdminService.baselineReport()).thenReturn(map(
                "w12Ready", true,
                "products", map("indexValidation", map("consistent", true)),
                "tasks", map("failed", 0, "deadLetterTotal", 0),
                "repairRecommendations", Collections.emptyList()
        ));
        when(syncAdminService.repairStrategyReport()).thenReturn(map("recommendations", Collections.emptyList()));
        when(syncAdminService.listProductRollbackCandidates()).thenReturn(Collections.singletonList(map(
                "index", "product_search_v2",
                "exists", true,
                "documentCount", 10L
        )));
        when(adminGovernanceService.dashboard(7, 8, 8)).thenReturn(map("overview", map("stage", "W13")));
        when(adminGovernanceService.releasePreflight(10, 10, 0.2D, null, null, 1D, 24)).thenReturn(map(
                "ready", true,
                "recommended_actions", Collections.singletonList("Ready")
        ));
        when(realtimeQueryOrchestratorService.status()).thenReturn(map(
                "enabled", true,
                "gatewayEnabled", true,
                "mockGatewayEnabled", false,
                "circuit", map("state", "CLOSED")
        ));
        String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        when(launchAdminStateStore.latestRecord("load_test")).thenReturn(map("id", "load-1", "passed", true, "updatedAt", now));
        when(launchAdminStateStore.latestRecord("drill")).thenReturn(map("id", "drill-1", "passed", true, "updatedAt", now));
        when(launchAdminStateStore.latestRecord("checklist_snapshot")).thenReturn(map("id", "checklist-1", "passed", true, "updatedAt", now));
        when(launchAdminStateStore.listRecords("signoff", 10)).thenReturn(Collections.singletonList(map(
                "id", "signoff-1",
                "signoffRole", "qa",
                "approved", true,
                "updatedAt", now
        )));
        when(launchAdminStateStore.listRecords("dependency_check", 20)).thenReturn(Collections.singletonList(map(
                "id", "dep-1",
                "dependencyName", "realtime-gateway",
                "ready", true,
                "updatedAt", now
        )));
        when(launchAdminStateStore.listRecords("launch_window", 20)).thenReturn(Collections.singletonList(map(
                "id", "window-1",
                "windowName", "go-live",
                "status", "open",
                "updatedAt", now
        )));

        Map<String, Object> result = service.handoffSummary(10, 10, 0.2D, 1D, 24);

        assertEquals(Boolean.TRUE, result.get("finalReady"));
        Map<String, Object> latestArtifacts = asMap(result.get("latestArtifacts"));
        assertEquals("load-1", asMap(latestArtifacts.get("loadTest")).get("id"));
        assertEquals(1, intValue(asMap(result.get("signoffOverview")).get("approvedTotal")));
        assertEquals(1, intValue(asMap(result.get("dependencyOverview")).get("readyTotal")));
        assertEquals("window-1", asMap(result.get("activeLaunchWindow")).get("id"));
        assertTrue(asMap(result.get("documents")).containsKey("opsRunbook"));
    }

    @Test
    void handoffSummaryShouldCountLatestSignoffAndDependencyStateOnly() {
        mockHealthyInternalGateway();
        when(syncAdminService.baselineReport()).thenReturn(map(
                "w12Ready", true,
                "products", map("indexValidation", map("consistent", true)),
                "tasks", map("failed", 0, "deadLetterTotal", 0),
                "repairRecommendations", Collections.emptyList()
        ));
        when(syncAdminService.repairStrategyReport()).thenReturn(map("recommendations", Collections.emptyList()));
        when(syncAdminService.listProductRollbackCandidates()).thenReturn(Collections.singletonList(map(
                "index", "product_search_v2",
                "exists", true,
                "documentCount", 10L
        )));
        when(adminGovernanceService.dashboard(7, 8, 8)).thenReturn(map("overview", map("stage", "W13")));
        when(adminGovernanceService.releasePreflight(10, 10, 0.2D, null, null, 1D, 24)).thenReturn(map(
                "ready", true,
                "recommended_actions", Collections.singletonList("Ready")
        ));
        when(realtimeQueryOrchestratorService.status()).thenReturn(map(
                "enabled", true,
                "gatewayEnabled", true,
                "mockGatewayEnabled", false,
                "circuit", map("state", "CLOSED")
        ));
        String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        when(launchAdminStateStore.latestRecord("load_test")).thenReturn(map("id", "load-1", "passed", true, "updatedAt", now));
        when(launchAdminStateStore.latestRecord("drill")).thenReturn(map("id", "drill-1", "passed", true, "updatedAt", now));
        when(launchAdminStateStore.latestRecord("checklist_snapshot")).thenReturn(map("id", "checklist-1", "passed", true, "updatedAt", now));
        when(launchAdminStateStore.listRecords("signoff", 10)).thenReturn(Arrays.asList(
                map("id", "signoff-2", "signoffRole", "qa", "approved", true, "updatedAt", now),
                map("id", "signoff-1", "signoffRole", "qa", "approved", false, "updatedAt", "2026-04-05 10:00:00")
        ));
        when(launchAdminStateStore.listRecords("dependency_check", 20)).thenReturn(Arrays.asList(
                map("id", "dep-2", "dependencyName", "realtime-gateway", "ready", true, "updatedAt", now),
                map("id", "dep-1", "dependencyName", "realtime-gateway", "ready", false, "updatedAt", "2026-04-05 10:00:00")
        ));
        when(launchAdminStateStore.listRecords("launch_window", 20)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.handoffSummary(10, 10, 0.2D, 1D, 24);

        Map<String, Object> signoffOverview = asMap(result.get("signoffOverview"));
        Map<String, Object> dependencyOverview = asMap(result.get("dependencyOverview"));
        assertEquals(1, intValue(signoffOverview.get("total")));
        assertEquals(2, intValue(signoffOverview.get("recordTotal")));
        assertEquals(1, intValue(signoffOverview.get("approvedTotal")));
        assertEquals(0, intValue(signoffOverview.get("rejectedTotal")));
        assertEquals(1, intValue(dependencyOverview.get("total")));
        assertEquals(2, intValue(dependencyOverview.get("recordTotal")));
        assertEquals(1, intValue(dependencyOverview.get("readyTotal")));
        assertEquals(0, intValue(dependencyOverview.get("blockedTotal")));
    }

    @Test
    void timelineShouldMergeLaunchAndGovernanceEvents() {
        when(launchAdminStateStore.listRecords("load_test", 5)).thenReturn(Collections.singletonList(map(
                "id", "load-1",
                "name", "launch-load-test",
                "summary", "load ok",
                "status", "passed",
                "updatedAt", "2026-04-05 22:00:00"
        )));
        when(launchAdminStateStore.listRecords("drill", 5)).thenReturn(Collections.singletonList(map(
                "id", "drill-1",
                "name", "gateway-drill",
                "passed", true,
                "updatedAt", "2026-04-05 21:59:00"
        )));
        when(launchAdminStateStore.listRecords("checklist_snapshot", 5)).thenReturn(Collections.emptyList());
        when(adminGovernanceService.releaseRecords("", 1, 5)).thenReturn(Collections.singletonList(map(
                "id", 301L,
                "releaseName", "release-301",
                "releaseStatus", "gray",
                "updatedAt", "2026-04-05 21:58:00"
        )));
        when(adminGovernanceService.evalRuns("", "", null, null, 1, 5)).thenReturn(Collections.singletonList(map(
                "id", 101L,
                "passRate", 0.95D,
                "failedTotal", 1,
                "createdAt", "2026-04-05 21:57:00"
        )));

        List<Map<String, Object>> result = service.timeline(5);

        assertEquals(4, result.size());
        assertEquals("load_test", result.get(0).get("type"));
        assertEquals("release_record", result.get(2).get("type"));
        assertEquals("eval_run", result.get(3).get("type"));
    }

    @Test
    void launchPackageShouldExposeTimelineAndRunbook() {
        mockHealthyInternalGateway();
        when(syncAdminService.baselineReport()).thenReturn(map(
                "w12Ready", true,
                "products", map("indexValidation", map("consistent", true)),
                "tasks", map("failed", 0, "deadLetterTotal", 0),
                "repairRecommendations", Collections.emptyList()
        ));
        when(syncAdminService.repairStrategyReport()).thenReturn(map("recommendations", Collections.emptyList()));
        when(syncAdminService.listProductRollbackCandidates()).thenReturn(Collections.singletonList(map(
                "index", "product_search_v2",
                "exists", true,
                "documentCount", 10L
        )));
        when(adminGovernanceService.dashboard(7, 8, 8)).thenReturn(map("overview", map("stage", "W13")));
        when(adminGovernanceService.releasePreflight(10, 10, 0.2D, null, null, 1D, 24)).thenReturn(map(
                "ready", true,
                "recommended_actions", Collections.singletonList("Ready")
        ));
        when(adminGovernanceService.releaseRecords("", 1, 3)).thenReturn(Collections.emptyList());
        when(adminGovernanceService.evalRuns("", "", null, null, 1, 3)).thenReturn(Collections.emptyList());
        when(realtimeQueryOrchestratorService.status()).thenReturn(map(
                "enabled", true,
                "gatewayEnabled", true,
                "mockGatewayEnabled", false,
                "circuit", map("state", "CLOSED")
        ));
        when(launchAdminStateStore.latestRecord("load_test")).thenReturn(Collections.emptyMap());
        when(launchAdminStateStore.latestRecord("drill")).thenReturn(Collections.emptyMap());
        when(launchAdminStateStore.latestRecord("checklist_snapshot")).thenReturn(Collections.emptyMap());
        when(launchAdminStateStore.listRecords("signoff", 10)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("dependency_check", 20)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("launch_window", 20)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("signoff", 3)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("load_test", 3)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("drill", 3)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("checklist_snapshot", 3)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("dependency_check", 3)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("launch_window", 3)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.launchPackage(10, 10, 0.2D, 1D, 24, 3);

        assertTrue(result.containsKey("timeline"));
        assertTrue(result.containsKey("runbookBundle"));
        assertTrue(result.containsKey("dependencyChecks"));
        assertTrue(result.containsKey("activeLaunchWindow"));
        assertTrue(asMap(result.get("runbookBundle")).containsKey("documents"));
    }

    @Test
    void createCloseoutShouldPersistLaunchPackageSummary() {
        mockHealthyInternalGateway();
        String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        when(syncAdminService.baselineReport()).thenReturn(map(
                "w12Ready", true,
                "products", map("indexValidation", map("consistent", true)),
                "tasks", map("failed", 0, "deadLetterTotal", 0),
                "repairRecommendations", Collections.emptyList()
        ));
        when(syncAdminService.repairStrategyReport()).thenReturn(map("recommendations", Collections.emptyList()));
        when(syncAdminService.listProductRollbackCandidates()).thenReturn(Collections.singletonList(map(
                "index", "product_search_v2",
                "exists", true,
                "documentCount", 10L
        )));
        when(adminGovernanceService.dashboard(7, 8, 8)).thenReturn(map("overview", map("stage", "W13")));
        when(adminGovernanceService.releasePreflight(10, 10, 0.2D, null, null, 1D, 24)).thenReturn(map(
                "ready", true,
                "recommended_actions", Collections.emptyList()
        ));
        when(adminGovernanceService.releaseRecords("", 1, 20)).thenReturn(Collections.emptyList());
        when(adminGovernanceService.evalRuns("", "", null, null, 1, 20)).thenReturn(Collections.emptyList());
        when(realtimeQueryOrchestratorService.status()).thenReturn(map(
                "enabled", true,
                "gatewayEnabled", true,
                "mockGatewayEnabled", false,
                "circuit", map("state", "CLOSED")
        ));
        when(launchAdminStateStore.latestRecord("load_test")).thenReturn(map(
                "id", "load-1",
                "passed", true,
                "updatedAt", now
        ));
        when(launchAdminStateStore.latestRecord("drill")).thenReturn(map(
                "id", "drill-1",
                "passed", true,
                "updatedAt", now
        ));
        when(launchAdminStateStore.latestRecord("checklist_snapshot")).thenReturn(map(
                "id", "checklist-1",
                "passed", true,
                "updatedAt", now
        ));
        when(launchAdminStateStore.listRecords("signoff", 10)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("dependency_check", 20)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("launch_window", 20)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("signoff", 20)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("load_test", 20)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("drill", 20)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("checklist_snapshot", 20)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("dependency_check", 20)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.listRecords("launch_window", 20)).thenReturn(Collections.emptyList());
        when(launchAdminStateStore.saveRecord(org.mockito.ArgumentMatchers.eq("launch_closeout"), org.mockito.ArgumentMatchers.eq("closeout"), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));

        Map<String, Object> result = service.createCloseout(map(
                "name", "go-live-closeout"
        ), 10, 10, 0.2D, 1D, 24, 20);

        assertEquals("go-live-closeout", result.get("name"));
        assertEquals(Boolean.TRUE, result.get("finalReady"));
        assertEquals("ready_for_go_live", result.get("status"));
        assertTrue(asMap(result.get("launchPackage")).containsKey("runbookBundle"));
    }

    private Map<String, Object> findItem(List<Map<String, Object>> checklist, String id) {
        for (Map<String, Object> item : checklist) {
            if (id.equals(item.get("id"))) {
                return item;
            }
        }
        throw new AssertionError("checklist item not found: " + id);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.<String, Object>emptyMap();
    }

    @SuppressWarnings("unchecked")
    private List<String> asList(Object value) {
        return value instanceof List ? (List<String>) value : Collections.<String>emptyList();
    }

    private int intValue(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private void mockHealthyInternalGateway() {
        when(internalRealtimeGatewayService.health()).thenReturn(map(
                "status", "UP",
                "productProviderReady", true
        ));
    }

    private RealtimeResultItem item(String entityId, boolean degraded, String source) {
        RealtimeResultItem item = new RealtimeResultItem();
        item.setEntityId(entityId);
        item.setDegraded(degraded);
        item.setSource(source);
        return item;
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
