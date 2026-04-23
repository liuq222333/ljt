package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Config.AgentRouterPythonProperties;
import com.example.demo.demos.Agent.Config.AgentComposerPythonProperties;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Pojo.CandidateSlots;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalRequest;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalResponse;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Python.PythonSidecarException;
import com.example.demo.demos.Agent.Python.PythonResponseComposerClient;
import com.example.demo.demos.Agent.Python.PythonToolRouterClient;
import com.example.demo.demos.Agent.Service.KnowledgeSearchService;
import com.example.demo.demos.Agent.Service.QueryParserService;
import com.example.demo.demos.CommunityMarket.DTO.ProductNearbyDTO;
import com.example.demo.demos.CommunityMarket.Service.ProductsService;
import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.common.enums.TaskType;
import com.example.demo.demos.common.error.BizException;
import com.example.demo.demos.common.error.ErrorCode;
import com.example.demo.demos.common.schema.FinalAnswer;
import com.example.demo.demos.common.schema.ToolPlan;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.service.RealtimeQueryOrchestratorService;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import com.example.demo.demos.search.mapper.ProductSearchSnapshotMapper;
import com.example.demo.demos.search.model.ProductSearchQuery;
import com.example.demo.demos.search.service.ProductQueryExpansionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AgentRuntime {

    private static final Logger log = LoggerFactory.getLogger(AgentRuntime.class);

    private static final List<String> CHITCHAT_KEYWORDS = java.util.Arrays.asList(
            "你好", "您好", "hello", "hi", "hey", "谢谢", "感谢", "再见", "拜拜", "晚安", "早安", "嗨"
    );
    private static final List<String> SEARCH_HINT_KEYWORDS = java.util.Arrays.asList(
            "买", "想买", "找", "搜索", "推荐", "看看", "哪些", "什么", "有没有", "在卖", "出售"
    );
    private static final List<String> FRUIT_INTENT_KEYWORDS = java.util.Arrays.asList(
            "水果", "果蔬", "草莓", "樱桃", "车厘子", "苹果", "香蕉", "橙子", "蓝莓", "葡萄", "西瓜"
    );
    private static final List<String> FRUIT_EXPANSION_EXCLUDE_KEYWORDS = java.util.Arrays.asList(
            "手机", "电脑", "笔记本", "平板", "耳机", "手表", "数码", "运营商", "摄像", "相机", "镜头",
            "iphone", "ipad", "macbook", "watch"
    );

    private static final List<String> UNSUPPORTED_ACTION_KEYWORDS = java.util.Arrays.asList(
            "\u53d1\u5e03", "\u4e0a\u67b6", "\u521b\u5efa", "\u65b0\u589e",
            "\u4e0b\u5355", "\u52a0\u5165\u8d2d\u7269\u8f66", "\u8d2d\u4e70", "\u5220\u9664", "\u4fee\u6539"
    );
    private static final Pattern EXPLICIT_ENTITY_ID_PATTERN = Pattern.compile(
            "(?i)(?:product(?:\\s*id)?|item(?:\\s*id)?|\\u5546\\u54c1(?:\\s*(?:id|\\u7f16\\u53f7))?|id|#)\\s*[:：]?\\s*(\\d{1,18})"
    );
    private static final Pattern BUY_QUERY_PATTERN_V2 = Pattern.compile(
            "(?:\\u6211\\u60f3\\u8981|\\u6211\\u60f3\\u4e70|\\u6211\\u60f3|\\u6211\\u8981|\\u60f3\\u4e70|\\u4e70\\u70b9|\\u4e70\\u4e2a|\\u4e70\\u4e00\\u90e8|\\u4e70\\u4e00\\u4e9b|\\u4e70)(.+?)(?:\\u73b0\\u5728|\\u76ee\\u524d|\\u6709\\u4ec0\\u4e48|\\u8fd8\\u6709\\u4ec0\\u4e48|\\u54ea\\u4e9b|\\u5728\\u5356|\\u80fd\\u4e70|\\u53ef\\u4e70|\\u5417|\\u5462|\\u5427|$)"
    );
    private static final Pattern SEARCH_QUERY_PATTERN_V2 = Pattern.compile(
            "(?:\\u627e|\\u641c\\u7d22|\\u63a8\\u8350|\\u770b\\u770b|\\u67e5\\u4e00\\u4e0b|\\u67e5\\u67e5|\\u5e2e\\u6211\\u627e|\\u7ed9\\u6211\\u627e)(.+?)(?:\\u73b0\\u5728|\\u76ee\\u524d|\\u6709\\u4ec0\\u4e48|\\u8fd8\\u6709\\u4ec0\\u4e48|\\u54ea\\u4e9b|\\u5728\\u5356|\\u80fd\\u4e70|\\u53ef\\u4e70|\\u5417|\\u5462|\\u5427|$)"
    );
    private static final Pattern BUY_QUERY_PATTERN = Pattern.compile(
            "(?:我想要|我想买|我想|我要|想买|买点|买个|买一部|买一些|买)(.+?)(?:现在|目前|有什么|还有什么|哪些|在卖|能买|可买|吗|吧|呢|$)"
    );
    private static final Pattern SEARCH_QUERY_PATTERN = Pattern.compile(
            "(?:找|搜索|推荐|看看|查一下|查查|帮我找|给我找)(.+?)(?:现在|目前|有什么|还有什么|哪些|在卖|吗|吧|呢|$)"
    );
    private static final Pattern PRICE_RANGE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Pattern REALTIME_KEYWORD_PATTERN = Pattern.compile(
            "(?i)(realtime|real-time|live|availability|available|stock|inventory|price|status|now|still|can buy|\\u5b9e\\u65f6|\\u73b0\\u5728|\\u5f53\\u524d|\\u8fd8\\u80fd|\\u80fd\\u4e70|\\u6709\\u8d27|\\u5e93\\u5b58|\\u4ef7\\u683c|\\u72b6\\u6001)"
    );

    private final AgentCheckpointStore agentCheckpointStore;
    private final QueryParserService queryParserService;
    private final AgentRouterPythonProperties agentRouterPythonProperties;
    private final AgentComposerPythonProperties agentComposerPythonProperties;
    private final PythonToolRouterClient pythonToolRouterClient;
    private final PythonResponseComposerClient pythonResponseComposerClient;
    private final SidecarToolPlanAdapter sidecarToolPlanAdapter;
    private final RealtimeQueryOrchestratorService realtimeQueryOrchestratorService;
    private final ActionIntentReviewService actionIntentReviewService;
    private final RuntimeAnswerComposer runtimeAnswerComposer;
    private final ProductSearchSnapshotMapper productSearchSnapshotMapper;
    private final KnowledgeSearchService knowledgeSearchService;
    private final ProductsService productsService;
    private final ProductQueryExpansionService productQueryExpansionService;

    public AgentRuntime(AgentCheckpointStore agentCheckpointStore,
                        QueryParserService queryParserService,
                        AgentRouterPythonProperties agentRouterPythonProperties,
                        AgentComposerPythonProperties agentComposerPythonProperties,
                        PythonToolRouterClient pythonToolRouterClient,
                        PythonResponseComposerClient pythonResponseComposerClient,
                        SidecarToolPlanAdapter sidecarToolPlanAdapter,
                        RealtimeQueryOrchestratorService realtimeQueryOrchestratorService,
                        ActionIntentReviewService actionIntentReviewService,
                        RuntimeAnswerComposer runtimeAnswerComposer,
                        ProductSearchSnapshotMapper productSearchSnapshotMapper,
                        KnowledgeSearchService knowledgeSearchService,
                        ProductsService productsService,
                        ProductQueryExpansionService productQueryExpansionService) {
        this.agentCheckpointStore = agentCheckpointStore;
        this.queryParserService = queryParserService;
        this.agentRouterPythonProperties = agentRouterPythonProperties;
        this.agentComposerPythonProperties = agentComposerPythonProperties;
        this.pythonToolRouterClient = pythonToolRouterClient;
        this.pythonResponseComposerClient = pythonResponseComposerClient;
        this.sidecarToolPlanAdapter = sidecarToolPlanAdapter;
        this.realtimeQueryOrchestratorService = realtimeQueryOrchestratorService;
        this.actionIntentReviewService = actionIntentReviewService;
        this.runtimeAnswerComposer = runtimeAnswerComposer;
        this.productSearchSnapshotMapper = productSearchSnapshotMapper;
        this.knowledgeSearchService = knowledgeSearchService;
        this.productsService = productsService;
        this.productQueryExpansionService = productQueryExpansionService;
    }

    public SessionState run(AgentChatRequest request, String authorization) {
        long runtimeStart = System.currentTimeMillis();
        SessionState state = new SessionState();
        SessionState.ExecutionMeta executionMeta = state.getExecutionMeta();
        executionMeta.setSessionId(resolveSessionId(request));
        restoreSessionContext(state, executionMeta);
        bumpDialogueTurn(state.getSessionContext());
        executionMeta.setRequestId("req-" + UUID.randomUUID().toString().replace("-", ""));
        executionMeta.setTraceId("trace-" + UUID.randomUUID().toString().replace("-", ""));

        FinalAnswer.DebugTrace debugTrace = createDebugTrace(executionMeta);
        AgentChatMessage latestMessage = latestMessage(request);
        ParsedIntent parsedIntent = traceParseIntent(request, latestMessage, state, executionMeta, debugTrace);

        FinalAnswer finalAnswer;
        FinalAnswer actionAnswer = traceActionReview(request, latestMessage, parsedIntent, authorization, state, executionMeta, debugTrace);
        if (actionAnswer != null) {
            finalAnswer = actionAnswer;
        } else if (shouldRunRealtime(parsedIntent, latestMessage, state.getSessionContext())) {
            finalAnswer = traceRealtimeFlow(request, latestMessage, parsedIntent, state, executionMeta, debugTrace);
        } else {
            finalAnswer = traceStructuredFlow(request, latestMessage, parsedIntent, state, executionMeta, debugTrace);
        }

        executionMeta.setDurationMs(System.currentTimeMillis() - runtimeStart);
        runtimeAnswerComposer.finalizeAnswer(finalAnswer, state, executionMeta, parsedIntent, debugTrace);
        state.setFinalAnswer(finalAnswer);
        agentCheckpointStore.record(executionMeta.getSessionId(), state);
        return state;
    }

    private FinalAnswer traceActionReview(AgentChatRequest request,
                                          AgentChatMessage latestMessage,
                                          ParsedIntent parsedIntent,
                                          String authorization,
                                          SessionState state,
                                          SessionState.ExecutionMeta executionMeta,
                                          FinalAnswer.DebugTrace debugTrace) {
        long start = System.currentTimeMillis();
        FinalAnswer.NodeTrace nodeTrace = createNodeTrace("action_review", start);
        ActionIntentReviewService.ActionReviewResult reviewResult = actionIntentReviewService.review(
                executionMeta.getSessionId(),
                latestMessage,
                parsedIntent,
                request,
                authorization
        );
        if (!reviewResult.isHandled()) {
            nodeTrace.setOutputSummary("none");
            finishNodeTrace(debugTrace, nodeTrace);
            return null;
        }

        executionMeta.getCompletedNodes().add("action_review");
        ActionConversationStore.PendingAction pendingAction = reviewResult.getPendingAction();
        if (pendingAction != null) {
            state.getIntermediateData().put("actionResource", pendingAction.getResource());
            state.getIntermediateData().put("actionName", pendingAction.getAction());
            state.getIntermediateData().put("actionOperationType", pendingAction.getOperationType());
            state.getIntermediateData().put("actionMissingFields", pendingAction.getMissingFields());
        }
        nodeTrace.setInputSummary(latestMessage == null ? "empty" : latestMessage.getContent());
        nodeTrace.setOutputSummary(reviewResult.getOutcome().name().toLowerCase(Locale.ROOT));

        FinalAnswer finalAnswer;
        switch (reviewResult.getOutcome()) {
            case NEED_CLARIFICATION:
                finalAnswer = runtimeAnswerComposer.buildActionClarificationAnswer(reviewResult);
                break;
            case NEED_CONFIRMATION:
                finalAnswer = runtimeAnswerComposer.buildActionConfirmationAnswer(reviewResult);
                break;
            case CANCELLED:
                finalAnswer = runtimeAnswerComposer.buildActionCancelledAnswer(reviewResult);
                break;
            case EXECUTED:
                executionMeta.getCompletedNodes().add("action_execute");
                finalAnswer = runtimeAnswerComposer.buildActionExecutionAnswer(reviewResult);
                break;
            case FAILED:
                executionMeta.getCompletedNodes().add("action_execute");
                executionMeta.setDegraded(true);
                executionMeta.setFailedNode("action_execute");
                nodeTrace.setSuccess(false);
                nodeTrace.setErrorMessage("action_execute_failed");
                finalAnswer = runtimeAnswerComposer.buildActionExecutionAnswer(reviewResult);
                break;
            default:
                finishNodeTrace(debugTrace, nodeTrace);
                return null;
        }
        finishNodeTrace(debugTrace, nodeTrace);
        return finalAnswer;
    }

    private ParsedIntent traceParseIntent(AgentChatRequest request,
                                          AgentChatMessage latestMessage,
                                          SessionState state,
                                          SessionState.ExecutionMeta executionMeta,
                                          FinalAnswer.DebugTrace debugTrace) {
        long start = System.currentTimeMillis();
        FinalAnswer.NodeTrace nodeTrace = createNodeTrace("parse_intent", start);
        ParsedIntent parsedIntent = latestMessage == null
                ? new ParsedIntent()
                : queryParserService.parse(
                        latestMessage.getContent(),
                        historyMessages(request),
                        state.getSessionContext(),
                        request == null ? null : request.getUserProfile()
                );
        state.getIntermediateData().put("parsedTaskType", parsedIntent.getTaskType().getCode());
        state.getIntermediateData().put("needRealtime", parsedIntent.isNeedRealtime());
        executionMeta.getCompletedNodes().add("parse_intent");
        nodeTrace.setInputSummary(latestMessage == null ? "empty" : latestMessage.getContent());
        nodeTrace.setOutputSummary(parsedIntent.getTaskType().getCode());
        finishNodeTrace(debugTrace, nodeTrace);
        return parsedIntent;
    }

    private FinalAnswer traceStructuredFlow(AgentChatRequest request,
                                            AgentChatMessage latestMessage,
                                            ParsedIntent parsedIntent,
                                            SessionState state,
                                            SessionState.ExecutionMeta executionMeta,
                                            FinalAnswer.DebugTrace debugTrace) {
        RuntimeRequest normalized = traceNormalizeParams(request, latestMessage, parsedIntent, state, executionMeta, debugTrace);
        RuntimePlan plan = traceRouteTools(parsedIntent, normalized, state, executionMeta, debugTrace);
        RuntimeExecution execution = traceExecute(request, parsedIntent, normalized, plan, state, executionMeta, debugTrace);
        return traceComposeResponse(latestMessage, parsedIntent, normalized, plan, execution, state, executionMeta, debugTrace);
    }

    private RuntimeRequest traceNormalizeParams(AgentChatRequest request,
                                                AgentChatMessage latestMessage,
                                                ParsedIntent parsedIntent,
                                                SessionState state,
                                                SessionState.ExecutionMeta executionMeta,
                                                FinalAnswer.DebugTrace debugTrace) {
        long start = System.currentTimeMillis();
        FinalAnswer.NodeTrace nodeTrace = createNodeTrace("normalize_params", start);
        RuntimeRequest normalized = normalizeRequest(
                latestMessage,
                parsedIntent,
                state == null ? null : state.getSessionContext(),
                request == null ? null : request.getUserProfile()
        );
        executionMeta.getCompletedNodes().add("normalize_params");
        state.getIntermediateData().put("normalizedKeyword", normalized.productQuery.getKeyword());
        state.getIntermediateData().put("referencedProductId", normalized.referencedProductId);
        state.getIntermediateData().put("nearbyRequested", normalized.nearbyRequested);
        state.getIntermediateData().put("knowledgePurpose", normalized.knowledgeRequest.getPurpose());
        nodeTrace.setInputSummary(latestMessage == null ? "empty" : latestMessage.getContent());
        nodeTrace.setOutputSummary(normalized.describe());
        finishNodeTrace(debugTrace, nodeTrace);
        return normalized;
    }

    private RuntimePlan traceRouteTools(ParsedIntent parsedIntent,
                                        RuntimeRequest normalized,
                                        SessionState state,
                                        SessionState.ExecutionMeta executionMeta,
                                        FinalAnswer.DebugTrace debugTrace) {
        long start = System.currentTimeMillis();
        FinalAnswer.NodeTrace nodeTrace = createNodeTrace("route_tools", start);
        RuntimePlan plan = tryPythonRoutePlan(parsedIntent, normalized, state);
        if (plan == null) {
            plan = routePlan(parsedIntent, normalized);
        }
        plan = applyRoutingGuards(plan, normalized);
        adjustPlanForReferencedEntity(plan, normalized);
        executionMeta.getCompletedNodes().add("route_tools");
        state.getIntermediateData().put("routePlan", plan.planCode);
        state.getIntermediateData().put("planType", plan.planCode);
        state.getIntermediateData().put("routePlanSource", plan.planSource);
        state.getIntermediateData().put("routeRoutingReason", plan.routingReason);
        nodeTrace.setInputSummary(parsedIntent == null || parsedIntent.getTaskType() == null
                ? TaskType.CHITCHAT.getCode() : parsedIntent.getTaskType().getCode());
        nodeTrace.setOutputSummary(plan.planCode);
        finishNodeTrace(debugTrace, nodeTrace);
        return plan;
    }

    private RuntimeExecution traceExecute(AgentChatRequest request,
                                          ParsedIntent parsedIntent,
                                          RuntimeRequest normalized,
                                          RuntimePlan plan,
                                          SessionState state,
                                          SessionState.ExecutionMeta executionMeta,
                                          FinalAnswer.DebugTrace debugTrace) {
        long start = System.currentTimeMillis();
        FinalAnswer.NodeTrace nodeTrace = createNodeTrace("execute", start);
        RuntimeExecution execution = executePlan(request, parsedIntent, normalized, plan, state, executionMeta);
        executionMeta.getCompletedNodes().add("execute");
        state.getIntermediateData().put("searchHitCount", execution.searchTotal);
        state.getIntermediateData().put("knowledgeHitCount", execution.knowledgeResponse.getHitCount());
        if (execution.degraded) {
            executionMeta.setDegraded(true);
            if (!StringUtils.hasText(executionMeta.getErrorCode())) {
                executionMeta.setErrorCode(execution.degradeReason);
            }
        }
        nodeTrace.setOutputSummary(execution.describe());
        if (execution.degraded) {
            nodeTrace.setSuccess(false);
            nodeTrace.setErrorMessage(execution.degradeReason);
        }
        finishNodeTrace(debugTrace, nodeTrace);
        return execution;
    }

    private FinalAnswer traceComposeResponse(AgentChatMessage latestMessage,
                                             ParsedIntent parsedIntent,
                                             RuntimeRequest normalized,
                                             RuntimePlan plan,
                                             RuntimeExecution execution,
                                             SessionState state,
                                             SessionState.ExecutionMeta executionMeta,
                                             FinalAnswer.DebugTrace debugTrace) {
        long start = System.currentTimeMillis();
        FinalAnswer.NodeTrace nodeTrace = createNodeTrace("compose_response", start);
        FinalAnswer finalAnswer;
        if (shouldComposeChitchat(parsedIntent, latestMessage)) {
            finalAnswer = runtimeAnswerComposer.buildChitchatAnswer(latestMessage, parsedIntent);
        } else if (plan.requiresClarification) {
            finalAnswer = runtimeAnswerComposer.buildRoutingClarificationAnswer(plan.clarificationPrompt, parsedIntent);
        } else if (execution.requireRealtimeClarification) {
            finalAnswer = runtimeAnswerComposer.buildRealtimeClarification(parsedIntent);
        } else {
            FinalAnswer pythonFinalAnswer = tryPythonComposeResponse(parsedIntent, normalized, execution, state, executionMeta);
            if (pythonFinalAnswer != null) {
                finalAnswer = pythonFinalAnswer;
            } else if (execution.searchTotal > 0 && execution.realtimeResponse != null) {
            finalAnswer = runtimeAnswerComposer.buildSearchRealtimeAnswer(
                    execution.searchResults,
                    execution.searchTotal,
                    normalized.productQuery,
                    execution.realtimeResponse,
                    latestMessage,
                    parsedIntent
            );
            } else if (execution.searchTotal > 0 && execution.knowledgeResponse.getHitCount() > 0) {
            finalAnswer = runtimeAnswerComposer.buildSearchKnowledgeAnswer(
                    execution.searchResults,
                    execution.searchTotal,
                    normalized.productQuery,
                    execution.knowledgeResponse,
                    latestMessage,
                    parsedIntent
            );
            } else if (execution.searchTotal > 0) {
            finalAnswer = runtimeAnswerComposer.buildProductSearchAnswer(
                    execution.searchResults,
                    execution.searchTotal,
                    normalized.productQuery,
                    latestMessage,
                    parsedIntent
            );
            } else if (execution.knowledgeResponse.getHitCount() > 0) {
            finalAnswer = runtimeAnswerComposer.buildKnowledgeAnswer(
                    execution.knowledgeResponse,
                    latestMessage,
                    parsedIntent
            );
            } else if (isUnsupportedActionRequest(latestMessage)) {
            finalAnswer = runtimeAnswerComposer.buildUnsupportedActionAnswer(latestMessage, parsedIntent);
            } else {
            finalAnswer = runtimeAnswerComposer.buildNoResultAnswer(latestMessage, parsedIntent, plan.planCode, execution.degradeReason);
            }
        }
        executionMeta.getCompletedNodes().add("compose_response");
        finalAnswer.getComposerMeta().getMetadata().put("routePlan", plan.planCode);
        finalAnswer.getComposerMeta().getMetadata().put("routePlanSource", plan.planSource);
        finalAnswer.getComposerMeta().getMetadata().put("routeRoutingReason", plan.routingReason);
        finalAnswer.getComposerMeta().getMetadata().put("searchTotal", execution.searchTotal);
        finalAnswer.getComposerMeta().getMetadata().put("knowledgeHitCount", execution.knowledgeResponse.getHitCount());
        nodeTrace.setInputSummary(plan.planCode);
        nodeTrace.setOutputSummary(finalAnswer.getAnswerType().getCode());
        finishNodeTrace(debugTrace, nodeTrace);
        return finalAnswer;
    }

    private boolean shouldComposeChitchat(ParsedIntent parsedIntent, AgentChatMessage latestMessage) {
        if (parsedIntent != null && parsedIntent.getTaskType() == TaskType.CHITCHAT) {
            return true;
        }
        if (latestMessage == null || !StringUtils.hasText(latestMessage.getContent())) {
            return false;
        }
        String text = latestMessage.getContent().trim().toLowerCase();
        if (text.length() > 8) {
            return false;
        }
        for (String keyword : CHITCHAT_KEYWORDS) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private RuntimeRequest normalizeRequest(AgentChatMessage latestMessage,
                                            ParsedIntent parsedIntent,
                                            SessionState.SessionContext sessionContext,
                                            Map<String, Object> userProfile) {
        RuntimeRequest normalized = new RuntimeRequest();
        String content = latestMessage == null ? "" : safeText(latestMessage.getContent());
        CandidateSlots slots = parsedIntent == null || parsedIntent.getCandidateSlots() == null
                ? new CandidateSlots() : parsedIntent.getCandidateSlots();
        slots = applySearchContextFallback(content, parsedIntent, slots, sessionContext);
        Long referencedProductId = resolveReferencedProductId(content, parsedIntent, sessionContext);
        String keyword = deriveSearchKeywordV2(content, slots);
        if (referencedProductId != null && parsedIntent != null && parsedIntent.isNeedRealtime()) {
            keyword = null;
        }
        if (StringUtils.hasText(keyword) && keyword.length() > 80) {
            keyword = keyword.substring(0, 80);
        }

        ProductSearchQuery productQuery = new ProductSearchQuery();
        productQuery.setKeyword(keyword);
        productQuery.setSearchableOnly(Boolean.TRUE);
        productQuery.setPage(1);
        productQuery.setSize(5);
        applyPriceFilter(productQuery, slots.getPriceText(), content);
        normalized.productQuery = productQuery;
        normalized.categoryHint = cleanSearchKeywordV2(slots.getCategoryText());
        normalized.referencedProductId = referencedProductId;
        normalized.nearbyRequested = isNearbyRequest(slots);
        normalized.userLatitude = parseDouble(userProfile, "latitude", "lat");
        normalized.userLongitude = parseDouble(userProfile, "longitude", "lng");
        normalized.nearbyRadiusKm = parseDouble(userProfile, "nearbyRadiusKm", "radiusKm");
        ProductQueryExpansionService.ExpansionPlan expansionPlan = productQueryExpansionService == null
                ? new ProductQueryExpansionService.ExpansionPlan()
                : productQueryExpansionService.expand(keyword, slots.getCategoryText());
        normalized.expandedKeywords.addAll(expansionPlan.getKeywords());
        normalized.expandedCategoryIds.addAll(expansionPlan.getCategoryIds());

        KnowledgeRetrievalRequest knowledgeRequest = new KnowledgeRetrievalRequest();
        knowledgeRequest.setQueryText(content);
        knowledgeRequest.setTaskType(parsedIntent == null || parsedIntent.getTaskType() == null
                ? TaskType.CHITCHAT.getCode() : parsedIntent.getTaskType().getCode());
        knowledgeRequest.setPurpose(resolveKnowledgePurpose(parsedIntent));
        knowledgeRequest.setEntityType(firstNonBlank(slots.getEntityType(), "product"));
        knowledgeRequest.setNeedRerank(true);
        knowledgeRequest.setTopK(3);
        if (referencedProductId != null) {
            knowledgeRequest.getEntityIds().add(String.valueOf(referencedProductId));
        }
        normalized.knowledgeRequest = knowledgeRequest;
        return normalized;
    }

    private CandidateSlots applySearchContextFallback(String content,
                                                      ParsedIntent parsedIntent,
                                                      CandidateSlots slots,
                                                      SessionState.SessionContext sessionContext) {
        CandidateSlots effectiveSlots = slots == null ? new CandidateSlots() : slots;
        if (!shouldReuseSearchContext(content, parsedIntent, effectiveSlots, sessionContext)) {
            return effectiveSlots;
        }
        if (!StringUtils.hasText(effectiveSlots.getKeyword())) {
            effectiveSlots.setKeyword(sessionConstraint(sessionContext, "keyword"));
        }
        if (!StringUtils.hasText(effectiveSlots.getCategoryText())) {
            effectiveSlots.setCategoryText(sessionConstraint(sessionContext, "categoryText"));
        }
        if (!StringUtils.hasText(effectiveSlots.getLocationText())) {
            effectiveSlots.setLocationText(sessionConstraint(sessionContext, "locationText"));
        }
        if (!StringUtils.hasText(effectiveSlots.getPriceText())) {
            effectiveSlots.setPriceText(sessionConstraint(sessionContext, "priceText"));
        }
        return effectiveSlots;
    }

    private boolean shouldReuseSearchContext(String content,
                                             ParsedIntent parsedIntent,
                                             CandidateSlots slots,
                                             SessionState.SessionContext sessionContext) {
        if (sessionContext == null || CollectionUtils.isEmpty(sessionContext.getConfirmedConstraints())) {
            return false;
        }
        TaskType taskType = parsedIntent == null || parsedIntent.getTaskType() == null
                ? TaskType.CHITCHAT : parsedIntent.getTaskType();
        if (!isSearchLikeTask(taskType)) {
            return false;
        }
        boolean followUp = parsedIntent != null && parsedIntent.isFollowUp();
        boolean missingKeyword = !StringUtils.hasText(deriveSearchKeywordV2(content, slots));
        return followUp || isGenericSearchFollowUpText(content) || missingKeyword;
    }

    private boolean isSearchLikeTask(TaskType taskType) {
        if (taskType == null) {
            return false;
        }
        switch (taskType) {
            case PRODUCT_SEARCH:
            case EVENT_SEARCH:
            case STORE_SEARCH:
            case FOLLOW_UP:
            case CLARIFICATION_RESPONSE:
            case MIXED_SEARCH_KNOWLEDGE:
            case MIXED_SEARCH_REALTIME:
            case REALTIME_QUERY:
                return true;
            default:
                return false;
        }
    }

    private boolean isGenericSearchFollowUpText(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        String normalized = content.trim().replaceAll("[，,。！？.!?、；;：:\\s]+", "");
        return "有什么".equals(normalized)
                || "都有什么".equals(normalized)
                || "都有哪些".equals(normalized)
                || "还有什么".equals(normalized)
                || "还有哪些".equals(normalized)
                || "还有啥".equals(normalized);
    }

    private String sessionConstraint(SessionState.SessionContext sessionContext, String key) {
        if (sessionContext == null
                || !StringUtils.hasText(key)
                || CollectionUtils.isEmpty(sessionContext.getConfirmedConstraints())) {
            return null;
        }
        Object value = sessionContext.getConfirmedConstraints().get(key);
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private RuntimePlan routePlan(ParsedIntent parsedIntent, RuntimeRequest normalized) {
        TaskType taskType = parsedIntent == null || parsedIntent.getTaskType() == null
                ? TaskType.CHITCHAT : parsedIntent.getTaskType();
        RuntimePlan plan = new RuntimePlan();
        plan.planSource = "java";
        plan.routingReason = "local_route_plan";
        if (normalized != null && normalized.nearbyRequested && !normalized.hasUserLocation()) {
            plan.requiresClarification = true;
            plan.clarificationPrompt = "要查附近的商品，我还需要你的定位或地址。";
            plan.planCode = "clarification";
            plan.routingReason = "nearby_location_missing";
            return plan;
        }
        switch (taskType) {
            case FAQ_QUERY:
                plan.runKnowledge = true;
                plan.planCode = "knowledge_only";
                break;
            case MIXED_SEARCH_KNOWLEDGE:
                plan.runSearch = true;
                plan.runKnowledge = true;
                plan.planCode = "search_then_knowledge";
                break;
            case PRODUCT_SEARCH:
            case EVENT_SEARCH:
            case STORE_SEARCH:
            case FOLLOW_UP:
            case CLARIFICATION_RESPONSE:
            case MIXED_SEARCH_REALTIME:
            case REALTIME_QUERY:
                plan.runSearch = true;
                plan.runKnowledge = parsedIntent != null && parsedIntent.isNeedExplanation();
                plan.runRealtime = parsedIntent != null && parsedIntent.isNeedRealtime();
                if (plan.runRealtime) {
                    plan.planCode = "search_then_realtime";
                } else {
                    plan.planCode = plan.runKnowledge ? "search_then_knowledge" : "search_only";
                }
                break;
            default:
                plan.planCode = "intent_only";
                break;
        }
        return plan;
    }

    private RuntimePlan tryPythonRoutePlan(ParsedIntent parsedIntent,
                                           RuntimeRequest normalized,
                                           SessionState state) {
        if (agentRouterPythonProperties == null || !agentRouterPythonProperties.isEnabled()) {
            return null;
        }
        try {
            List<String> missingRequiredSlots = determineMissingRequiredSlots(parsedIntent);
            ToolPlan toolPlan = pythonToolRouterClient.route(
                    parsedIntent,
                    normalized == null || normalized.productQuery == null ? null : normalized.productQuery.getKeyword(),
                    resolveEntityType(parsedIntent),
                    isExecutionReady(parsedIntent, normalized, missingRequiredSlots),
                    missingRequiredSlots,
                    Collections.<String>emptyList(),
                    state == null ? null : state.getSessionContext()
            );
            RuntimePlan runtimePlan = sidecarToolPlanAdapter.adapt(toolPlan);
            log.info("AgentRuntime route_tools used Python sidecar plan: {}", runtimePlan.planCode);
            return runtimePlan;
        } catch (PythonSidecarException ex) {
            log.warn("AgentRuntime route_tools Python sidecar failed, fallback to Java router: {}", ex.getMessage());
            return null;
        } catch (RuntimeException ex) {
            log.warn("AgentRuntime route_tools Python sidecar returned invalid result, fallback to Java router: {}", ex.getMessage());
            return null;
        }
    }

    private void adjustPlanForReferencedEntity(RuntimePlan plan, RuntimeRequest normalized) {
        if (plan == null || normalized == null || normalized.referencedProductId == null || plan.runKnowledge) {
            return;
        }
        plan.runSearch = true;
        if (!StringUtils.hasText(plan.planCode) || "intent_only".equals(plan.planCode)) {
            plan.planCode = "search_only";
        }
    }

    private RuntimePlan applyRoutingGuards(RuntimePlan plan, RuntimeRequest normalized) {
        if (normalized != null && normalized.nearbyRequested && !normalized.hasUserLocation()) {
            RuntimePlan guardedPlan = new RuntimePlan();
            guardedPlan.requiresClarification = true;
            guardedPlan.clarificationPrompt = "要查附近的商品，我还需要你的定位或地址。";
            guardedPlan.planCode = "clarification";
            guardedPlan.planSource = "java_guard";
            guardedPlan.routingReason = "nearby_location_missing";
            return guardedPlan;
        }
        return plan;
    }

    private List<String> determineMissingRequiredSlots(ParsedIntent parsedIntent) {
        List<String> missing = new ArrayList<String>();
        if (parsedIntent == null || parsedIntent.getTaskType() == null) {
            return missing;
        }
        if (parsedIntent.getTaskType() == TaskType.STORE_SEARCH) {
            CandidateSlots slots = parsedIntent.getCandidateSlots();
            String location = slots == null ? null : firstNonBlank(slots.getLocationText(), slots.getDistrictText(), slots.getCityText());
            if (!StringUtils.hasText(location)) {
                missing.add("location");
            }
        }
        return missing;
    }

    private boolean isExecutionReady(ParsedIntent parsedIntent,
                                     RuntimeRequest normalized,
                                     List<String> missingRequiredSlots) {
        if (!CollectionUtils.isEmpty(missingRequiredSlots)) {
            return false;
        }
        TaskType taskType = parsedIntent == null || parsedIntent.getTaskType() == null
                ? TaskType.CHITCHAT : parsedIntent.getTaskType();
        if (taskType == TaskType.FAQ_QUERY || taskType == TaskType.CHITCHAT) {
            return true;
        }
        if (normalized != null && normalized.referencedProductId != null) {
            return true;
        }
        return normalized != null
                && normalized.productQuery != null
                && StringUtils.hasText(normalized.productQuery.getKeyword());
    }

    private FinalAnswer tryPythonComposeResponse(ParsedIntent parsedIntent,
                                                 RuntimeRequest normalized,
                                                 RuntimeExecution execution,
                                                 SessionState state,
                                                 SessionState.ExecutionMeta executionMeta) {
        if (agentComposerPythonProperties == null || !agentComposerPythonProperties.isEnabled()) {
            return null;
        }
        try {
            FinalAnswer finalAnswer = pythonResponseComposerClient.compose(
                    parsedIntent,
                    normalized == null || normalized.productQuery == null ? null : normalized.productQuery.getKeyword(),
                    resolveEntityType(parsedIntent),
                    execution == null ? Collections.<ProductSearchSnapshot>emptyList() : execution.searchResults,
                    execution == null ? 0L : execution.searchTotal,
                    execution == null ? new KnowledgeRetrievalResponse() : execution.knowledgeResponse,
                    execution == null ? null : execution.realtimeResponse,
                    state == null ? null : state.getSessionContext(),
                    executionMeta,
                    execution != null && execution.degraded,
                    execution != null ? execution.degradeReason : null,
                    resolveComposeErrorMessage(state, execution),
                    executionMeta == null ? null : executionMeta.getFailedNode()
            );
            log.info("AgentRuntime compose_response used Python sidecar answer: {}", finalAnswer.getAnswerType());
            return finalAnswer;
        } catch (PythonSidecarException ex) {
            log.warn("AgentRuntime compose_response Python sidecar failed, fallback to Java composer: {}", ex.getMessage());
            return null;
        } catch (RuntimeException ex) {
            log.warn("AgentRuntime compose_response Python sidecar returned invalid result, fallback to Java composer: {}", ex.getMessage());
            return null;
        }
    }

    private String resolveComposeErrorMessage(SessionState state, RuntimeExecution execution) {
        if (state != null) {
            Object realtimeError = state.getIntermediateData().get("realtimeError");
            if (realtimeError != null) {
                return String.valueOf(realtimeError);
            }
            Object knowledgeError = state.getIntermediateData().get("knowledgeError");
            if (knowledgeError != null) {
                return String.valueOf(knowledgeError);
            }
            Object searchError = state.getIntermediateData().get("searchError");
            if (searchError != null) {
                return String.valueOf(searchError);
            }
        }
        return execution == null ? null : execution.degradeReason;
    }

    private RuntimeExecution executePlan(AgentChatRequest request,
                                         ParsedIntent parsedIntent,
                                         RuntimeRequest normalized,
                                         RuntimePlan plan,
                                         SessionState state,
                                         SessionState.ExecutionMeta executionMeta) {
        RuntimeExecution execution = new RuntimeExecution();
        if (plan.runSearch) {
            try {
                SearchBundle searchBundle = executeSearch(normalized);
                execution.searchResults.addAll(searchBundle.items);
                execution.searchTotal = searchBundle.total;
            } catch (RuntimeException ex) {
                execution.degraded = true;
                execution.degradeReason = "search_unavailable";
                executionMeta.setFailedNode("execute");
                state.getIntermediateData().put("searchError", ex.getMessage());
            }
        }
        if (plan.runKnowledge) {
            try {
                execution.knowledgeResponse = knowledgeSearchService.retrieve(normalized.knowledgeRequest);
            } catch (RuntimeException ex) {
                execution.degraded = true;
                if (!StringUtils.hasText(execution.degradeReason)) {
                    execution.degradeReason = "knowledge_unavailable";
                }
                executionMeta.setFailedNode("execute");
                state.getIntermediateData().put("knowledgeError", ex.getMessage());
            }
        }
        if (plan.runRealtime) {
            try {
                List<Long> realtimeEntityIds = resolveRealtimeEntityIds(normalized, execution.searchResults);
                if (realtimeEntityIds.isEmpty()) {
                    if (!plan.runSearch) {
                        execution.degraded = true;
                        if (!StringUtils.hasText(execution.degradeReason)) {
                            execution.degradeReason = "realtime_entity_missing";
                        }
                        execution.requireRealtimeClarification = true;
                    }
                } else {
                    execution.realtimeResponse = realtimeQueryOrchestratorService.query(
                            buildRealtimeRequest(request, executionMeta, parsedIntent, realtimeEntityIds)
                    );
                    execution.realtimeEntityIds.addAll(realtimeEntityIds);
                    state.getIntermediateData().put("realtimeStatus", execution.realtimeResponse.getRealtimeStatus().getCode());
                    state.getIntermediateData().put("realtimeQueryMeta", execution.realtimeResponse.getQueryMeta());
                    if (execution.realtimeResponse.getRealtimeStatus() != RealtimeStatus.SUCCESS) {
                        execution.degraded = true;
                        if (!StringUtils.hasText(execution.degradeReason)) {
                            execution.degradeReason = execution.realtimeResponse.getRealtimeStatus().getCode();
                        }
                    }
                }
            } catch (RuntimeException ex) {
                execution.degraded = true;
                if (!StringUtils.hasText(execution.degradeReason)) {
                    execution.degradeReason = "realtime_unavailable";
                }
                executionMeta.setFailedNode("execute");
                state.getIntermediateData().put("realtimeError", ex.getMessage());
            }
        }
        if (!plan.runSearch && !plan.runKnowledge && parsedIntent != null && parsedIntent.getTaskType() == TaskType.CHITCHAT) {
            execution.degradeReason = "no_executable_plan";
        }
        syncSessionContext(state.getSessionContext(), parsedIntent, normalized, execution);
        return execution;
    }

    private FinalAnswer traceRealtimeFlow(AgentChatRequest request,
                                          AgentChatMessage latestMessage,
                                          ParsedIntent parsedIntent,
                                          SessionState state,
                                          SessionState.ExecutionMeta executionMeta,
                                          FinalAnswer.DebugTrace debugTrace) {
        long start = System.currentTimeMillis();
        FinalAnswer.NodeTrace nodeTrace = createNodeTrace("realtime_query", start);
        executionMeta.getCompletedNodes().add("realtime_query");
        List<Long> entityIds = resolveRealtimeEntityIdsFromMessage(latestMessage, parsedIntent, state.getSessionContext());
        nodeTrace.setInputSummary(entityIds.toString());
        if (entityIds.isEmpty()) {
            nodeTrace.setOutputSummary("clarification");
            finishNodeTrace(debugTrace, nodeTrace);
            return runtimeAnswerComposer.buildRealtimeClarification(parsedIntent);
        }

        RealtimeQueryRequest realtimeRequest = buildRealtimeRequest(request, executionMeta, parsedIntent, entityIds);
        try {
            RealtimeQueryResponse realtimeResponse = realtimeQueryOrchestratorService.query(realtimeRequest);
            state.getIntermediateData().put("realtimeStatus", realtimeResponse.getRealtimeStatus().getCode());
            state.getIntermediateData().put("realtimeQueryMeta", realtimeResponse.getQueryMeta());
            rememberFocusedEntities(state.getSessionContext(), entityIds);
            executionMeta.setDegraded(realtimeResponse.getRealtimeStatus() != RealtimeStatus.SUCCESS);
            nodeTrace.setOutputSummary(realtimeResponse.getRealtimeStatus().getCode());
            finishNodeTrace(debugTrace, nodeTrace);
            return runtimeAnswerComposer.buildRealtimeAnswer(realtimeResponse, realtimeRequest);
        } catch (BizException ex) {
            markRealtimeFailure(state, executionMeta, String.valueOf(ex.getCode()), ex.getMessage());
            nodeTrace.setSuccess(false);
            nodeTrace.setErrorMessage(ex.getMessage());
            nodeTrace.setOutputSummary("partial_result");
            finishNodeTrace(debugTrace, nodeTrace);
            return runtimeAnswerComposer.buildRealtimeUnavailableAnswer(realtimeRequest, String.valueOf(ex.getCode()), ex.getMessage());
        } catch (RuntimeException ex) {
            markRealtimeFailure(state, executionMeta, String.valueOf(ErrorCode.REALTIME_UNAVAILABLE), ex.getMessage());
            nodeTrace.setSuccess(false);
            nodeTrace.setErrorMessage(ex.getMessage());
            nodeTrace.setOutputSummary("partial_result");
            finishNodeTrace(debugTrace, nodeTrace);
            return runtimeAnswerComposer.buildRealtimeUnavailableAnswer(realtimeRequest, String.valueOf(ErrorCode.REALTIME_UNAVAILABLE), ex.getMessage());
        }
    }

    private RealtimeQueryRequest buildRealtimeRequest(AgentChatRequest request,
                                                      SessionState.ExecutionMeta executionMeta,
                                                      ParsedIntent parsedIntent,
                                                      List<Long> entityIds) {
        RealtimeQueryRequest realtimeRequest = new RealtimeQueryRequest();
        realtimeRequest.setEntityType(resolveEntityType(parsedIntent));
        realtimeRequest.setEntityIds(entityIds);
        realtimeRequest.setQueryType("availability");
        realtimeRequest.setTraceId(executionMeta.getTraceId());
        realtimeRequest.setUserId(request == null ? null : request.getUserId());
        realtimeRequest.setForceRefresh(Boolean.TRUE.equals(request == null ? null : request.getDebug()));
        return realtimeRequest;
    }

    private void markRealtimeFailure(SessionState state,
                                     SessionState.ExecutionMeta executionMeta,
                                     String errorCode,
                                     String errorMessage) {
        executionMeta.setDegraded(true);
        executionMeta.setFailedNode("realtime_query");
        executionMeta.setErrorCode(errorCode);
        state.getIntermediateData().put("realtimeStatus", RealtimeStatus.FAILED.getCode());
        state.getIntermediateData().put("realtimeErrorCode", errorCode);
        state.getIntermediateData().put("realtimeErrorMessage", errorMessage);
    }

    private void restoreSessionContext(SessionState state, SessionState.ExecutionMeta executionMeta) {
        if (state == null || executionMeta == null || !StringUtils.hasText(executionMeta.getSessionId())) {
            return;
        }
        SessionState.SessionContext restoredContext =
                agentCheckpointStore.latestSessionContext(executionMeta.getSessionId());
        if (restoredContext == null) {
            return;
        }
        state.setSessionContext(restoredContext);
        executionMeta.setRestoredFromCheckpoint(true);
    }

    private void bumpDialogueTurn(SessionState.SessionContext sessionContext) {
        if (sessionContext == null) {
            return;
        }
        Integer currentTurn = sessionContext.getDialogueTurnIndex();
        sessionContext.setDialogueTurnIndex((currentTurn == null ? 0 : currentTurn) + 1);
    }

    private SearchBundle executeSearch(RuntimeRequest normalized) {
        SearchBundle bundle = new SearchBundle();
        if (normalized == null) {
            return bundle;
        }
        if (normalized.referencedProductId != null) {
            ProductSearchSnapshot snapshot = productSearchSnapshotMapper.selectByProductId(normalized.referencedProductId);
            if (snapshot != null) {
                appendSearchResult(bundle, snapshot);
                bundle.total = bundle.items.size();
                return bundle;
            }
        }
        if (normalized.nearbyRequested && normalized.hasUserLocation()) {
            searchNearby(normalized, bundle);
            if (!bundle.items.isEmpty()) {
                bundle.total = bundle.items.size();
                return bundle;
            }
        }
        searchSnapshot(normalized, bundle);
        bundle.total = bundle.items.size();
        return bundle;
    }

    private void searchNearby(RuntimeRequest normalized, SearchBundle bundle) {
        if (productsService == null || normalized == null || !normalized.hasUserLocation()) {
            return;
        }
        List<ProductSearchQuery> queries = buildSearchQueries(normalized);
        if (queries.isEmpty()) {
            queries.add(copyQuery(normalized.productQuery, normalized.productQuery == null ? null : normalized.productQuery.getKeyword(), null));
        }
        for (ProductSearchQuery query : queries) {
            int sizeBeforeQuery = bundle.items.size();
            ResponseEntity<List<ProductNearbyDTO>> response = productsService.getNearbyProducts(
                    normalized.userLatitude,
                    normalized.userLongitude,
                    normalized.nearbyRadiusKm,
                    query == null ? 5 : query.getSize(),
                    0,
                    query == null || query.getCategoryId() == null ? null : query.getCategoryId().intValue(),
                    query == null ? null : query.getMinPrice(),
                    query == null ? null : query.getMaxPrice(),
                    query == null ? null : query.getKeyword()
            );
            List<ProductNearbyDTO> nearbyItems = response == null ? null : response.getBody();
            if (CollectionUtils.isEmpty(nearbyItems)) {
                continue;
            }
            for (ProductNearbyDTO nearbyItem : nearbyItems) {
                ProductSearchSnapshot snapshot = toSnapshot(nearbyItem);
                if (!shouldAcceptSnapshot(normalized, query, snapshot)) {
                    continue;
                }
                appendSearchResult(bundle, snapshot);
                if (bundle.items.size() >= 5) {
                    return;
                }
            }
            if (bundle.items.size() > sizeBeforeQuery) {
                return;
            }
        }
    }

    private void searchSnapshot(RuntimeRequest normalized, SearchBundle bundle) {
        List<ProductSearchQuery> queries = buildSearchQueries(normalized);
        if (queries.isEmpty()) {
            queries.add(copyQuery(normalized.productQuery, normalized.productQuery == null ? null : normalized.productQuery.getKeyword(), null));
        }
        for (ProductSearchQuery query : queries) {
            List<ProductSearchSnapshot> snapshots = productSearchSnapshotMapper.searchForProducts(query);
            if (CollectionUtils.isEmpty(snapshots)) {
                continue;
            }
            for (ProductSearchSnapshot snapshot : snapshots) {
                if (!shouldAcceptSnapshot(normalized, query, snapshot)) {
                    continue;
                }
                appendSearchResult(bundle, snapshot);
                if (bundle.items.size() >= 5) {
                    return;
                }
            }
        }
    }

    private List<ProductSearchQuery> buildSearchQueries(RuntimeRequest normalized) {
        List<ProductSearchQuery> queries = new ArrayList<ProductSearchQuery>();
        if (normalized == null || normalized.productQuery == null) {
            return queries;
        }
        ProductSearchQuery baseQuery = normalized.productQuery;
        if (StringUtils.hasText(baseQuery.getKeyword())) {
            queries.add(copyQuery(baseQuery, baseQuery.getKeyword(), null));
        }
        if (!CollectionUtils.isEmpty(normalized.expandedCategoryIds)) {
            for (Long categoryId : normalized.expandedCategoryIds) {
                queries.add(copyQuery(baseQuery, null, categoryId));
            }
        }
        if (!CollectionUtils.isEmpty(normalized.expandedKeywords)) {
            for (String expandedKeyword : normalized.expandedKeywords) {
                if (!StringUtils.hasText(expandedKeyword)
                        || expandedKeyword.equals(baseQuery.getKeyword())) {
                    continue;
                }
                queries.add(copyQuery(baseQuery, expandedKeyword, null));
            }
        }
        return queries;
    }

    private ProductSearchQuery copyQuery(ProductSearchQuery source, String keyword, Long categoryId) {
        ProductSearchQuery query = new ProductSearchQuery();
        if (source != null) {
            query.setSearchableOnly(source.getSearchableOnly());
            query.setSortBy(source.getSortBy());
            query.setPage(source.getPage());
            query.setSize(source.getSize());
            query.setMinPrice(source.getMinPrice());
            query.setMaxPrice(source.getMaxPrice());
            query.setCityId(source.getCityId());
            query.setDistrictId(source.getDistrictId());
            query.setTagNames(source.getTagNames() == null
                    ? new ArrayList<String>()
                    : new ArrayList<String>(source.getTagNames()));
        }
        query.setKeyword(keyword);
        query.setCategoryId(categoryId);
        return query;
    }

    private boolean shouldAcceptSnapshot(RuntimeRequest normalized,
                                         ProductSearchQuery query,
                                         ProductSearchSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        if (!shouldApplyFruitExpansionGuard(normalized, query)) {
            return true;
        }
        String searchableText = buildSnapshotSearchableText(snapshot).toLowerCase(Locale.ROOT);
        return !containsAnyToken(searchableText, FRUIT_EXPANSION_EXCLUDE_KEYWORDS);
    }

    private boolean shouldApplyFruitExpansionGuard(RuntimeRequest normalized, ProductSearchQuery query) {
        if (normalized == null || normalized.productQuery == null || query == null) {
            return false;
        }
        if (!StringUtils.hasText(query.getKeyword())) {
            return false;
        }
        String baseKeyword = normalized.productQuery.getKeyword();
        if (!StringUtils.hasText(baseKeyword)) {
            return false;
        }
        if (query.getKeyword().equals(baseKeyword)) {
            return false;
        }
        String intentHint = firstNonBlank(normalized.categoryHint, baseKeyword);
        return isFruitIntent(intentHint);
    }

    private boolean isFruitIntent(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return containsAnyToken(normalized, FRUIT_INTENT_KEYWORDS);
    }

    private boolean containsAnyToken(String text, List<String> tokens) {
        if (!StringUtils.hasText(text) || CollectionUtils.isEmpty(tokens)) {
            return false;
        }
        for (String token : tokens) {
            if (StringUtils.hasText(token) && text.contains(token.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String buildSnapshotSearchableText(ProductSearchSnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        appendText(builder, snapshot.getTitle());
        appendText(builder, snapshot.getSubtitle());
        appendText(builder, snapshot.getSummaryText());
        appendText(builder, snapshot.getCategoryName());
        appendText(builder, snapshot.getTagNames());
        return builder.toString();
    }

    private void appendText(StringBuilder builder, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value.trim());
    }

    private void appendSearchResult(SearchBundle bundle, ProductSearchSnapshot snapshot) {
        if (bundle == null || snapshot == null || snapshot.getProductId() == null) {
            return;
        }
        for (ProductSearchSnapshot existing : bundle.items) {
            if (existing != null && snapshot.getProductId().equals(existing.getProductId())) {
                return;
            }
        }
        bundle.items.add(snapshot);
    }

    private ProductSearchSnapshot toSnapshot(ProductNearbyDTO nearbyItem) {
        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        if (nearbyItem == null) {
            return snapshot;
        }
        snapshot.setProductId(nearbyItem.getId());
        snapshot.setSellerId(nearbyItem.getSellerId() == null ? null : nearbyItem.getSellerId().longValue());
        snapshot.setCategoryId(nearbyItem.getCategoryId() == null ? null : nearbyItem.getCategoryId().longValue());
        snapshot.setTitle(nearbyItem.getTitle());
        snapshot.setSummaryText(nearbyItem.getDescription());
        snapshot.setBasePrice(nearbyItem.getPrice());
        snapshot.setDisplayPrice(nearbyItem.getPrice());
        snapshot.setCoverImage(nearbyItem.getImageUrls());
        snapshot.setDistanceKm(nearbyItem.getDistanceKm());
        snapshot.setSearchableStatus("searchable");
        return snapshot;
    }

    private void syncSessionContext(SessionState.SessionContext sessionContext,
                                    ParsedIntent parsedIntent,
                                    RuntimeRequest normalized,
                                    RuntimeExecution execution) {
        if (sessionContext == null) {
            return;
        }
        CandidateSlots slots = parsedIntent == null ? null : parsedIntent.getCandidateSlots();
        if (slots != null) {
            rememberConstraint(sessionContext, "keyword", normalized == null || normalized.productQuery == null ? null : normalized.productQuery.getKeyword());
            rememberConstraint(sessionContext, "categoryText", slots.getCategoryText());
            rememberConstraint(sessionContext, "locationText", slots.getLocationText());
            rememberConstraint(sessionContext, "priceText", slots.getPriceText());
        }
        if (normalized != null && normalized.referencedProductId != null) {
            rememberFocusedEntities(sessionContext, Collections.singletonList(normalized.referencedProductId));
        }
        if (execution != null && !CollectionUtils.isEmpty(execution.searchResults)) {
            List<Long> entityIds = new ArrayList<Long>();
            for (ProductSearchSnapshot snapshot : execution.searchResults) {
                if (snapshot != null && snapshot.getProductId() != null) {
                    entityIds.add(snapshot.getProductId());
                }
            }
            rememberFocusedEntities(sessionContext, entityIds);
            sessionContext.getFollowUpContext().put("lastSearchTotal", execution.searchTotal);
            if (normalized != null && normalized.nearbyRequested) {
                sessionContext.getFollowUpContext().put("lastSearchMode", "nearby");
            }
        }
    }

    private void rememberConstraint(SessionState.SessionContext sessionContext, String key, String value) {
        if (sessionContext == null || !StringUtils.hasText(key) || !StringUtils.hasText(value)) {
            return;
        }
        sessionContext.getConfirmedConstraints().put(key, value);
    }

    private void rememberFocusedEntities(SessionState.SessionContext sessionContext, List<Long> entityIds) {
        if (sessionContext == null || CollectionUtils.isEmpty(entityIds)) {
            return;
        }
        List<String> ids = new ArrayList<String>();
        for (Long entityId : entityIds) {
            if (entityId != null) {
                ids.add(String.valueOf(entityId));
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        sessionContext.setCandidateEntities(new ArrayList<String>(ids));
        sessionContext.setLastSelectedEntityIds(new ArrayList<String>(ids));
        sessionContext.setFocusedEntityId(ids.get(0));
    }

    private List<Long> resolveRealtimeEntityIdsFromMessage(AgentChatMessage latestMessage,
                                                           ParsedIntent parsedIntent,
                                                           SessionState.SessionContext sessionContext) {
        LinkedHashSet<Long> entityIds = new LinkedHashSet<Long>();
        if (latestMessage != null && StringUtils.hasText(latestMessage.getContent())) {
            entityIds.addAll(extractEntityIds(latestMessage.getContent()));
            Long referencedProductId = resolveReferencedProductId(latestMessage.getContent(), parsedIntent, sessionContext);
            if (referencedProductId != null) {
                entityIds.add(referencedProductId);
            }
        }
        return new ArrayList<Long>(entityIds);
    }

    private Long resolveReferencedProductId(String content,
                                            ParsedIntent parsedIntent,
                                            SessionState.SessionContext sessionContext) {
        Long explicitEntityId = firstEntityId(content);
        if (explicitEntityId != null) {
            return explicitEntityId;
        }
        CandidateSlots slots = parsedIntent == null ? null : parsedIntent.getCandidateSlots();
        if (slots != null && StringUtils.hasText(slots.getEntityRef())) {
            String resolvedEntityId = resolveEntityIdByReference(sessionContext, slots.getEntityRef());
            if (StringUtils.hasText(resolvedEntityId)) {
                try {
                    return Long.valueOf(resolvedEntityId);
                } catch (NumberFormatException ignore) {
                    return null;
                }
            }
        }
        String fallbackEntityId = resolveFocusedOrLastSelectedEntityId(sessionContext);
        if (StringUtils.hasText(fallbackEntityId) && parsedIntent != null && parsedIntent.isNeedRealtime()) {
            try {
                return Long.valueOf(fallbackEntityId);
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    private String resolveEntityIdByReference(SessionState.SessionContext sessionContext, String entityRef) {
        if (sessionContext == null || !StringUtils.hasText(entityRef)) {
            return null;
        }
        if (entityRef.contains("第一个")) {
            return resolveCandidateEntityId(sessionContext, 0);
        }
        if (entityRef.contains("第二个")) {
            return resolveCandidateEntityId(sessionContext, 1);
        }
        return resolveFocusedOrLastSelectedEntityId(sessionContext);
    }

    private String resolveFocusedOrLastSelectedEntityId(SessionState.SessionContext sessionContext) {
        if (sessionContext == null) {
            return null;
        }
        if (StringUtils.hasText(sessionContext.getFocusedEntityId())) {
            return sessionContext.getFocusedEntityId();
        }
        if (!CollectionUtils.isEmpty(sessionContext.getLastSelectedEntityIds())) {
            return sessionContext.getLastSelectedEntityIds().get(0);
        }
        return resolveCandidateEntityId(sessionContext, 0);
    }

    private String resolveCandidateEntityId(SessionState.SessionContext sessionContext, int index) {
        if (sessionContext == null
                || CollectionUtils.isEmpty(sessionContext.getCandidateEntities())
                || index < 0
                || index >= sessionContext.getCandidateEntities().size()) {
            return null;
        }
        return sessionContext.getCandidateEntities().get(index);
    }

    private boolean isNearbyRequest(CandidateSlots slots) {
        return slots != null && "附近".equals(slots.getLocationText());
    }

    private Double parseDouble(Map<String, Object> userProfile, String... keys) {
        if (userProfile == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = userProfile.get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            try {
                return Double.valueOf(String.valueOf(value));
            } catch (NumberFormatException ignore) {
                // Ignore malformed values and keep trying.
            }
        }
        return null;
    }

    private String resolveSessionId(AgentChatRequest request) {
        if (request != null && StringUtils.hasText(request.getSessionId())) {
            return request.getSessionId();
        }
        return "session-" + UUID.randomUUID().toString().replace("-", "");
    }

    private boolean shouldRunRealtime(ParsedIntent parsedIntent,
                                      AgentChatMessage latestMessage,
                                      SessionState.SessionContext sessionContext) {
        if (latestMessage == null || !StringUtils.hasText(latestMessage.getContent()) || parsedIntent == null) {
            return false;
        }
        String content = latestMessage.getContent();
        Long explicitEntityId = firstEntityId(content);
        if (explicitEntityId == null
                && parsedIntent.getTaskType() == TaskType.REALTIME_QUERY
                && resolveReferencedProductId(content, parsedIntent, sessionContext) == null) {
            return !hasSearchIntent(latestMessage);
        }
        if (explicitEntityId == null) {
            return false;
        }
        if (parsedIntent.getTaskType() == TaskType.REALTIME_QUERY
                || parsedIntent.getTaskType() == TaskType.MIXED_SEARCH_REALTIME
                || parsedIntent.isNeedRealtime()) {
            return true;
        }
        return REALTIME_KEYWORD_PATTERN.matcher(content).find();
    }

    private boolean hasSearchIntent(AgentChatMessage latestMessage) {
        if (latestMessage == null || !StringUtils.hasText(latestMessage.getContent())) {
            return false;
        }
        String text = latestMessage.getContent();
        for (String keyword : SEARCH_HINT_KEYWORDS) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnsupportedActionRequest(AgentChatMessage latestMessage) {
        if (latestMessage == null || !StringUtils.hasText(latestMessage.getContent())) {
            return false;
        }
        String text = latestMessage.getContent();
        for (String keyword : UNSUPPORTED_ACTION_KEYWORDS) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<Long> resolveRealtimeEntityIds(RuntimeRequest normalized, List<ProductSearchSnapshot> searchResults) {
        LinkedHashSet<Long> ids = new LinkedHashSet<Long>();
        if (normalized != null && normalized.referencedProductId != null) {
            ids.add(normalized.referencedProductId);
        }
        if (!CollectionUtils.isEmpty(searchResults)) {
            for (ProductSearchSnapshot snapshot : searchResults) {
                if (snapshot != null && snapshot.getProductId() != null) {
                    ids.add(snapshot.getProductId());
                }
                if (ids.size() >= 5) {
                    break;
                }
            }
        }
        return new ArrayList<Long>(ids);
    }

    private String resolveEntityType(ParsedIntent parsedIntent) {
        if (parsedIntent != null
                && parsedIntent.getCandidateSlots() != null
                && StringUtils.hasText(parsedIntent.getCandidateSlots().getEntityType())) {
            return parsedIntent.getCandidateSlots().getEntityType();
        }
        return "product";
    }

    private FinalAnswer.DebugTrace createDebugTrace(SessionState.ExecutionMeta executionMeta) {
        FinalAnswer.DebugTrace debugTrace = new FinalAnswer.DebugTrace();
        debugTrace.setRequestId(executionMeta.getRequestId());
        debugTrace.setTraceId(executionMeta.getTraceId());
        return debugTrace;
    }

    private FinalAnswer.NodeTrace createNodeTrace(String nodeName, long startTimeMs) {
        FinalAnswer.NodeTrace nodeTrace = new FinalAnswer.NodeTrace();
        nodeTrace.setNodeName(nodeName);
        nodeTrace.setStartTimeMs(startTimeMs);
        return nodeTrace;
    }

    private void finishNodeTrace(FinalAnswer.DebugTrace debugTrace, FinalAnswer.NodeTrace nodeTrace) {
        nodeTrace.setDurationMs(System.currentTimeMillis() - nodeTrace.getStartTimeMs());
        debugTrace.getNodeTraces().add(nodeTrace);
    }

    private AgentChatMessage latestMessage(AgentChatRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.getMessages())) {
            return null;
        }
        return request.getMessages().get(request.getMessages().size() - 1);
    }

    private List<AgentChatMessage> historyMessages(AgentChatRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.getMessages()) || request.getMessages().size() <= 1) {
            return Collections.emptyList();
        }
        return request.getMessages().subList(0, request.getMessages().size() - 1);
    }

    private List<Long> extractEntityIds(String content) {
        if (!StringUtils.hasText(content)) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<Long>();
        Matcher matcher = EXPLICIT_ENTITY_ID_PATTERN.matcher(content);
        while (matcher.find()) {
            try {
                ids.add(Long.valueOf(matcher.group(1)));
            } catch (NumberFormatException ignore) {
                // Ignore malformed IDs.
            }
        }
        return new ArrayList<Long>(ids);
    }

    private Long firstEntityId(String content) {
        List<Long> ids = extractEntityIds(content);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private void applyPriceFilter(ProductSearchQuery query, String priceText, String rawContent) {
        String candidate = firstNonBlank(priceText, rawContent);
        if (!StringUtils.hasText(candidate)) {
            return;
        }
        Matcher matcher = PRICE_RANGE_PATTERN.matcher(candidate);
        List<BigDecimal> values = new ArrayList<BigDecimal>();
        while (matcher.find()) {
            values.add(new BigDecimal(matcher.group(1)));
        }
        if (values.isEmpty()) {
            return;
        }
        String lowered = candidate.toLowerCase();
        if (lowered.contains("以内") || lowered.contains("以下") || lowered.contains("under") || lowered.contains("below")) {
            query.setMaxPrice(values.get(0));
            return;
        }
        if (lowered.contains("以上") || lowered.contains("不少于") || lowered.contains("over") || lowered.contains("above")) {
            query.setMinPrice(values.get(0));
            return;
        }
        if (values.size() >= 2) {
            query.setMinPrice(values.get(0));
            query.setMaxPrice(values.get(1));
        }
    }

    private String resolveKnowledgePurpose(ParsedIntent parsedIntent) {
        TaskType taskType = parsedIntent == null || parsedIntent.getTaskType() == null
                ? TaskType.CHITCHAT : parsedIntent.getTaskType();
        if (taskType == TaskType.FAQ_QUERY) {
            return "faq_direct";
        }
        if (taskType == TaskType.MIXED_SEARCH_KNOWLEDGE || parsedIntent != null && parsedIntent.isNeedExplanation()) {
            return "knowledge_enrichment";
        }
        return "context_support";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String deriveSearchKeyword(String content, CandidateSlots slots) {
        String structured = firstNonBlank(slots.getCategoryText(), slots.getKeyword());
        String extracted = extractSearchFocus(content);
        String candidate = firstNonBlank(extracted, structured, content);
        if (!StringUtils.hasText(candidate)) {
            return candidate;
        }
        String normalized = candidate
                .replace("现在", "")
                .replace("目前", "")
                .replace("有什么", "")
                .replace("还有什么", "")
                .replace("哪些", "")
                .replace("在卖", "")
                .replace("能买", "")
                .replace("可买", "")
                .replace("帮我", "")
                .replace("给我", "")
                .replace("我想要", "")
                .replace("我想买", "")
                .replace("我想", "")
                .replace("我要", "")
                .replace("想买", "")
                .replace("买点", "")
                .replace("买个", "")
                .replace("买一些", "")
                .replace("买一部", "")
                .replace("买", "")
                .replace("搜索", "")
                .replace("推荐", "")
                .replace("看看", "")
                .replace("查一下", "")
                .replace("查查", "")
                .replace("找", "")
                .replace("吗", "")
                .replace("吧", "")
                .replace("呢", "")
                .trim();
        normalized = normalized.replaceAll("^[，,。\\s]+", "").replaceAll("[，,。\\s]+$", "");
        return StringUtils.hasText(normalized) ? normalized : candidate;
    }

    private String extractSearchFocus(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String safe = content.trim();
        Matcher buyMatcher = BUY_QUERY_PATTERN.matcher(safe);
        if (buyMatcher.find()) {
            return buyMatcher.group(1).trim();
        }
        Matcher searchMatcher = SEARCH_QUERY_PATTERN.matcher(safe);
        if (searchMatcher.find()) {
            return searchMatcher.group(1).trim();
        }
        return null;
    }

    private String deriveSearchKeywordV2(String content, CandidateSlots slots) {
        String extracted = cleanSearchKeywordV2(extractSearchFocusV2(content));
        String categoryText = cleanSearchKeywordV2(slots == null ? null : slots.getCategoryText());
        String structuredKeyword = normalizeStructuredKeywordV2(content, slots == null ? null : slots.getKeyword());
        String plainKeyword = normalizePlainKeywordV2(content);
        return firstNonBlank(extracted, categoryText, structuredKeyword, plainKeyword);
    }

    private String extractSearchFocusV2(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String safe = content.trim();
        Matcher buyMatcher = BUY_QUERY_PATTERN_V2.matcher(safe);
        if (buyMatcher.find()) {
            return buyMatcher.group(1).trim();
        }
        Matcher searchMatcher = SEARCH_QUERY_PATTERN_V2.matcher(safe);
        if (searchMatcher.find()) {
            return searchMatcher.group(1).trim();
        }
        return null;
    }

    private String normalizeStructuredKeywordV2(String content, String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        String trimmed = candidate.trim();
        if (StringUtils.hasText(content) && trimmed.equals(content.trim())) {
            return null;
        }
        return cleanSearchKeywordV2(trimmed);
    }

    private String normalizePlainKeywordV2(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String trimmed = content.trim();
        if (containsSearchPhraseV2(trimmed)) {
            return null;
        }
        return cleanSearchKeywordV2(trimmed);
    }

    private boolean containsSearchPhraseV2(String content) {
        return BUY_QUERY_PATTERN_V2.matcher(content).find() || SEARCH_QUERY_PATTERN_V2.matcher(content).find();
    }

    private String cleanSearchKeywordV2(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        String normalized = candidate.trim().replaceAll("[，,。！？.!?、；;：:\\s]+", "");
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (isGenericSearchFollowUpPhraseV2(normalized)) {
            return null;
        }
        normalized = removeLeadingAffixesV2(normalized,
                "\u6211\u60f3\u8981", "\u6211\u60f3\u4e70", "\u6211\u60f3", "\u6211\u8981",
                "\u60f3\u4e70", "\u4e70\u70b9", "\u4e70\u4e2a", "\u4e70\u4e00\u4e9b",
                "\u4e70\u4e00\u90e8", "\u4e70", "\u5e2e\u6211", "\u7ed9\u6211", "\u627e",
                "\u641c\u7d22", "\u63a8\u8350", "\u770b\u770b", "\u67e5\u4e00\u4e0b", "\u67e5\u67e5");
        normalized = removeTrailingAffixesV2(normalized,
                "\u73b0\u5728", "\u76ee\u524d", "\u90fd\u6709\u4ec0\u4e48", "\u90fd\u6709\u54ea\u4e9b",
                "\u6709\u4ec0\u4e48", "\u8fd8\u6709\u4ec0\u4e48", "\u8fd8\u6709\u54ea\u4e9b", "\u8fd8\u6709\u5565",
                "\u54ea\u4e9b", "\u5728\u5356", "\u80fd\u4e70", "\u53ef\u4e70", "\u5417", "\u5462", "\u5427");
        normalized = normalized.replaceAll("^[\\-_/]+", "").replaceAll("[\\-_/]+$", "").trim();
        if (isGenericSearchFollowUpPhraseV2(normalized)) {
            return null;
        }
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private boolean isGenericSearchFollowUpPhraseV2(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return "\u6709\u4ec0\u4e48".equals(value)
                || "\u90fd\u6709\u4ec0\u4e48".equals(value)
                || "\u90fd\u6709\u54ea\u4e9b".equals(value)
                || "\u8fd8\u6709\u4ec0\u4e48".equals(value)
                || "\u8fd8\u6709\u54ea\u4e9b".equals(value)
                || "\u8fd8\u6709\u5565".equals(value)
                || "\u90fd".equals(value)
                || "\u8fd8\u6709".equals(value);
    }

    private String removeLeadingAffixesV2(String value, String... affixes) {
        String normalized = value;
        boolean changed;
        do {
            changed = false;
            for (String affix : affixes) {
                if (normalized.startsWith(affix)) {
                    normalized = normalized.substring(affix.length());
                    changed = true;
                }
            }
        } while (changed && StringUtils.hasText(normalized));
        return normalized;
    }

    private String removeTrailingAffixesV2(String value, String... affixes) {
        String normalized = value;
        boolean changed;
        do {
            changed = false;
            for (String affix : affixes) {
                if (normalized.endsWith(affix)) {
                    normalized = normalized.substring(0, normalized.length() - affix.length());
                    changed = true;
                }
            }
        } while (changed && StringUtils.hasText(normalized));
        return normalized;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class RuntimeRequest {
        private ProductSearchQuery productQuery = new ProductSearchQuery();
        private KnowledgeRetrievalRequest knowledgeRequest = new KnowledgeRetrievalRequest();
        private Long referencedProductId;
        private final List<String> expandedKeywords = new ArrayList<String>();
        private final List<Long> expandedCategoryIds = new ArrayList<Long>();
        private String categoryHint;
        private boolean nearbyRequested;
        private Double userLatitude;
        private Double userLongitude;
        private Double nearbyRadiusKm;

        private String describe() {
            return "keyword=" + (productQuery == null ? null : productQuery.getKeyword())
                    + ", productId=" + referencedProductId
                    + ", nearby=" + nearbyRequested
                    + ", knowledgePurpose=" + (knowledgeRequest == null ? null : knowledgeRequest.getPurpose());
        }

        private boolean hasUserLocation() {
            return userLatitude != null && userLongitude != null;
        }
    }

    private static final class SearchBundle {
        private final List<ProductSearchSnapshot> items = new ArrayList<ProductSearchSnapshot>();
        private long total;
    }

    static final class RuntimePlan {
        boolean runSearch;
        boolean runKnowledge;
        boolean runRealtime;
        String planCode = "intent_only";
        boolean requiresClarification;
        String clarificationPrompt;
        String planSource = "java";
        String routingReason;
    }

    private static final class RuntimeExecution {
        private final List<ProductSearchSnapshot> searchResults = new ArrayList<ProductSearchSnapshot>();
        private long searchTotal;
        private KnowledgeRetrievalResponse knowledgeResponse = new KnowledgeRetrievalResponse();
        private RealtimeQueryResponse realtimeResponse;
        private final List<Long> realtimeEntityIds = new ArrayList<Long>();
        private boolean requireRealtimeClarification;
        private boolean degraded;
        private String degradeReason;

        private String describe() {
            return "searchTotal=" + searchTotal
                    + ", knowledgeHits=" + (knowledgeResponse == null ? 0 : knowledgeResponse.getHitCount())
                    + ", realtimeStatus=" + (realtimeResponse == null || realtimeResponse.getRealtimeStatus() == null ? null : realtimeResponse.getRealtimeStatus().getCode())
                    + ", degraded=" + degraded;
        }
    }
}
