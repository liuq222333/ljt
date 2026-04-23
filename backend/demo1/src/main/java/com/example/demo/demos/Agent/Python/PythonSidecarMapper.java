package com.example.demo.demos.Agent.Python;

import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Entity.ApiRoute;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.CandidateSlots;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalResponse;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Runtime.ActionConversationStore;
import com.example.demo.demos.Agent.Runtime.SessionState;
import com.example.demo.demos.common.enums.AnswerType;
import com.example.demo.demos.common.enums.FollowUpType;
import com.example.demo.demos.common.enums.PlanType;
import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.common.enums.TaskType;
import com.example.demo.demos.common.enums.ToolName;
import com.example.demo.demos.common.schema.FinalAnswer;
import com.example.demo.demos.common.schema.ToolPlan;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.model.RealtimeResultItem;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PythonSidecarMapper {

    public PythonSidecarModels.QueryParserRequestPayload toQueryParserRequest(String currentMessage,
                                                                              List<AgentChatMessage> recentMessages) {
        return toQueryParserRequest(currentMessage, recentMessages, null, null);
    }

    public PythonSidecarModels.QueryParserRequestPayload toQueryParserRequest(String currentMessage,
                                                                              List<AgentChatMessage> recentMessages,
                                                                              SessionState.SessionContext sessionContext,
                                                                              Map<String, Object> userProfile) {
        PythonSidecarModels.QueryParserRequestPayload payload = new PythonSidecarModels.QueryParserRequestPayload();
        payload.setCurrentMessage(currentMessage == null ? "" : currentMessage);
        if (!CollectionUtils.isEmpty(recentMessages)) {
            List<PythonSidecarModels.AgentChatMessagePayload> messages = new ArrayList<PythonSidecarModels.AgentChatMessagePayload>();
            for (AgentChatMessage recentMessage : recentMessages) {
                if (recentMessage == null) {
                    continue;
                }
                AgentChatMessage normalized = recentMessage.normalize();
                PythonSidecarModels.AgentChatMessagePayload messagePayload = new PythonSidecarModels.AgentChatMessagePayload();
                messagePayload.setRole(normalized.getRole());
                messagePayload.setContent(normalized.getContent());
                messages.add(messagePayload);
            }
            payload.setRecentMessages(messages);
        }
        payload.setSessionContextSummary(toSessionContextPayload(sessionContext, null));
        payload.setUserProfile(userProfile == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(userProfile));
        return payload;
    }

    public PythonSidecarModels.ToolRouterRequestPayload toToolRouterRequest(ParsedIntent parsedIntent,
                                                                            String keywords,
                                                                            String entityType,
                                                                            boolean executionReady,
                                                                            List<String> missingRequiredSlots,
                                                                            List<String> validationErrors,
                                                                            SessionState.SessionContext sessionContext) {
        PythonSidecarModels.ToolRouterRequestPayload payload = new PythonSidecarModels.ToolRouterRequestPayload();
        payload.setParsedIntent(toParsedIntentPayload(parsedIntent));
        payload.setNormalizedParams(toNormalizedParamsPayload(parsedIntent, keywords, entityType, executionReady, missingRequiredSlots, validationErrors));
        payload.setSessionContext(toSessionContextPayload(sessionContext, entityType));
        return payload;
    }

    public PythonSidecarModels.ResponseComposerRequestPayload toResponseComposerRequest(ParsedIntent parsedIntent,
                                                                                        String keywords,
                                                                                        String entityType,
                                                                                        List<ProductSearchSnapshot> searchResults,
                                                                                        long searchTotal,
                                                                                        KnowledgeRetrievalResponse knowledgeResponse,
                                                                                        RealtimeQueryResponse realtimeResponse,
                                                                                        SessionState.SessionContext sessionContext,
                                                                                        SessionState.ExecutionMeta executionMeta,
                                                                                        boolean hasError,
                                                                                        String errorCode,
                                                                                        String errorMessage,
                                                                                        String failedNode) {
        PythonSidecarModels.ResponseComposerRequestPayload payload = new PythonSidecarModels.ResponseComposerRequestPayload();
        payload.setParsedIntent(toParsedIntentPayload(parsedIntent));
        payload.setNormalizedParams(toNormalizedParamsPayload(parsedIntent, keywords, entityType, true, Collections.<String>emptyList(), Collections.<String>emptyList()));
        payload.setSearchResults(toSearchResultsPayload(searchResults, searchTotal, entityType));
        payload.setRetrievedDocs(toRetrievedDocsPayload(knowledgeResponse));
        payload.setRealtimeResults(toRealtimeResultsPayload(realtimeResponse));
        payload.setSessionContext(toSessionContextPayload(sessionContext, entityType));
        payload.setExecutionMeta(toExecutionMetaPayload(executionMeta, hasError, errorCode, failedNode));
        payload.setErrorState(toErrorStatePayload(hasError, errorCode, errorMessage, failedNode));
        return payload;
    }

    public PythonSidecarModels.ActionReviewRequestPayload toActionReviewRequest(String currentMessage,
                                                                                ParsedIntent parsedIntent,
                                                                                ActionConversationStore.PendingAction pendingAction,
                                                                                List<ApiRoute> availableRoutes) {
        PythonSidecarModels.ActionReviewRequestPayload payload = new PythonSidecarModels.ActionReviewRequestPayload();
        payload.setCurrentMessage(currentMessage == null ? "" : currentMessage);
        payload.setParsedIntent(toParsedIntentPayload(parsedIntent));
        payload.setPendingAction(toPendingActionPayload(pendingAction));
        payload.setAvailableRoutes(toActionRoutePayloads(availableRoutes));
        return payload;
    }

    public ParsedIntent toParsedIntent(PythonSidecarModels.ParsedIntentPayload payload, String originalMessage) {
        if (payload == null || !StringUtils.hasText(payload.getTaskType())) {
            throw new PythonSidecarException("Python sidecar parse_intent payload is missing task_type");
        }

        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.fromCode(payload.getTaskType()));
        parsedIntent.setIntentConfidence(payload.getIntentConfidence() == null ? 0.0 : payload.getIntentConfidence());
        parsedIntent.setQueryText(StringUtils.hasText(payload.getQueryText()) ? payload.getQueryText() : (originalMessage == null ? "" : originalMessage));
        parsedIntent.setCandidateSlots(toCandidateSlots(payload.getCandidateSlots()));
        parsedIntent.setNeedRealtime(Boolean.TRUE.equals(payload.getNeedRealtime()));
        parsedIntent.setNeedExplanation(Boolean.TRUE.equals(payload.getNeedExplanation()));
        parsedIntent.setNeedRecommendation(Boolean.TRUE.equals(payload.getNeedRecommendation()));
        parsedIntent.setFollowUp(Boolean.TRUE.equals(payload.getFollowUp()));
        parsedIntent.setNegation(Boolean.TRUE.equals(payload.getNegation()));
        parsedIntent.setFollowUpType(FollowUpType.fromCode(payload.getFollowUpType()));
        parsedIntent.setNegatedEntities(payload.getNegatedEntities() == null
                ? Collections.<String>emptyList()
                : new ArrayList<String>(payload.getNegatedEntities()));
        return parsedIntent;
    }

    public ToolPlan toToolPlan(PythonSidecarModels.ToolPlanPayload payload) {
        if (payload == null || !StringUtils.hasText(payload.getPlanType())) {
            throw new PythonSidecarException("Python sidecar route_tools payload is missing plan_type");
        }
        ToolPlan toolPlan = new ToolPlan();
        toolPlan.setPlanId(payload.getPlanId());
        toolPlan.setPlanType(resolvePlanType(payload.getPlanType()));
        toolPlan.setExecutionMode(firstNonBlank(payload.getExecutionMode(), "single"));
        toolPlan.setRequiresClarification(Boolean.TRUE.equals(payload.getRequiresClarification()));
        toolPlan.setClarificationPrompt(payload.getClarificationPrompt());
        toolPlan.setRoutingReason(payload.getRoutingReason());
        toolPlan.setPlanConfidence(payload.getPlanConfidence() == null ? 0.0D : payload.getPlanConfidence());
        toolPlan.setMaxParallelTools(payload.getMaxParallelTools() == null ? 1 : payload.getMaxParallelTools());
        toolPlan.setSteps(toToolSteps(payload.getSteps()));
        if (payload.getFallbackPlan() != null) {
            toolPlan.setFallbackPlan(toToolPlan(payload.getFallbackPlan()));
        }
        return toolPlan;
    }

    public FinalAnswer toFinalAnswer(PythonSidecarModels.FinalAnswerPayload payload) {
        if (payload == null || !StringUtils.hasText(payload.getAnswerType())) {
            throw new PythonSidecarException("Python sidecar compose_response payload is missing answer_type");
        }
        FinalAnswer finalAnswer = new FinalAnswer();
        finalAnswer.setAnswerType(AnswerType.fromCode(payload.getAnswerType()));
        finalAnswer.setAnswerText(payload.getAnswerText());
        finalAnswer.setSummary(payload.getSummary());
        finalAnswer.setCards(toEntityCards(payload.getCards()));
        finalAnswer.setDisclaimers(copyList(payload.getDisclaimers()));
        finalAnswer.setCitations(toCitations(payload.getCitations()));
        finalAnswer.setNextActions(copyList(payload.getNextActions()));
        applyComposerMeta(finalAnswer, payload.getComposerMeta());
        finalAnswer.getComposerMeta().getMetadata().put("pythonDebugTrace", payload.getDebugTrace());
        return finalAnswer;
    }

    public ActionConversationStore.PendingAction toPendingAction(PythonSidecarModels.PendingActionPayload payload) {
        if (payload == null) {
            return null;
        }
        ActionConversationStore.PendingAction pendingAction = new ActionConversationStore.PendingAction();
        pendingAction.setSessionId(payload.getSessionId());
        pendingAction.setResource(payload.getResource());
        pendingAction.setAction(payload.getAction());
        pendingAction.setOperationType(payload.getOperationType());
        pendingAction.setDisplayName(payload.getDisplayName());
        pendingAction.setRouteId(payload.getRouteId());
        pendingAction.setRouteDescription(payload.getRouteDescription());
        pendingAction.setParams(copyMap(payload.getParams()));
        pendingAction.setPayload(copyMap(payload.getPayload()));
        pendingAction.setMissingFields(copyList(payload.getMissingFields()));
        pendingAction.setRouteKeywords(copyList(payload.getRouteKeywords()));
        pendingAction.setAwaitingConfirmation(Boolean.TRUE.equals(payload.getAwaitingConfirmation()));
        pendingAction.setOriginalText(payload.getOriginalText());
        return pendingAction;
    }

    private PythonSidecarModels.ParsedIntentPayload toParsedIntentPayload(ParsedIntent parsedIntent) {
        PythonSidecarModels.ParsedIntentPayload payload = new PythonSidecarModels.ParsedIntentPayload();
        if (parsedIntent == null) {
            payload.setTaskType(TaskType.CHITCHAT.getCode());
            payload.setQueryText("");
            payload.setCandidateSlots(new PythonSidecarModels.CandidateSlotsPayload());
            payload.setNegatedEntities(new ArrayList<String>());
            return payload;
        }
        payload.setTaskType(parsedIntent.getTaskType() == null ? TaskType.CHITCHAT.getCode() : parsedIntent.getTaskType().getCode());
        payload.setIntentConfidence(parsedIntent.getIntentConfidence());
        payload.setQueryText(parsedIntent.getQueryText());
        payload.setCandidateSlots(toCandidateSlotsPayload(parsedIntent.getCandidateSlots()));
        payload.setNeedRealtime(parsedIntent.isNeedRealtime());
        payload.setNeedExplanation(parsedIntent.isNeedExplanation());
        payload.setNeedRecommendation(parsedIntent.isNeedRecommendation());
        payload.setFollowUp(parsedIntent.isFollowUp());
        payload.setNegation(parsedIntent.isNegation());
        payload.setFollowUpType(parsedIntent.getFollowUpType() == null ? null : parsedIntent.getFollowUpType().getCode());
        payload.setNegatedEntities(parsedIntent.getNegatedEntities() == null
                ? new ArrayList<String>()
                : new ArrayList<String>(parsedIntent.getNegatedEntities()));
        return payload;
    }

    private PythonSidecarModels.NormalizedParamsPayload toNormalizedParamsPayload(ParsedIntent parsedIntent,
                                                                                  String keywords,
                                                                                  String entityType,
                                                                                  boolean executionReady,
                                                                                  List<String> missingRequiredSlots,
                                                                                  List<String> validationErrors) {
        PythonSidecarModels.NormalizedParamsPayload normalizedParams = new PythonSidecarModels.NormalizedParamsPayload();
        normalizedParams.setTaskType(parsedIntent == null || parsedIntent.getTaskType() == null
                ? TaskType.CHITCHAT.getCode() : parsedIntent.getTaskType().getCode());
        normalizedParams.setKeywords(keywords);
        normalizedParams.setEntityType(firstNonBlank(entityType,
                parsedIntent == null || parsedIntent.getCandidateSlots() == null ? null : parsedIntent.getCandidateSlots().getEntityType(),
                "product"));
        normalizedParams.setExecutionReady(executionReady);
        normalizedParams.setMissingRequiredSlots(copyList(missingRequiredSlots));
        normalizedParams.setValidationErrors(copyList(validationErrors));
        return normalizedParams;
    }

    private PythonSidecarModels.CandidateSlotsPayload toCandidateSlotsPayload(CandidateSlots candidateSlots) {
        PythonSidecarModels.CandidateSlotsPayload payload = new PythonSidecarModels.CandidateSlotsPayload();
        if (candidateSlots == null) {
            return payload;
        }
        payload.setKeyword(candidateSlots.getKeyword());
        payload.setCategoryText(candidateSlots.getCategoryText());
        payload.setCrowdTagText(candidateSlots.getCrowdTagText());
        payload.setSceneTagText(candidateSlots.getSceneTagText());
        payload.setCityText(candidateSlots.getCityText());
        payload.setDistrictText(candidateSlots.getDistrictText());
        payload.setLocationText(candidateSlots.getLocationText());
        payload.setPriceText(candidateSlots.getPriceText());
        payload.setDateText(candidateSlots.getDateText());
        payload.setSortText(candidateSlots.getSortText());
        payload.setEntityType(candidateSlots.getEntityType());
        payload.setEntityRef(candidateSlots.getEntityRef());
        return payload;
    }

    private PythonSidecarModels.SessionContextPayload toSessionContextPayload(SessionState.SessionContext sessionContext,
                                                                              String entityType) {
        PythonSidecarModels.SessionContextPayload payload = new PythonSidecarModels.SessionContextPayload();
        if (sessionContext == null) {
            payload.setFocusedEntityType(entityType);
            return payload;
        }
        payload.setConfirmedConstraints(sessionContext.getConfirmedConstraints() == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(sessionContext.getConfirmedConstraints()));
        payload.setPendingSlots(sessionContext.getPendingSlots() == null
                ? new ArrayList<String>()
                : new ArrayList<String>(sessionContext.getPendingSlots().keySet()));
        payload.setCandidateEntities(toCandidateEntities(sessionContext.getCandidateEntities()));
        payload.setFocusedEntityId(sessionContext.getFocusedEntityId());
        payload.setFocusedEntityType(entityType);
        payload.setLastSelectedEntityIds(copyList(sessionContext.getLastSelectedEntityIds()));
        payload.setDialogueTurnIndex(sessionContext.getDialogueTurnIndex() == null ? 0 : sessionContext.getDialogueTurnIndex());
        payload.setFollowUpContext(sessionContext.getFollowUpContext() == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(sessionContext.getFollowUpContext()));
        return payload;
    }

    private PythonSidecarModels.PendingActionPayload toPendingActionPayload(ActionConversationStore.PendingAction pendingAction) {
        if (pendingAction == null) {
            return null;
        }
        PythonSidecarModels.PendingActionPayload payload = new PythonSidecarModels.PendingActionPayload();
        payload.setSessionId(pendingAction.getSessionId());
        payload.setResource(pendingAction.getResource());
        payload.setAction(pendingAction.getAction());
        payload.setOperationType(pendingAction.getOperationType());
        payload.setDisplayName(pendingAction.getDisplayName());
        payload.setRouteId(pendingAction.getRouteId());
        payload.setRouteDescription(pendingAction.getRouteDescription());
        payload.setParams(copyMap(pendingAction.getParams()));
        payload.setPayload(copyMap(pendingAction.getPayload()));
        payload.setMissingFields(copyList(pendingAction.getMissingFields()));
        payload.setRouteKeywords(copyList(pendingAction.getRouteKeywords()));
        payload.setAwaitingConfirmation(pendingAction.isAwaitingConfirmation());
        payload.setOriginalText(pendingAction.getOriginalText());
        return payload;
    }

    private List<PythonSidecarModels.ActionRouteSummaryPayload> toActionRoutePayloads(List<ApiRoute> availableRoutes) {
        if (CollectionUtils.isEmpty(availableRoutes)) {
            return new ArrayList<PythonSidecarModels.ActionRouteSummaryPayload>();
        }
        List<PythonSidecarModels.ActionRouteSummaryPayload> payloads = new ArrayList<PythonSidecarModels.ActionRouteSummaryPayload>();
        for (ApiRoute availableRoute : availableRoutes) {
            if (availableRoute == null) {
                continue;
            }
            PythonSidecarModels.ActionRouteSummaryPayload payload = new PythonSidecarModels.ActionRouteSummaryPayload();
            payload.setResource(availableRoute.getResource());
            payload.setAction(availableRoute.getAction());
            payload.setOperationType(availableRoute.getOperationType());
            payload.setRouteId(availableRoute.getId());
            payload.setRouteDescription(availableRoute.getDescription());
            payloads.add(payload);
        }
        return payloads;
    }

    private PythonSidecarModels.SearchResultsPayload toSearchResultsPayload(List<ProductSearchSnapshot> searchResults,
                                                                            long searchTotal,
                                                                            String entityType) {
        PythonSidecarModels.SearchResultsPayload payload = new PythonSidecarModels.SearchResultsPayload();
        payload.setEntityType(firstNonBlank(entityType, "product"));
        payload.setSource("product_search_snapshot");
        payload.setTotal(Long.valueOf(searchTotal).intValue());
        payload.setReturned(searchResults == null ? 0 : searchResults.size());
        payload.setSearchStatus(searchResults == null ? "not_started" : (searchResults.isEmpty() ? "empty" : "success"));
        if (CollectionUtils.isEmpty(searchResults)) {
            return payload;
        }
        List<PythonSidecarModels.SearchItemPayload> items = new ArrayList<PythonSidecarModels.SearchItemPayload>();
        for (ProductSearchSnapshot snapshot : searchResults) {
            if (snapshot == null) {
                continue;
            }
            PythonSidecarModels.SearchItemPayload item = new PythonSidecarModels.SearchItemPayload();
            item.setProductId(snapshot.getProductId() == null ? null : String.valueOf(snapshot.getProductId()));
            item.setTitle(snapshot.getTitle());
            item.setSubtitle(firstNonBlank(snapshot.getSubtitle(), snapshot.getSummaryText()));
            item.setDisplayPrice(formatPrice(snapshot.getDisplayPrice() == null ? snapshot.getBasePrice() : snapshot.getDisplayPrice(), snapshot.getCurrency()));
            item.setStoreId(snapshot.getStoreId() == null ? null : String.valueOf(snapshot.getStoreId()));
            item.setStoreName(snapshot.getStoreName());
            item.setCoverImage(snapshot.getCoverImage());
            item.setRating(snapshot.getRating() == null ? null : snapshot.getRating().doubleValue());
            item.setSalesCount(snapshot.getSalesCount() == null ? null : snapshot.getSalesCount().intValue());
            item.setCityName(snapshot.getCityName());
            item.setDistrictName(snapshot.getDistrictName());
            item.setBusinessAreaName(snapshot.getBusinessAreaName());
            item.setDistanceM(snapshot.getDistanceKm() == null ? null : Long.valueOf(Math.round(snapshot.getDistanceKm() * 1000D)).intValue());
            item.setTagNames(splitTags(snapshot.getTagNames()));
            item.setRecommendScore(snapshot.getRecommendScore() == null ? null : snapshot.getRecommendScore().doubleValue());
            items.add(item);
        }
        payload.setItems(items);
        return payload;
    }

    private PythonSidecarModels.RetrievedDocsPayload toRetrievedDocsPayload(KnowledgeRetrievalResponse knowledgeResponse) {
        PythonSidecarModels.RetrievedDocsPayload payload = new PythonSidecarModels.RetrievedDocsPayload();
        if (knowledgeResponse == null) {
            return payload;
        }
        payload.setDocCount(knowledgeResponse.getHitCount());
        payload.setQueryVersion(knowledgeResponse.getQueryVersion());
        payload.setRetrievalStatus(CollectionUtils.isEmpty(knowledgeResponse.getItems()) ? "empty" : "success");
        List<PythonSidecarModels.KnowledgeDocPayload> items = new ArrayList<PythonSidecarModels.KnowledgeDocPayload>();
        for (KnowledgeBase item : knowledgeResponse.getItems()) {
            if (item == null) {
                continue;
            }
            PythonSidecarModels.KnowledgeDocPayload docPayload = new PythonSidecarModels.KnowledgeDocPayload();
            docPayload.setId(item.getId() == null ? null : String.valueOf(item.getId()));
            docPayload.setCategory(item.getCategory());
            docPayload.setTitle(item.getTitle());
            docPayload.setContent(firstNonBlank(item.getContent(), item.getSummary()));
            items.add(docPayload);
        }
        payload.setItems(items);
        return payload;
    }

    private PythonSidecarModels.RealtimeResultsPayload toRealtimeResultsPayload(RealtimeQueryResponse realtimeResponse) {
        PythonSidecarModels.RealtimeResultsPayload payload = new PythonSidecarModels.RealtimeResultsPayload();
        if (realtimeResponse == null) {
            return payload;
        }
        payload.setCheckedEntityCount(realtimeResponse.getItems() == null ? 0 : realtimeResponse.getItems().size());
        payload.setRealtimeStatus(realtimeResponse.getRealtimeStatus() == null ? RealtimeStatus.FAILED.getCode() : realtimeResponse.getRealtimeStatus().getCode());
        List<PythonSidecarModels.RealtimeEntityPayload> items = new ArrayList<PythonSidecarModels.RealtimeEntityPayload>();
        for (RealtimeResultItem item : realtimeResponse.getItems()) {
            if (item == null) {
                continue;
            }
            PythonSidecarModels.RealtimeEntityPayload entity = new PythonSidecarModels.RealtimeEntityPayload();
            entity.setEntityId(item.getEntityId());
            entity.setProductId(item.getEntityId());
            entity.setStatusText(firstNonBlank(item.getAvailabilityStatus(), item.getSellStatus(), item.getInventoryStatus()));
            entity.setConfirmLevel(Boolean.TRUE.equals(item.getBookable()) ? "high" : (item.isDegraded() ? "low" : "medium"));
            entity.getRawPayload().put("inventoryStatus", item.getInventoryStatus());
            entity.getRawPayload().put("inventoryCount", item.getInventoryCount());
            entity.getRawPayload().put("sellStatus", item.getSellStatus());
            entity.getRawPayload().put("availabilityStatus", item.getAvailabilityStatus());
            entity.getRawPayload().put("price", item.getPrice() == null ? null : item.getPrice().toPlainString());
            entity.getRawPayload().put("currency", item.getCurrency());
            entity.getRawPayload().put("source", item.getSource());
            items.add(entity);
            if (payload.getQueryTs() == null && item.getQueryTs() != null) {
                payload.setQueryTs(item.getQueryTs().toString());
            }
        }
        payload.setItems(items);
        return payload;
    }

    private PythonSidecarModels.ExecutionMetaPayload toExecutionMetaPayload(SessionState.ExecutionMeta executionMeta,
                                                                            boolean hasError,
                                                                            String errorCode,
                                                                            String failedNode) {
        PythonSidecarModels.ExecutionMetaPayload payload = new PythonSidecarModels.ExecutionMetaPayload();
        if (executionMeta == null) {
            payload.setDegraded(hasError);
            payload.setFallbackApplied(hasError);
            payload.setFallbackReason(errorCode);
            payload.setFallbackSourceStep(failedNode);
            return payload;
        }
        payload.setRequestId(executionMeta.getRequestId());
        payload.setTraceId(executionMeta.getTraceId());
        payload.setSessionId(executionMeta.getSessionId());
        payload.setDegraded(executionMeta.isDegraded() || hasError);
        payload.setRestoredFromCheckpoint(executionMeta.isRestoredFromCheckpoint());
        payload.setFallbackApplied(hasError);
        payload.setFallbackReason(errorCode);
        payload.setFallbackSourceStep(failedNode);
        return payload;
    }

    private PythonSidecarModels.ErrorStatePayload toErrorStatePayload(boolean hasError,
                                                                      String errorCode,
                                                                      String errorMessage,
                                                                      String failedNode) {
        PythonSidecarModels.ErrorStatePayload payload = new PythonSidecarModels.ErrorStatePayload();
        payload.setHasError(hasError);
        payload.setErrorCode(errorCode);
        payload.setErrorMessage(errorMessage);
        payload.setFailedNode(failedNode);
        payload.setRecoverable(true);
        return payload;
    }

    private List<ToolPlan.ToolStep> toToolSteps(List<PythonSidecarModels.ToolStepPayload> payloads) {
        if (CollectionUtils.isEmpty(payloads)) {
            return new ArrayList<ToolPlan.ToolStep>();
        }
        List<ToolPlan.ToolStep> steps = new ArrayList<ToolPlan.ToolStep>();
        for (PythonSidecarModels.ToolStepPayload payload : payloads) {
            if (payload == null) {
                continue;
            }
            ToolPlan.ToolStep step = new ToolPlan.ToolStep();
            step.setStepId(payload.getStepId());
            step.setToolName(resolveToolName(payload.getToolName()));
            step.setPurpose(payload.getPurpose());
            step.setInputRef(payload.getInputRef());
            step.setDependsOn(copyList(payload.getDependsOn()));
            step.setOutputKey(payload.getOutputKey());
            step.setOptional(Boolean.TRUE.equals(payload.getOptional()));
            step.setParams(payload.getParams());
            step.setTimeoutMs(payload.getTimeoutMs() == null ? 0 : payload.getTimeoutMs());
            steps.add(step);
        }
        return steps;
    }

    private CandidateSlots toCandidateSlots(PythonSidecarModels.CandidateSlotsPayload payload) {
        CandidateSlots candidateSlots = new CandidateSlots();
        if (payload == null) {
            return candidateSlots;
        }
        candidateSlots.setKeyword(payload.getKeyword());
        candidateSlots.setCategoryText(payload.getCategoryText());
        candidateSlots.setCrowdTagText(payload.getCrowdTagText());
        candidateSlots.setSceneTagText(payload.getSceneTagText());
        candidateSlots.setCityText(payload.getCityText());
        candidateSlots.setDistrictText(payload.getDistrictText());
        candidateSlots.setLocationText(payload.getLocationText());
        candidateSlots.setPriceText(payload.getPriceText());
        candidateSlots.setDateText(payload.getDateText());
        candidateSlots.setSortText(payload.getSortText());
        candidateSlots.setEntityType(payload.getEntityType());
        candidateSlots.setEntityRef(payload.getEntityRef());
        return candidateSlots;
    }

    private List<Map<String, Object>> toCandidateEntities(List<String> rawValues) {
        List<Map<String, Object>> candidates = new ArrayList<Map<String, Object>>();
        if (CollectionUtils.isEmpty(rawValues)) {
            return candidates;
        }
        for (String rawValue : rawValues) {
            if (!StringUtils.hasText(rawValue)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("label", rawValue);
            item.put("entity_id", rawValue);
            candidates.add(item);
        }
        return candidates;
    }

    private List<FinalAnswer.EntityCard> toEntityCards(List<PythonSidecarModels.EntityCardPayload> payloads) {
        List<FinalAnswer.EntityCard> cards = new ArrayList<FinalAnswer.EntityCard>();
        if (CollectionUtils.isEmpty(payloads)) {
            return cards;
        }
        for (PythonSidecarModels.EntityCardPayload payload : payloads) {
            if (payload == null) {
                continue;
            }
            FinalAnswer.EntityCard card = new FinalAnswer.EntityCard();
            card.setEntityId(payload.getEntityId());
            card.setEntityType(payload.getEntityType());
            card.setTitle(payload.getTitle());
            card.setSubtitle(payload.getSubtitle());
            card.setImageUrl(payload.getImageUrl());
            card.setPriceText(payload.getPriceText());
            card.setTags(copyList(payload.getTags()));
            card.setLocationText(payload.getLocationText());
            card.setRealtimeStatusText(payload.getRealtimeStatusText());
            card.setRecommendReason(payload.getRecommendReason());
            cards.add(card);
        }
        return cards;
    }

    private List<FinalAnswer.Citation> toCitations(List<PythonSidecarModels.CitationPayload> payloads) {
        List<FinalAnswer.Citation> citations = new ArrayList<FinalAnswer.Citation>();
        if (CollectionUtils.isEmpty(payloads)) {
            return citations;
        }
        for (PythonSidecarModels.CitationPayload payload : payloads) {
            if (payload == null) {
                continue;
            }
            FinalAnswer.Citation citation = new FinalAnswer.Citation();
            citation.setSourceType("python_composer");
            citation.setSourceId(firstNonBlank(payload.getDocId(), payload.getDocTitle()));
            citation.setDocId(payload.getDocId());
            citation.setDocTitle(payload.getDocTitle());
            citation.setTitle(payload.getDocTitle());
            citation.setSnippet(payload.getSnippet());
            citation.setConfidence(payload.getConfidence() == null ? 0.0D : payload.getConfidence());
            citations.add(citation);
        }
        return citations;
    }

    private void applyComposerMeta(FinalAnswer finalAnswer, Map<String, Object> composerMeta) {
        if (composerMeta == null) {
            return;
        }
        Object usedSources = composerMeta.get("used_sources");
        if (usedSources instanceof List<?>) {
            for (Object value : (List<?>) usedSources) {
                if (value != null) {
                    finalAnswer.getComposerMeta().getUsedSources().add(String.valueOf(value));
                }
            }
        }
        Object degraded = composerMeta.get("degraded");
        if (degraded instanceof Boolean) {
            finalAnswer.getComposerMeta().setDegraded((Boolean) degraded);
        }
        Object degradeReason = composerMeta.get("degrade_reason");
        if (degradeReason != null) {
            finalAnswer.getComposerMeta().setDegradeReason(String.valueOf(degradeReason));
        }
        finalAnswer.getComposerMeta().getMetadata().put("pythonComposerMeta", composerMeta);
    }

    private List<String> splitTags(String rawTags) {
        if (!StringUtils.hasText(rawTags)) {
            return new ArrayList<String>();
        }
        String[] parts = rawTags.split("[,?|]");
        List<String> result = new ArrayList<String>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                result.add(part.trim());
            }
        }
        return result;
    }

    private String formatPrice(BigDecimal price, String currency) {
        if (price == null) {
            return null;
        }
        return price.stripTrailingZeros().toPlainString() + (StringUtils.hasText(currency) ? " " + currency : "");
    }

    private PlanType resolvePlanType(String code) {
        PlanType planType = PlanType.fromCode(code);
        if (planType == PlanType.SINGLE_TOOL && !PlanType.SINGLE_TOOL.getCode().equalsIgnoreCase(code)) {
            throw new PythonSidecarException("Unknown Python tool plan type: " + code);
        }
        return planType;
    }

    private ToolName resolveToolName(String code) {
        ToolName toolName = ToolName.fromCode(code);
        if (toolName == null) {
            throw new PythonSidecarException("Unknown Python tool name: " + code);
        }
        return toolName;
    }

    private List<String> copyList(List<String> values) {
        return values == null ? new ArrayList<String>() : new ArrayList<String>(values);
    }

    private Map<String, Object> copyMap(Map<String, Object> values) {
        return values == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(values);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
