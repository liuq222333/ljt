package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Config.AgentActionReviewPythonProperties;
import com.example.demo.demos.Agent.Config.AgentRouterPythonProperties;
import com.example.demo.demos.Agent.Config.AgentComposerPythonProperties;
import com.example.demo.demos.Agent.Config.AgentRouteMatcherProperties;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalResponse;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Runtime.adapter.ActivityRouteResultAdapter;
import com.example.demo.demos.Agent.Runtime.adapter.DefaultRouteResultAdapter;
import com.example.demo.demos.Agent.Runtime.adapter.StoreRouteResultAdapter;
import com.example.demo.demos.Agent.Python.PythonActionReviewClient;
import com.example.demo.demos.Agent.Python.PythonSidecarException;
import com.example.demo.demos.Agent.Python.PythonSidecarMapper;
import com.example.demo.demos.Agent.Python.PythonSidecarModels;
import com.example.demo.demos.Agent.Python.PythonResponseComposerClient;
import com.example.demo.demos.Agent.Python.PythonToolRouterClient;
import com.example.demo.demos.Agent.Service.ApiRouteService;
import com.example.demo.demos.Agent.Service.BackendApiProxyService;
import com.example.demo.demos.Agent.Service.KnowledgeSearchService;
import com.example.demo.demos.Agent.Service.QueryParserService;
import com.example.demo.demos.CommunityMarket.DTO.ProductNearbyDTO;
import com.example.demo.demos.CommunityMarket.Pojo.Category;
import com.example.demo.demos.CommunityMarket.Service.CategoryService;
import com.example.demo.demos.CommunityMarket.Service.ProductsService;
import com.example.demo.demos.common.enums.AnswerType;
import com.example.demo.demos.common.enums.PlanType;
import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.common.enums.TaskType;
import com.example.demo.demos.common.enums.ToolName;
import com.example.demo.demos.common.error.BizException;
import com.example.demo.demos.common.error.ErrorCode;
import com.example.demo.demos.common.schema.FinalAnswer;
import com.example.demo.demos.common.schema.ToolPlan;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.model.RealtimeResultItem;
import com.example.demo.demos.realtime.service.RealtimeQueryOrchestratorService;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import com.example.demo.demos.search.mapper.ProductSearchSnapshotMapper;
import com.example.demo.demos.search.mapper.QueryExpandDictMapper;
import com.example.demo.demos.search.mapper.SearchCategoryMapper;
import com.example.demo.demos.search.service.ProductQueryExpansionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentRuntimeTest {

    @Mock
    private AgentCheckpointStore agentCheckpointStore;
    @Mock
    private QueryParserService queryParserService;
    @Mock
    private PythonToolRouterClient pythonToolRouterClient;
    @Mock
    private PythonResponseComposerClient pythonResponseComposerClient;
    @Mock
    private PythonActionReviewClient pythonActionReviewClient;
    @Mock
    private RealtimeQueryOrchestratorService realtimeQueryOrchestratorService;
    @Mock
    private ApiRouteService apiRouteService;
    @Mock
    private BackendApiProxyService backendApiProxyService;
    @Mock
    private ProductSearchSnapshotMapper productSearchSnapshotMapper;
    @Mock
    private KnowledgeSearchService knowledgeSearchService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private ProductsService productsService;
    @Mock
    private QueryExpandDictMapper queryExpandDictMapper;
    @Mock
    private SearchCategoryMapper searchCategoryMapper;

    private AgentRuntime agentRuntime;
    private ActionConversationStore actionConversationStore;
    private AgentRouterPythonProperties agentRouterPythonProperties;
    private AgentComposerPythonProperties agentComposerPythonProperties;
    private AgentActionReviewPythonProperties agentActionReviewPythonProperties;

    @BeforeEach
    void setUp() {
        actionConversationStore = new ActionConversationStore();
        agentRouterPythonProperties = new AgentRouterPythonProperties();
        agentRouterPythonProperties.setEnabled(false);
        agentComposerPythonProperties = new AgentComposerPythonProperties();
        agentComposerPythonProperties.setEnabled(false);
        agentActionReviewPythonProperties = new AgentActionReviewPythonProperties();
        agentActionReviewPythonProperties.setEnabled(false);
        ProductQueryExpansionService productQueryExpansionService = new ProductQueryExpansionService(queryExpandDictMapper, searchCategoryMapper);
        ApiRouteExecutionService apiRouteExecutionService = new ApiRouteExecutionService(
                new ApiRouteIntentMatcher(apiRouteService, new AgentRouteMatcherProperties()),
                new RouteParamBuilder(),
                backendApiProxyService,
                java.util.Arrays.asList(
                        new ActivityRouteResultAdapter(),
                        new StoreRouteResultAdapter(),
                        new DefaultRouteResultAdapter()
                )
        );
        agentRuntime = new AgentRuntime(
                agentCheckpointStore,
                queryParserService,
                agentRouterPythonProperties,
                agentComposerPythonProperties,
                pythonToolRouterClient,
                pythonResponseComposerClient,
                new SidecarToolPlanAdapter(),
                realtimeQueryOrchestratorService,
                new ActionIntentReviewService(
                        apiRouteService,
                        backendApiProxyService,
                        actionConversationStore,
                        new LocalActivityActionAdapter(apiRouteService, backendApiProxyService, actionConversationStore),
                        categoryService,
                        agentActionReviewPythonProperties,
                        pythonActionReviewClient,
                        new PythonSidecarMapper()
                ),
                new RuntimeAnswerComposer(),
                productSearchSnapshotMapper,
                knowledgeSearchService,
                productsService,
                productQueryExpansionService,
                null,
                null,
                apiRouteExecutionService
        );
    }

    @Test
    void runShouldExecuteRealtimePathAndBuildCitations() {
        ParsedIntent parsedIntent = realtimeIntent();
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);

        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.SUCCESS);
        RealtimeResultItem item = new RealtimeResultItem();
        item.setEntityId("123");
        item.setAvailabilityStatus("bookable");
        item.setInventoryStatus("available");
        item.setInventoryCount(9);
        item.setSellStatus("on_sale");
        item.setPrice(new BigDecimal("19.90"));
        item.setCurrency("CNY");
        item.setBookable(true);
        item.setSource("gateway");
        response.getItems().add(item);
        when(realtimeQueryOrchestratorService.query(any())).thenReturn(response);

        SessionState state = agentRuntime.run(request("check product id 123 realtime"), null);

        assertEquals(AnswerType.REALTIME_CONFIRMATION, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getExecutionMeta().getCompletedNodes().contains("realtime_query"));
        assertEquals("success", state.getIntermediateData().get("realtimeStatus"));
        assertNotNull(state.getFinalAnswer().getSummary());
        assertEquals(1, state.getFinalAnswer().getCitations().size());
        assertEquals("realtime_service", state.getFinalAnswer().getCitations().get(0).getSourceType());
        assertEquals("gateway", state.getFinalAnswer().getCards().get(0).getSourceLabel());
        assertNotNull(state.getFinalAnswer().getDebugTrace());
        verify(realtimeQueryOrchestratorService).query(any());
    }

    @Test
    void runShouldAskForClarificationWhenRealtimeIntentHasNoEntityId() {
        ParsedIntent parsedIntent = realtimeIntent();
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);

        SessionState state = agentRuntime.run(request("can I buy this now"), null);

        assertEquals(AnswerType.CLARIFICATION, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getFinalAnswer().getAnswerText().contains("商品 ID"));
        assertFalse(state.getFinalAnswer().getNextActions().isEmpty());
        verify(realtimeQueryOrchestratorService, never()).query(any());
    }

    @Test
    void runShouldExecuteStructuredSearchFlowForProductIntent() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);

        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        snapshot.setProductId(1L);
        snapshot.setTitle("Sample product");
        snapshot.setSummaryText("Snapshot-backed search result");
        when(productSearchSnapshotMapper.searchForProducts(any())).thenReturn(Collections.singletonList(snapshot));

        SessionState state = agentRuntime.run(request("find a product"), null);

        assertEquals(AnswerType.RECOMMENDATION, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getExecutionMeta().getCompletedNodes().contains("normalize_params"));
        assertTrue(state.getExecutionMeta().getCompletedNodes().contains("route_tools"));
        assertTrue(state.getExecutionMeta().getCompletedNodes().contains("execute"));
        assertTrue(state.getExecutionMeta().getCompletedNodes().contains("compose_response"));
        assertEquals("product_search_snapshot", state.getFinalAnswer().getComposerMeta().getUsedSources().get(0));
    }

    @Test
    void runShouldUsePythonToolRouterPlanWhenAvailable() {
        agentRouterPythonProperties.setEnabled(true);
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);

        ToolPlan toolPlan = new ToolPlan();
        toolPlan.setPlanType(PlanType.SEARCH_THEN_KNOWLEDGE);
        toolPlan.setRoutingReason("python_sidecar_search_then_knowledge");
        ToolPlan.ToolStep searchStep = new ToolPlan.ToolStep();
        searchStep.setStepId("search");
        searchStep.setToolName(ToolName.STRUCTURED_SEARCH);
        ToolPlan.ToolStep knowledgeStep = new ToolPlan.ToolStep();
        knowledgeStep.setStepId("knowledge");
        knowledgeStep.setToolName(ToolName.KNOWLEDGE_RETRIEVAL);
        toolPlan.getSteps().add(searchStep);
        toolPlan.getSteps().add(knowledgeStep);
        when(pythonToolRouterClient.route(any(), any(), any(), anyBoolean(), any(java.util.List.class), any(java.util.List.class), any()))
                .thenReturn(toolPlan);

        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        snapshot.setProductId(1L);
        snapshot.setTitle("Apple");
        snapshot.setSummaryText("Fresh apple");
        when(productSearchSnapshotMapper.searchForProducts(any())).thenReturn(Collections.singletonList(snapshot));

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(101L);
        knowledgeBase.setTitle("Apple refund");
        knowledgeBase.setSummary("Apple can be refunded");
        KnowledgeRetrievalResponse knowledgeResponse = new KnowledgeRetrievalResponse();
        knowledgeResponse.setHitCount(1);
        knowledgeResponse.getItems().add(knowledgeBase);
        when(knowledgeSearchService.retrieve(any())).thenReturn(knowledgeResponse);

        SessionState state = agentRuntime.run(request("find apple"), null);

        assertEquals("search_then_knowledge", state.getIntermediateData().get("routePlan"));
        assertEquals("python", state.getIntermediateData().get("routePlanSource"));
        assertTrue(state.getFinalAnswer().getComposerMeta().getUsedSources().contains("knowledge_base"));
        verify(knowledgeSearchService).retrieve(any());
    }

    @Test
    void runShouldFallbackToLocalRouterWhenPythonToolRouterFails() {
        agentRouterPythonProperties.setEnabled(true);
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);
        when(pythonToolRouterClient.route(any(), any(), any(), anyBoolean(), any(java.util.List.class), any(java.util.List.class), any()))
                .thenThrow(new PythonSidecarException("router down"));

        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        snapshot.setProductId(2L);
        snapshot.setTitle("Fallback Apple");
        snapshot.setSummaryText("Fallback result");
        when(productSearchSnapshotMapper.searchForProducts(any())).thenReturn(Collections.singletonList(snapshot));

        SessionState state = agentRuntime.run(request("find apple"), null);

        assertEquals("search_only", state.getIntermediateData().get("routePlan"));
        assertEquals("java", state.getIntermediateData().get("routePlanSource"));
        verify(knowledgeSearchService, never()).retrieve(any());
    }

    @Test
    void runShouldUsePythonComposerWhenAvailable() {
        agentComposerPythonProperties.setEnabled(true);
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);

        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        snapshot.setProductId(3L);
        snapshot.setTitle("Python Apple");
        when(productSearchSnapshotMapper.searchForProducts(any())).thenReturn(Collections.singletonList(snapshot));

        FinalAnswer pythonAnswer = new FinalAnswer();
        pythonAnswer.setAnswerType(AnswerType.RECOMMENDATION);
        pythonAnswer.setAnswerText("python composed answer");
        pythonAnswer.setSummary("python summary");
        when(pythonResponseComposerClient.compose(any(), any(), any(), any(java.util.List.class), anyLong(), any(), any(), any(), any(), anyBoolean(), any(), any(), any()))
                .thenReturn(pythonAnswer);

        SessionState state = agentRuntime.run(request("find apple"), null);

        assertEquals("python composed answer", state.getFinalAnswer().getAnswerText());
        verify(pythonResponseComposerClient).compose(any(), any(), any(), any(java.util.List.class), anyLong(), any(), any(), any(), any(), anyBoolean(), any(), any(), any());
    }

    @Test
    void runShouldFallbackToJavaComposerWhenPythonComposerFails() {
        agentComposerPythonProperties.setEnabled(true);
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);

        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        snapshot.setProductId(4L);
        snapshot.setTitle("Local Apple");
        snapshot.setSummaryText("Local summary");
        when(productSearchSnapshotMapper.searchForProducts(any())).thenReturn(Collections.singletonList(snapshot));

        when(pythonResponseComposerClient.compose(any(), any(), any(), any(java.util.List.class), anyLong(), any(), any(), any(), any(), anyBoolean(), any(), any(), any()))
                .thenThrow(new PythonSidecarException("composer down"));

        SessionState state = agentRuntime.run(request("find apple"), null);

        assertEquals(AnswerType.RECOMMENDATION, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getFinalAnswer().getAnswerText().contains("Local Apple"));
    }

    @Test
    void runShouldUseRealtimeHeuristicForExplicitRealtimeAvailabilityQuery() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        parsedIntent.setNeedRealtime(false);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);

        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.SUCCESS);
        RealtimeResultItem item = new RealtimeResultItem();
        item.setEntityId("1");
        item.setAvailabilityStatus("bookable");
        item.setInventoryStatus("available");
        item.setInventoryCount(3);
        item.setSellStatus("on_sale");
        item.setSource("gateway");
        response.getItems().add(item);
        when(realtimeQueryOrchestratorService.query(any())).thenReturn(response);

        SessionState state = agentRuntime.run(request("product id 1 realtime availability"), null);

        assertEquals(AnswerType.REALTIME_CONFIRMATION, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getExecutionMeta().getCompletedNodes().contains("realtime_query"));
        verify(realtimeQueryOrchestratorService).query(any());
    }

    @Test
    void runShouldNotTreatArbitraryNumbersAsEntityIds() {
        ParsedIntent parsedIntent = realtimeIntent();
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);

        SessionState state = agentRuntime.run(request("under 100 yuan can I buy now"), null);

        assertEquals(AnswerType.CLARIFICATION, state.getFinalAnswer().getAnswerType());
        verify(realtimeQueryOrchestratorService, never()).query(any());
    }

    @Test
    void runShouldDegradeInsteadOfThrowingWhenRealtimeFails() {
        ParsedIntent parsedIntent = realtimeIntent();
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);
        when(realtimeQueryOrchestratorService.query(any()))
                .thenThrow(new BizException(ErrorCode.REALTIME_TIMEOUT, "timeout"));

        SessionState state = agentRuntime.run(request("check product id 123 realtime"), null);

        assertEquals(AnswerType.PARTIAL_RESULT, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getExecutionMeta().isDegraded());
        assertEquals("realtime_query", state.getExecutionMeta().getFailedNode());
        assertEquals(String.valueOf(ErrorCode.REALTIME_TIMEOUT), state.getExecutionMeta().getErrorCode());
        assertFalse(state.getFinalAnswer().getDisclaimers().isEmpty());
        assertEquals("realtime_service", state.getFinalAnswer().getCitations().get(0).getSourceType());
    }

    @Test
    void runShouldSearchThenRealtimeForRealtimeSearchIntentWithoutExplicitEntityId() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.REALTIME_QUERY);
        parsedIntent.setNeedRealtime(true);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);

        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        snapshot.setProductId(11L);
        snapshot.setTitle("新鲜苹果");
        when(productSearchSnapshotMapper.searchForProducts(any())).thenReturn(Collections.singletonList(snapshot));

        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.SUCCESS);
        RealtimeResultItem item = new RealtimeResultItem();
        item.setEntityId("11");
        item.setAvailabilityStatus("在售");
        item.setInventoryStatus("available");
        item.setInventoryCount(8);
        item.setBookable(true);
        item.setSource("snapshot_fallback");
        response.getItems().add(item);
        when(realtimeQueryOrchestratorService.query(any())).thenReturn(response);

        SessionState state = agentRuntime.run(request("我想要买点水果，现在有什么水果在卖"), null);

        assertEquals(AnswerType.RECOMMENDATION, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getFinalAnswer().getSummary().contains("已补充实时状态"));
        assertTrue(state.getFinalAnswer().getAnswerText().contains("实时状态"));
        assertTrue(state.getExecutionMeta().getCompletedNodes().contains("execute"));
        assertFalse(state.getExecutionMeta().getCompletedNodes().contains("realtime_query"));
        verify(realtimeQueryOrchestratorService).query(any());
    }

    @Test
    void runShouldExtractKeywordAndAvoidRealtimeClarificationForRealtimeSearchQuery() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.REALTIME_QUERY);
        parsedIntent.setNeedRealtime(true);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);

        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        snapshot.setProductId(21L);
        snapshot.setTitle("\u65b0\u9c9c\u6c34\u679c");
        when(productSearchSnapshotMapper.searchForProducts(any())).thenReturn(Collections.singletonList(snapshot));

        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.SUCCESS);
        RealtimeResultItem item = new RealtimeResultItem();
        item.setEntityId("21");
        item.setAvailabilityStatus("\u5728\u5356");
        item.setInventoryStatus("available");
        item.setInventoryCount(6);
        item.setBookable(true);
        item.setSource("snapshot_fallback");
        response.getItems().add(item);
        when(realtimeQueryOrchestratorService.query(any())).thenReturn(response);

        SessionState state = agentRuntime.run(request("\u6211\u60f3\u8981\u4e70\u70b9\u6c34\u679c\uff0c\u73b0\u5728\u6709\u4ec0\u4e48\u6c34\u679c\u5728\u5356"), null);

        assertEquals(AnswerType.RECOMMENDATION, state.getFinalAnswer().getAnswerType());
        assertEquals("\u6c34\u679c", state.getIntermediateData().get("normalizedKeyword"));
        assertFalse(state.getFinalAnswer().getAnswerText().contains("\u5546\u54c1 ID"));
        verify(realtimeQueryOrchestratorService).query(any());
    }

    @Test
    void runShouldReturnNoResultForRealtimeSearchWithoutCandidates() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.REALTIME_QUERY);
        parsedIntent.setNeedRealtime(true);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);
        when(productSearchSnapshotMapper.searchForProducts(any())).thenReturn(Collections.emptyList());

        SessionState state = agentRuntime.run(request("\u6211\u60f3\u8981\u4e70\u70b9\u6c34\u679c\uff0c\u73b0\u5728\u6709\u4ec0\u4e48\u6c34\u679c\u5728\u5356"), null);

        assertEquals(AnswerType.NO_RESULT, state.getFinalAnswer().getAnswerType());
        assertEquals("\u6c34\u679c", state.getIntermediateData().get("normalizedKeyword"));
        assertFalse(state.getFinalAnswer().getAnswerText().contains("\u5546\u54c1 ID"));
        verify(realtimeQueryOrchestratorService, never()).query(any());
    }

    @Test
    void runShouldExplainUnsupportedWriteActionInsteadOfReturningNoResult() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);
        when(apiRouteService.listEnabledRoutes("product", "CREATE"))
                .thenReturn(Collections.singletonList(route(6L, "product", "create", "CREATE", "\u53d1\u5e03/\u65b0\u589e\u4e00\u4e2a\u5546\u54c1")));

        SessionState state = agentRuntime.run(request("\u5e2e\u6211\u53d1\u5e03\u4e00\u4e2a\u5546\u54c1"), null);

        assertEquals(AnswerType.CLARIFICATION, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getFinalAnswer().getAnswerText().contains("\u5546\u54c1\u540d\u79f0"));
        assertTrue(state.getExecutionMeta().getCompletedNodes().contains("action_review"));
    }

    @Test
    void runShouldUsePythonActionReviewForCreateClarification() {
        agentActionReviewPythonProperties.setEnabled(true);
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);
        when(apiRouteService.listEnabledRoutes("product", "CREATE"))
                .thenReturn(Collections.singletonList(route(6L, "product", "create", "CREATE", "\u53d1\u5e03/\u65b0\u589e\u4e00\u4e2a\u5546\u54c1")));
        when(pythonActionReviewClient.review(any(), any(), any(), any(java.util.List.class)))
                .thenReturn(actionReviewResponse("need_clarification", pendingAction("create", "CREATE")));

        SessionState state = agentRuntime.run(request("\u5e2e\u6211\u53d1\u5e03\u4e00\u4e2a\u5546\u54c1"), "Bearer token-demo");

        assertEquals(AnswerType.CLARIFICATION, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getFinalAnswer().getAnswerText().contains("\u5546\u54c1\u540d\u79f0"));
        assertEquals("create", state.getIntermediateData().get("actionName"));
        verify(pythonActionReviewClient).review(any(), any(), any(), any(java.util.List.class));
        verify(backendApiProxyService, never()).invoke(any(), any());
    }

    @Test
    void runShouldFallbackToLocalActionReviewWhenPythonActionReviewFails() {
        agentActionReviewPythonProperties.setEnabled(true);
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);
        when(apiRouteService.listEnabledRoutes("product", "CREATE"))
                .thenReturn(Collections.singletonList(route(6L, "product", "create", "CREATE", "\u53d1\u5e03/\u65b0\u589e\u4e00\u4e2a\u5546\u54c1")));
        when(pythonActionReviewClient.review(any(), any(), any(), any(java.util.List.class)))
                .thenThrow(new PythonSidecarException("python action review down"));

        SessionState state = agentRuntime.run(request("\u5e2e\u6211\u53d1\u5e03\u4e00\u4e2a\u5546\u54c1"), "Bearer token-demo");

        assertEquals(AnswerType.CLARIFICATION, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getFinalAnswer().getAnswerText().contains("\u5546\u54c1\u540d\u79f0"));
        assertEquals("create", state.getIntermediateData().get("actionName"));
        verify(pythonActionReviewClient).review(any(), any(), any(), any(java.util.List.class));
        verify(backendApiProxyService, never()).invoke(any(), any());
    }

    @Test
    void runShouldExecuteConfirmedActionViaJavaBackendAfterPythonActionReview() {
        agentActionReviewPythonProperties.setEnabled(true);
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);
        when(apiRouteService.listEnabledRoutes("product", "CREATE"))
                .thenReturn(Collections.singletonList(route(6L, "product", "create", "CREATE", "\u53d1\u5e03/\u65b0\u589e\u4e00\u4e2a\u5546\u54c1")));
        when(pythonActionReviewClient.review(any(), any(), any(), any(java.util.List.class)))
                .thenReturn(actionReviewResponse("need_confirmation", createPendingActionPayload()))
                .thenReturn(actionReviewResponse("ready_to_execute", createPendingActionPayload()));

        BackendApiProxyService.InvocationResult invocationResult = new BackendApiProxyService.InvocationResult();
        invocationResult.setPresentationHint("direct_response");
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("code", 200);
        payload.put("message", "\u64cd\u4f5c\u6210\u529f");
        invocationResult.setData(payload);
        when(backendApiProxyService.invoke(any(), any())).thenReturn(invocationResult);

        AgentChatRequest firstRequest = request(
                "\u53d1\u5e03\u5546\u54c1\uff0c\u5546\u54c1\u540d\u53eb\u82f9\u679c\u624b\u673a\uff0c\u4ef7\u683c3\u5143\uff0c\u5e93\u5b5810\u4ef6\uff0c\u5730\u70b9\u4e00\u98df\u5802\uff0c\u5206\u7c7b15\uff0c\u56fe\u7247https://img.example.com/apple.jpg"
        );
        firstRequest.setSessionId("python-action-s-1");
        SessionState firstState = agentRuntime.run(firstRequest, "Bearer token-demo");

        assertEquals(AnswerType.CLARIFICATION, firstState.getFinalAnswer().getAnswerType());
        assertTrue(firstState.getFinalAnswer().getAnswerText().contains("\u786e\u8ba4"));

        AgentChatRequest confirmRequest = request("\u786e\u8ba4\u6267\u884c");
        confirmRequest.setSessionId("python-action-s-1");
        SessionState secondState = agentRuntime.run(confirmRequest, "Bearer token-demo");

        assertEquals(AnswerType.FAQ_ANSWER, secondState.getFinalAnswer().getAnswerType());
        assertTrue(secondState.getExecutionMeta().getCompletedNodes().contains("action_execute"));
        verify(backendApiProxyService).invoke(any(), any());
    }

    @Test
    void runShouldReturnChitchatGreetingForHello() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.CHITCHAT);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);

        SessionState state = agentRuntime.run(request("你好"), null);

        assertEquals(AnswerType.FAQ_ANSWER, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getFinalAnswer().getAnswerText().contains("你好"));
        assertTrue(state.getExecutionMeta().getCompletedNodes().contains("compose_response"));
    }

    @Test
    void runShouldAskForMissingCreateProductFieldsViaActionReview() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);
        when(apiRouteService.listEnabledRoutes("product", "CREATE"))
                .thenReturn(Collections.singletonList(route(6L, "product", "create", "CREATE", "\u53d1\u5e03/\u65b0\u589e\u4e00\u4e2a\u5546\u54c1")));
        SessionState state = agentRuntime.run(
                request("\u5e2e\u6211\u53d1\u5e03\u5546\u54c1\uff0c\u540d\u5b57\u53eb\u82f9\u679c\uff0c\u4ef7\u683c3\u5143\uff0c\u5e93\u5b5810\u4ef6\uff0c\u5730\u70b9\u4e00\u98df\u5802"),
                "Bearer token-demo"
        );

        assertEquals(AnswerType.CLARIFICATION, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getFinalAnswer().getAnswerText().contains("\u56fe\u7247"));
        assertEquals("create", state.getIntermediateData().get("actionName"));
        verify(backendApiProxyService, never()).invoke(any(), any());
    }

    @Test
    void runShouldExecuteConfirmedCreateProductAction() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);
        when(apiRouteService.listEnabledRoutes("product", "CREATE"))
                .thenReturn(Collections.singletonList(route(6L, "product", "create", "CREATE", "\u53d1\u5e03/\u65b0\u589e\u4e00\u4e2a\u5546\u54c1")));
        when(categoryService.getAllCategories()).thenReturn(Collections.singletonList(category("15", "\u624b\u673a\u901a\u8baf")));

        BackendApiProxyService.InvocationResult invocationResult = new BackendApiProxyService.InvocationResult();
        invocationResult.setPresentationHint("direct_response");
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("code", 200);
        payload.put("message", "\u64cd\u4f5c\u6210\u529f");
        invocationResult.setData(payload);
        when(backendApiProxyService.invoke(any(), any())).thenReturn(invocationResult);

        AgentChatRequest firstRequest = request(
                "\u53d1\u5e03\u5546\u54c1\uff0c\u5546\u54c1\u540d\u53eb\u82f9\u679c\u624b\u673a\uff0c\u4ef7\u683c3\u5143\uff0c\u5e93\u5b5810\u4ef6\uff0c\u5730\u70b9\u4e00\u98df\u5802\uff0c\u56fe\u7247https://img.example.com/apple.jpg"
        );
        firstRequest.setSessionId("action-s-1");
        SessionState firstState = agentRuntime.run(firstRequest, "Bearer token-demo");

        assertEquals(AnswerType.CLARIFICATION, firstState.getFinalAnswer().getAnswerType());
        assertTrue(firstState.getFinalAnswer().getAnswerText().contains("\u786e\u8ba4"));

        AgentChatRequest confirmRequest = request("\u786e\u8ba4\u6267\u884c");
        confirmRequest.setSessionId("action-s-1");
        SessionState secondState = agentRuntime.run(confirmRequest, "Bearer token-demo");

        assertEquals(AnswerType.FAQ_ANSWER, secondState.getFinalAnswer().getAnswerType());
        assertTrue(secondState.getFinalAnswer().getAnswerText().contains("\u53d1\u5e03\u5546\u54c1"));
        assertTrue(secondState.getExecutionMeta().getCompletedNodes().contains("action_execute"));
        verify(backendApiProxyService).invoke(any(), any());
    }

    @Test
    void runShouldAskForCategoryBeforeCreateConfirmationWhenCategoryCannotBeInferred() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);
        when(apiRouteService.listEnabledRoutes("product", "CREATE"))
                .thenReturn(Collections.singletonList(route(6L, "product", "create", "CREATE", "\u53d1\u5e03/\u65b0\u589e\u4e00\u4e2a\u5546\u54c1")));
        SessionState state = agentRuntime.run(
                request("\u53d1\u5e03\u5546\u54c1\uff0c\u5546\u54c1\u540d\u53eb\u624b\u5de5\u597d\u7269\uff0c\u4ef7\u683c30\u5143\uff0c\u5e93\u5b5810\u4ef6\uff0c\u5730\u70b9\u4e00\u98df\u5802\uff0c\u56fe\u7247https://img.example.com/a.jpg"),
                "Bearer token-demo"
        );

        assertEquals(AnswerType.CLARIFICATION, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getFinalAnswer().getAnswerText().contains("\u5546\u54c1\u5206\u7c7b"));
        verify(backendApiProxyService, never()).invoke(any(), any());
    }

    @Test
    void runShouldExecuteConfirmedUpdatePriceAction() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class))).thenReturn(parsedIntent);
        when(apiRouteService.listEnabledRoutes("product", "UPDATE"))
                .thenReturn(Collections.singletonList(route(11L, "product", "update_price", "UPDATE", "\u8c03\u6574\u5546\u54c1\u4ef7\u683c")));

        BackendApiProxyService.InvocationResult invocationResult = new BackendApiProxyService.InvocationResult();
        invocationResult.setPresentationHint("direct_response");
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<String, Object>();
        data.put("code", 200);
        data.put("message", "\u64cd\u4f5c\u6210\u529f");
        invocationResult.setData(data);
        when(backendApiProxyService.invoke(any(), any())).thenReturn(invocationResult);

        SessionState state = agentRuntime.run(
                request("\u628a\u5546\u54c1 id 1 \u4ef7\u683c\u6539\u6210 9.9 \u5143\uff0c\u786e\u8ba4\u6267\u884c"),
                "Bearer token-demo"
        );

        assertEquals(AnswerType.FAQ_ANSWER, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getFinalAnswer().getAnswerText().contains("\u4fee\u6539\u5546\u54c1\u4ef7\u683c"));
        assertTrue(state.getExecutionMeta().getCompletedNodes().contains("action_execute"));
        verify(backendApiProxyService).invoke(any(), any());
    }

    @Test
    void runShouldResolveRealtimeFollowUpFromRestoredSessionContext() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.REALTIME_QUERY);
        parsedIntent.setNeedRealtime(true);
        parsedIntent.getCandidateSlots().setEntityRef("recent");
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class)))
                .thenReturn(parsedIntent);

        SessionState.SessionContext restoredContext = new SessionState.SessionContext();
        restoredContext.setFocusedEntityId("250");
        restoredContext.setCandidateEntities(Collections.singletonList("250"));
        restoredContext.setLastSelectedEntityIds(Collections.singletonList("250"));
        when(agentCheckpointStore.latestSessionContext("session-follow-up")).thenReturn(restoredContext);

        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        snapshot.setProductId(250L);
        snapshot.setTitle("\u8349\u8393");
        snapshot.setSummaryText("\u65b0\u9c9c\u8349\u8393");
        when(productSearchSnapshotMapper.selectByProductId(250L)).thenReturn(snapshot);

        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.SUCCESS);
        RealtimeResultItem item = new RealtimeResultItem();
        item.setEntityId("250");
        item.setAvailabilityStatus("\u5728\u5356");
        item.setInventoryStatus("available");
        item.setInventoryCount(6);
        item.setBookable(true);
        item.setSource("gateway");
        response.getItems().add(item);
        when(realtimeQueryOrchestratorService.query(any())).thenReturn(response);

        AgentChatRequest followUpRequest = request("\u8fd9\u4e2a\u5546\u54c1\u6709\u8d27\u5417");
        followUpRequest.setSessionId("session-follow-up");
        SessionState state = agentRuntime.run(followUpRequest, null);

        assertEquals(AnswerType.RECOMMENDATION, state.getFinalAnswer().getAnswerType());
        assertEquals(250L, state.getIntermediateData().get("referencedProductId"));
        assertEquals("250", state.getSessionContext().getFocusedEntityId());
        verify(productSearchSnapshotMapper).selectByProductId(250L);
        verify(realtimeQueryOrchestratorService).query(any());
    }

    @Test
    void runShouldReuseSearchKeywordFromRestoredSessionContextForGenericFollowUp() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        parsedIntent.setFollowUp(true);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class)))
                .thenReturn(parsedIntent);

        SessionState.SessionContext restoredContext = new SessionState.SessionContext();
        restoredContext.getConfirmedConstraints().put("keyword", "\u6c34\u679c");
        when(agentCheckpointStore.latestSessionContext("session-search-follow-up")).thenReturn(restoredContext);

        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        snapshot.setProductId(301L);
        snapshot.setTitle("\u8349\u8393");
        snapshot.setSummaryText("\u5f53\u65e5\u65b0\u9c9c\u4e0a\u67b6");
        when(productSearchSnapshotMapper.searchForProducts(any())).thenReturn(Collections.singletonList(snapshot));

        AgentChatRequest followUpRequest = request("\u90fd\u6709\u4ec0\u4e48");
        followUpRequest.setSessionId("session-search-follow-up");
        SessionState state = agentRuntime.run(followUpRequest, null);

        assertEquals(AnswerType.RECOMMENDATION, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getExecutionMeta().isRestoredFromCheckpoint());
        assertEquals("\u6c34\u679c", state.getIntermediateData().get("normalizedKeyword"));
        assertEquals(1L, state.getIntermediateData().get("searchHitCount"));
        assertTrue(state.getFinalAnswer().getAnswerText().contains("\u8349\u8393"));
    }

    @Test
    void runShouldUseNearbySearchWhenCoordinatesAreProvided() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.MIXED_SEARCH_REALTIME);
        parsedIntent.setNeedRealtime(true);
        parsedIntent.getCandidateSlots().setKeyword("\u6c34\u679c");
        parsedIntent.getCandidateSlots().setLocationText("\u9644\u8fd1");
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class)))
                .thenReturn(parsedIntent);

        ProductNearbyDTO nearby = new ProductNearbyDTO();
        nearby.setId(250L);
        nearby.setCategoryId(1);
        nearby.setTitle("\u8349\u8393");
        nearby.setDescription("\u9650\u65f6\u5728\u5356");
        nearby.setPrice(new BigDecimal("19.90"));
        nearby.setDistanceKm(0.5D);
        when(productsService.getNearbyProducts(any(double.class), any(double.class), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(ResponseEntity.ok(new ArrayList<ProductNearbyDTO>(Collections.singletonList(nearby))));

        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.SUCCESS);
        RealtimeResultItem item = new RealtimeResultItem();
        item.setEntityId("250");
        item.setAvailabilityStatus("\u5728\u5356");
        item.setInventoryStatus("available");
        item.setInventoryCount(4);
        item.setBookable(true);
        item.setSource("gateway");
        response.getItems().add(item);
        when(realtimeQueryOrchestratorService.query(any())).thenReturn(response);

        AgentChatRequest nearbyRequest = request("\u9644\u8fd1\u6709\u4ec0\u4e48\u6c34\u679c\u5728\u5356");
        java.util.Map<String, Object> userProfile = new java.util.LinkedHashMap<String, Object>();
        userProfile.put("latitude", 31.2304D);
        userProfile.put("longitude", 121.4737D);
        userProfile.put("nearbyRadiusKm", 3D);
        nearbyRequest.setUserProfile(userProfile);

        SessionState state = agentRuntime.run(nearbyRequest, null);

        assertEquals(AnswerType.RECOMMENDATION, state.getFinalAnswer().getAnswerType());
        assertEquals(Boolean.TRUE, state.getIntermediateData().get("nearbyRequested"));
        verify(productsService).getNearbyProducts(any(double.class), any(double.class), any(), any(), any(), any(), any(), any(), any());
        verify(realtimeQueryOrchestratorService).query(any());
    }

    @Test
    void runShouldClarifyNearbyRequestWithoutCoordinatesEvenWhenPythonRouterReturnsPlan() {
        agentRouterPythonProperties.setEnabled(true);
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.MIXED_SEARCH_REALTIME);
        parsedIntent.setNeedRealtime(true);
        parsedIntent.getCandidateSlots().setKeyword("\u6c34\u679c");
        parsedIntent.getCandidateSlots().setLocationText("\u9644\u8fd1");
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class)))
                .thenReturn(parsedIntent);

        ToolPlan toolPlan = new ToolPlan();
        toolPlan.setPlanType(PlanType.SEARCH_THEN_REALTIME);
        ToolPlan.ToolStep searchStep = new ToolPlan.ToolStep();
        searchStep.setStepId("search");
        searchStep.setToolName(ToolName.STRUCTURED_SEARCH);
        ToolPlan.ToolStep realtimeStep = new ToolPlan.ToolStep();
        realtimeStep.setStepId("realtime");
        realtimeStep.setToolName(ToolName.REALTIME_QUERY);
        toolPlan.getSteps().add(searchStep);
        toolPlan.getSteps().add(realtimeStep);
        when(pythonToolRouterClient.route(any(), any(), any(), anyBoolean(), any(java.util.List.class), any(java.util.List.class), any()))
                .thenReturn(toolPlan);

        SessionState state = agentRuntime.run(request("\u9644\u8fd1\u6709\u4ec0\u4e48\u6c34\u679c\u5728\u5356"), null);

        assertEquals(AnswerType.CLARIFICATION, state.getFinalAnswer().getAnswerType());
        assertEquals("clarification", state.getIntermediateData().get("routePlan"));
        assertEquals("java_guard", state.getIntermediateData().get("routePlanSource"));
        verify(productsService, never()).getNearbyProducts(any(double.class), any(double.class), any(), any(), any(), any(), any(), any(), any());
        verify(realtimeQueryOrchestratorService, never()).query(any());
    }

    @Test
    void runShouldFallbackToExpandedKeywordsWhenFruitCategoryHasNoDirectHits() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        parsedIntent.getCandidateSlots().setKeyword("\u6c34\u679c");
        parsedIntent.getCandidateSlots().setCategoryText("\u6c34\u679c");
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class)))
                .thenReturn(parsedIntent);

        when(productSearchSnapshotMapper.searchForProducts(any())).thenAnswer(invocation -> {
            com.example.demo.demos.search.model.ProductSearchQuery query = invocation.getArgument(0);
            if (!"\u8349\u8393".equals(query.getKeyword())) {
                return Collections.emptyList();
            }
            ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
            snapshot.setProductId(301L);
            snapshot.setTitle("\u8349\u8393");
            snapshot.setSummaryText("\u5f53\u65e5\u65b0\u9c9c\u4e0a\u67b6");
            return Collections.singletonList(snapshot);
        });

        SessionState state = agentRuntime.run(request("\u6709\u4ec0\u4e48\u6c34\u679c\u5728\u5356"), null);

        assertEquals(AnswerType.RECOMMENDATION, state.getFinalAnswer().getAnswerType());
        assertEquals("\u6c34\u679c", state.getIntermediateData().get("normalizedKeyword"));
        assertTrue(state.getFinalAnswer().getAnswerText().contains("\u8349\u8393"));
    }

    @Test
    void runShouldFilterElectronicsFromFruitExpansionResults() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        parsedIntent.getCandidateSlots().setKeyword("\u6c34\u679c");
        parsedIntent.getCandidateSlots().setCategoryText("\u6c34\u679c");
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class)))
                .thenReturn(parsedIntent);

        when(productSearchSnapshotMapper.searchForProducts(any())).thenAnswer(invocation -> {
            com.example.demo.demos.search.model.ProductSearchQuery query = invocation.getArgument(0);
            if ("\u8349\u8393".equals(query.getKeyword())) {
                return Collections.singletonList(snapshot(250L, "\u65b0\u9c9c\u8349\u83932\u65a4", "\u5f53\u5929\u91c7\u6458\uff0c\u987a\u4e30\u7a7a\u8fd0"));
            }
            if ("\u6a31\u6843".equals(query.getKeyword())) {
                return Collections.singletonList(snapshot(259L, "\u70df\u53f0\u5927\u6a31\u68432\u65a4", "\u5f53\u5b63\u73b0\u6458\uff0c\u51b0\u888b\u53d1\u8d27"));
            }
            if ("\u82f9\u679c".equals(query.getKeyword())) {
                ArrayList<ProductSearchSnapshot> snapshots = new ArrayList<ProductSearchSnapshot>();
                snapshots.add(snapshot(285L, "\u82f9\u679c\u624b\u673a", null));
                snapshots.add(snapshot(284L, "\u82f9\u679c\u624b\u673a", null));
                snapshots.add(snapshot(255L, "\u65b0\u7586\u963f\u514b\u82cf\u51b0\u7cd6\u5fc3\u82f9\u679c", "10\u65a4\u88c5\uff0c\u8106\u751c"));
                snapshots.add(snapshot(229L, "\u70df\u53f0\u7ea2\u5bcc\u58eb\u82f9\u679c20\u65a4", "\u81ea\u5bb6\u679c\u56ed\u73b0\u6458\u73b0\u53d1"));
                return snapshots;
            }
            return Collections.emptyList();
        });

        SessionState state = agentRuntime.run(request("\u6709\u4ec0\u4e48\u6c34\u679c"), null);

        assertEquals(AnswerType.RECOMMENDATION, state.getFinalAnswer().getAnswerType());
        assertEquals("\u6c34\u679c", state.getIntermediateData().get("normalizedKeyword"));
        assertEquals(4, state.getFinalAnswer().getCards().size());
        for (FinalAnswer.EntityCard card : state.getFinalAnswer().getCards()) {
            assertFalse("\u82f9\u679c\u624b\u673a".equals(card.getTitle()));
        }
        assertTrue(state.getFinalAnswer().getAnswerText().contains("\u8349\u8393"));
        assertTrue(state.getFinalAnswer().getAnswerText().contains("\u82f9\u679c"));
    }

    @Test
    void runShouldExecuteLocalActivityListThroughReadRouteMatcher() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class)))
                .thenReturn(parsedIntent);
        when(apiRouteService.listEnabledRoutes("local_activity", "READ")).thenReturn(java.util.Arrays.asList(
                localActivityRoute(91L, "list", "查询活动列表", "/api/local-act/activities/list"),
                localActivityRoute(92L, "nearby", "查询附近活动", "/api/local-act/activities/nearby")
        ));
        BackendApiProxyService.InvocationResult invocationResult = new BackendApiProxyService.InvocationResult();
        invocationResult.setPresentationHint("direct_response");
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("code", 200);
        payload.put("message", "ok");
        payload.put("data", java.util.Collections.singletonList(new java.util.LinkedHashMap<String, Object>() {{
            put("title", "社区观影夜");
            put("locationText", "青年中心");
            put("status", "PUBLISHED");
        }}));
        invocationResult.setData(payload);
        when(backendApiProxyService.invoke(any(BackendApiProxyService.InvocationRequest.class), any())).thenReturn(invocationResult);

        SessionState state = agentRuntime.run(request("当前有哪些活动"), null);

        assertEquals(AnswerType.RECOMMENDATION, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getExecutionMeta().getCompletedNodes().contains("execute"));
        assertFalse(state.getExecutionMeta().getCompletedNodes().contains("action_review"));
        assertTrue(state.getFinalAnswer().getAnswerText().contains("社区观影夜"));
        assertTrue(state.getFinalAnswer().getComposerMeta().getUsedSources().contains("api_routes"));
        assertEquals("local_activity", state.getFinalAnswer().getComposerMeta().getMetadata().get("matchedRouteResource"));
    }

    @Test
    void runShouldClarifyWhenNearbyActivityQueryHasNoCoordinates() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        when(queryParserService.parse(any(String.class), any(java.util.List.class), any(SessionState.SessionContext.class), any(java.util.Map.class)))
                .thenReturn(parsedIntent);
        when(apiRouteService.listEnabledRoutes("local_activity", "READ")).thenReturn(java.util.Arrays.asList(
                localActivityRoute(91L, "list", "查询活动列表", "/api/local-act/activities/list"),
                localActivityRoute(92L, "nearby", "查询附近活动", "/api/local-act/activities/nearby")
        ));

        SessionState state = agentRuntime.run(request("我附近有什么活动"), null);

        assertEquals(AnswerType.CLARIFICATION, state.getFinalAnswer().getAnswerType());
        assertTrue(state.getFinalAnswer().getAnswerText().contains("当前位置坐标"));
    }

    private PythonSidecarModels.ActionReviewResponsePayload actionReviewResponse(String outcome,
                                                                                 PythonSidecarModels.PendingActionPayload pendingAction) {
        PythonSidecarModels.ActionReviewResponsePayload response = new PythonSidecarModels.ActionReviewResponsePayload();
        response.setHandled(true);
        response.setOutcome(outcome);
        response.setPendingAction(pendingAction);
        return response;
    }

    private PythonSidecarModels.PendingActionPayload pendingAction(String action, String operationType) {
        PythonSidecarModels.PendingActionPayload payload = new PythonSidecarModels.PendingActionPayload();
        payload.setResource("product");
        payload.setAction(action);
        payload.setOperationType(operationType);
        payload.setDisplayName("\u53d1\u5e03\u5546\u54c1");
        return payload;
    }

    private PythonSidecarModels.PendingActionPayload createPendingActionPayload() {
        PythonSidecarModels.PendingActionPayload payload = pendingAction("create", "CREATE");
        payload.getPayload().put("title", "\u82f9\u679c\u624b\u673a");
        payload.getPayload().put("price", "3");
        payload.getPayload().put("stockQuantity", 10);
        payload.getPayload().put("location", "\u4e00\u98df\u5802");
        payload.getPayload().put("categoryId", 15);
        payload.getPayload().put("imageUrls", Collections.singletonList("https://img.example.com/apple.jpg"));
        return payload;
    }

    private ParsedIntent realtimeIntent() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.REALTIME_QUERY);
        parsedIntent.setNeedRealtime(true);
        return parsedIntent;
    }

    private AgentChatRequest request(String content) {
        AgentChatRequest request = new AgentChatRequest();
        AgentChatMessage message = new AgentChatMessage();
        message.setRole("user");
        message.setContent(content);
        request.setMessages(Collections.singletonList(message));
        request.setUserId("u-1");
        return request;
    }

    private com.example.demo.demos.Agent.Entity.ApiRoute route(Long id,
                                                               String resource,
                                                               String action,
                                                               String operationType,
                                                               String description) {
        com.example.demo.demos.Agent.Entity.ApiRoute route = new com.example.demo.demos.Agent.Entity.ApiRoute();
        route.setId(id);
        route.setResource(resource);
        route.setAction(action);
        route.setOperationType(operationType);
        route.setDescription(description);
        route.setHttpMethod("POST");
        route.setPathTemplate("/api/products/" + action);
        route.setEnabled(1);
        return route;
    }

    private Category category(String id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private com.example.demo.demos.Agent.Entity.ApiRoute localActivityRoute(Long id,
                                                                            String action,
                                                                            String description,
                                                                            String path) {
        com.example.demo.demos.Agent.Entity.ApiRoute route = new com.example.demo.demos.Agent.Entity.ApiRoute();
        route.setId(id);
        route.setResource("local_activity");
        route.setAction(action);
        route.setOperationType("READ");
        route.setDescription(description);
        route.setHttpMethod("GET");
        route.setPathTemplate(path);
        route.setEnabled(1);
        return route;
    }

    private ProductSearchSnapshot snapshot(Long productId, String title, String summaryText) {
        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        snapshot.setProductId(productId);
        snapshot.setTitle(title);
        snapshot.setSummaryText(summaryText);
        snapshot.setSearchableStatus("searchable");
        return snapshot;
    }
}
