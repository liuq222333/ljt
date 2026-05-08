package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Config.AgentActionReviewPythonProperties;
import com.example.demo.demos.Agent.Entity.ApiRoute;
import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.CandidateSlots;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Python.PythonActionReviewClient;
import com.example.demo.demos.Agent.Python.PythonSidecarException;
import com.example.demo.demos.Agent.Python.PythonSidecarMapper;
import com.example.demo.demos.Agent.Python.PythonSidecarModels;
import com.example.demo.demos.Agent.Service.ApiRouteService;
import com.example.demo.demos.Agent.Service.BackendApiProxyService;
import com.example.demo.demos.CommunityMarket.Pojo.Category;
import com.example.demo.demos.CommunityMarket.Service.CategoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ActionIntentReviewService {

    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile(
            "(?i)(?:product(?:\\s*id)?|item(?:\\s*id)?|\\u5546\\u54c1(?:\\s*(?:id|\\u7f16\\u53f7))?|id|#)\\s*[:\\uff1a#]?\\s*(\\d{1,18})"
    );
    private static final Pattern PRODUCT_ID_SHORT_PATTERN = Pattern.compile(
            "(?:^|[^\\d])(\\d{1,18})\\s*(?:\\u53f7|#)?\\s*\\u5546\\u54c1"
    );
    private static final Pattern ENTITY_ID_VALUE_PATTERN = Pattern.compile("(\\d{1,18})");
    private static final Pattern CREATE_TITLE_PATTERN = Pattern.compile(
            "(?:\\u5546\\u54c1\\u540d(?:\\u79f0|\\u5b57)?|\\u6807\\u9898|\\u540d\\u4e3a|\\u53eb)\\s*(?:\\u662f|\\u4e3a|\\uff1a|:)?\\s*([^,\\uff0c.\\u3002;\\uff1b\\n]+)"
    );
    private static final Pattern CREATE_INLINE_TITLE_PATTERN = Pattern.compile(
            "(?:\\u53d1\\u5e03|\\u4e0a\\u67b6|\\u521b\\u5efa|\\u65b0\\u589e)(?:\\u4e00\\u4e2a|\\u4e00\\u4ef6|\\u4e2a)?([^,\\uff0c.\\u3002;\\uff1b\\n]+?)(?:\\u4ef7\\u683c|\\u5e93\\u5b58|\\u5730\\u70b9|\\u56fe\\u7247|\\u63cf\\u8ff0|$)"
    );
    private static final Pattern PRICE_LABEL_PATTERN = Pattern.compile(
            "(?:\\u4ef7\\u683c|\\u552e\\u4ef7)\\s*(?:\\u6539\\u6210|\\u6539\\u4e3a|\\u8c03\\u6574\\u4e3a|\\u8bbe\\u4e3a|\\u8bbe\\u7f6e\\u4e3a|\\u662f|\\u4e3a|\\uff1a|:)?\\s*(\\d+(?:\\.\\d+)?)"
    );
    private static final Pattern PRICE_ACTION_PATTERN = Pattern.compile(
            "(?:\\u6539\\u4ef7|\\u8c03\\u4ef7)\\s*(?:\\u4e3a|\\u5230|\\u6210|\\uff1a|:)?\\s*(\\d+(?:\\.\\d+)?)"
    );
    private static final Pattern PRICE_CURRENCY_PATTERN = Pattern.compile(
            "(?:\\u6539\\u6210|\\u6539\\u4e3a|\\u8c03\\u6574\\u4e3a|\\u8bbe\\u4e3a|\\u8bbe\\u7f6e\\u4e3a)?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:\\u5143|\\u5757|\\uffe5)"
    );
    private static final Pattern STOCK_PATTERN = Pattern.compile(
            "(?:\\u5e93\\u5b58|\\u6570\\u91cf|\\u8865\\u8d27|\\u589e\\u52a0\\u5e93\\u5b58|\\u52a0\\u5e93\\u5b58)\\s*(?:\\u662f|\\u4e3a|\\u589e\\u52a0|\\u8865|\\u5230|\\uff1a|:)?\\s*(\\d+)"
    );
    private static final Pattern PIECE_COUNT_PATTERN = Pattern.compile("(\\d+)\\s*(?:\\u4ef6|\\u4e2a)");
    private static final Pattern LOCATION_PATTERN = Pattern.compile(
            "(?:\\u5730\\u70b9|\\u4f4d\\u7f6e)\\s*(?:\\u6539\\u6210|\\u6539\\u4e3a|\\u8c03\\u6574\\u4e3a|\\u662f|\\u4e3a|\\uff1a|:)?\\s*([^,\\uff0c.\\u3002;\\uff1b\\n]+)"
    );
    private static final Pattern MOVE_LOCATION_PATTERN = Pattern.compile(
            "(?:\\u653e\\u5230|\\u79fb\\u5230|\\u6539\\u5230)\\s*([^,\\uff0c.\\u3002;\\uff1b\\n]+)"
    );
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile(
            "(?:\\u63cf\\u8ff0|\\u4ecb\\u7ecd)\\s*(?:\\u662f|\\u4e3a|\\uff1a|:)?\\s*([^\\n]+)"
    );
    private static final Pattern CONDITION_PATTERN = Pattern.compile(
            "(?:\\u6210\\u8272|\\u72b6\\u51b5|condition)\\s*(?:\\u662f|\\u4e3a|\\uff1a|:)?\\s*([^,\\uff0c.\\u3002;\\uff1b\\n]+)"
    );
    private static final Pattern CATEGORY_PATTERN = Pattern.compile(
            "(?:\\u5206\\u7c7b|category)\\s*(?:id)?\\s*(?:\\u662f|\\u4e3a|\\uff1a|:)?\\s*(\\d+)"
    );
    private static final Pattern CATEGORY_NAME_PATTERN = Pattern.compile(
            "(?:\\u5206\\u7c7b|\\u7c7b\\u76ee)\\s*(?:\\u662f|\\u4e3a|\\uff1a|:)?\\s*([^,\\uff0c.\\u3002;\\uff1b\\n]+)"
    );
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s,\\uff0c]+");
    private static final Pattern NUMBER_ONLY_PATTERN = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s*$");
    private static final Pattern INTEGER_ONLY_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*$");

    private static final List<String> CREATE_KEYWORDS = Arrays.asList(
            "\u53d1\u5e03", "\u4e0a\u67b6", "\u521b\u5efa", "\u65b0\u589e"
    );
    private static final List<String> TAKE_DOWN_KEYWORDS = Arrays.asList(
            "\u4e0b\u67b6", "\u64a4\u4e0b", "\u505c\u552e", "\u4e0b\u6389"
    );
    private static final List<String> UPDATE_PRICE_KEYWORDS = Arrays.asList(
            "\u6539\u4ef7", "\u4fee\u6539\u4ef7\u683c", "\u8c03\u6574\u4ef7\u683c", "\u4ef7\u683c\u6539\u6210", "\u552e\u4ef7\u6539\u6210"
    );
    private static final List<String> UPDATE_LOCATION_KEYWORDS = Arrays.asList(
            "\u4fee\u6539\u5730\u70b9", "\u8c03\u6574\u5730\u70b9", "\u6539\u5730\u70b9", "\u6539\u4f4d\u7f6e", "\u653e\u5230", "\u79fb\u5230", "\u6539\u5230"
    );
    private static final List<String> INCREASE_STOCK_KEYWORDS = Arrays.asList(
            "\u589e\u52a0\u5e93\u5b58", "\u52a0\u5e93\u5b58", "\u8865\u8d27", "\u5e93\u5b58\u589e\u52a0"
    );
    private static final List<String> CONFIRM_KEYWORDS = Arrays.asList(
            "\u786e\u8ba4", "\u786e\u8ba4\u6267\u884c", "\u6267\u884c", "\u63d0\u4ea4", "\u5c31\u8fd9\u6837", "\u53ef\u4ee5\u53d1\u5e03", "\u53ef\u4ee5\u6267\u884c"
    );
    private static final List<String> CANCEL_KEYWORDS = Arrays.asList(
            "\u53d6\u6d88", "\u4e0d\u7528\u4e86", "\u7b97\u4e86", "\u5148\u522b", "\u4e0d\u6267\u884c"
    );
    private static final List<String> PRODUCT_HINT_KEYWORDS = Arrays.asList(
            "\u5546\u54c1", "\u8d27", "\u5356", "\u53d1\u5e03", "\u4e0a\u67b6", "\u4e0b\u67b6"
    );
    private static final List<String> ACTIVITY_READ_QUERY_KEYWORDS = Arrays.asList(
            "\u67e5\u8be2", "\u67e5\u770b", "\u83b7\u53d6", "\u770b\u770b", "\u5217\u51fa", "\u6709\u54ea\u4e9b", "\u6709\u4ec0\u4e48",
            "\u6211\u7684\u6536\u85cf", "\u6211\u6536\u85cf\u7684", "\u6211\u6536\u85cf\u4e86", "\u6536\u85cf\u4e86\u54ea\u4e9b",
            "\u6211\u7684\u53d1\u5e03", "\u6211\u53d1\u5e03\u7684", "\u6211\u53d1\u5e03\u4e86", "\u6211\u7684\u62a5\u540d", "\u6211\u62a5\u540d\u7684", "\u62a5\u540d\u8bb0\u5f55"
    );
    private static final List<String> LEARNING_INTENT_KEYWORDS = Arrays.asList(
            "\u4e86\u89e3", "\u60f3\u77e5\u9053", "\u77e5\u9053\u4e00\u4e0b", "\u600e\u4e48", "\u5982\u4f55", "\u600e\u6837",
            "\u6559\u7a0b", "\u6d41\u7a0b", "\u89c4\u5219", "\u4ec0\u4e48\u610f\u601d"
    );
    private static final List<String> QUESTION_MARKERS = Arrays.asList(
            "\u5417", "\u5417?", "\u5417\uff1f", "\u5462", "\u5462?", "\u5462\uff1f", "?", "\uff1f"
    );
    private static final List<String> REQUEST_COMMAND_PREFIXES = Arrays.asList(
            "\u5e2e\u6211", "\u8bf7", "\u6211\u8981", "\u6211\u60f3", "\u7ed9\u6211", "\u9ebb\u70e6"
    );
    private static final List<String> REFERENCE_KEYWORDS = Arrays.asList(
            "\u8fd9\u4e2a", "\u90a3\u4e2a", "\u5b83", "\u8be5\u5546\u54c1", "\u4e0a\u4e00\u4e2a", "\u521a\u624d\u90a3\u4e2a",
            "\u521a\u624d\u7684", "\u521a\u624d", "\u9009\u4e2d\u7684", "\u7b2c\u4e00\u4e2a", "\u7b2c\u4e8c\u4e2a", "\u7b2c1\u4e2a", "\u7b2c2\u4e2a"
    );

    private final ApiRouteService apiRouteService;
    private final BackendApiProxyService backendApiProxyService;
    private final ActionConversationStore actionConversationStore;
    private final LocalActivityActionAdapter localActivityActionAdapter;
    private final CategoryService categoryService;
    private final AgentActionReviewPythonProperties agentActionReviewPythonProperties;
    private final PythonActionReviewClient pythonActionReviewClient;
    private final PythonSidecarMapper pythonSidecarMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ActionIntentReviewService(ApiRouteService apiRouteService,
                                     BackendApiProxyService backendApiProxyService,
                                     ActionConversationStore actionConversationStore,
                                     LocalActivityActionAdapter localActivityActionAdapter,
                                     CategoryService categoryService,
                                     AgentActionReviewPythonProperties agentActionReviewPythonProperties,
                                     PythonActionReviewClient pythonActionReviewClient,
                                     PythonSidecarMapper pythonSidecarMapper) {
        this.apiRouteService = apiRouteService;
        this.backendApiProxyService = backendApiProxyService;
        this.actionConversationStore = actionConversationStore;
        this.localActivityActionAdapter = localActivityActionAdapter;
        this.categoryService = categoryService;
        this.agentActionReviewPythonProperties = agentActionReviewPythonProperties;
        this.pythonActionReviewClient = pythonActionReviewClient;
        this.pythonSidecarMapper = pythonSidecarMapper;
    }

    public ActionReviewResult review(String sessionId,
                                     AgentChatMessage latestMessage,
                                     ParsedIntent parsedIntent,
                                     String authorization) {
        return review(sessionId, latestMessage, parsedIntent, null, authorization);
    }

    public ActionReviewResult review(String sessionId,
                                     AgentChatMessage latestMessage,
                                     ParsedIntent parsedIntent,
                                     AgentChatRequest request,
                                     String authorization) {
        return review(sessionId, latestMessage, parsedIntent, request, authorization, null);
    }

    public ActionReviewResult review(String sessionId,
                                     AgentChatMessage latestMessage,
                                     ParsedIntent parsedIntent,
                                     AgentChatRequest request,
                                     String authorization,
                                     SessionState.SessionContext sessionContext) {
        ActionReviewResult result = new ActionReviewResult();
        if (latestMessage == null || !StringUtils.hasText(latestMessage.getContent())) {
            return result;
        }
        String text = latestMessage.getContent().trim();
        ActionConversationStore.PendingAction pending = actionConversationStore.get(sessionId);
        if (shouldDropPendingForFreshQuery(text, parsedIntent, pending)) {
            actionConversationStore.clear(sessionId);
            pending = null;
        }
        ActionReviewResult localActivityResult = localActivityActionAdapter.review(
                sessionId,
                latestMessage,
                parsedIntent,
                request,
                authorization,
                pending
        );
        if (localActivityResult != null && localActivityResult.isHandled()) {
            return localActivityResult;
        }
        ActionSignal signal = detectActionSignal(text, parsedIntent);
        if (!shouldEnterReview(text, pending, signal)) {
            return result;
        }

        ActionReviewResult pythonResult = tryPythonReview(sessionId, text, parsedIntent, authorization, pending, signal, sessionContext);
        if (pythonResult != null) {
            return pythonResult;
        }

        return reviewLocal(sessionId, text, parsedIntent, authorization, pending, signal, sessionContext);
    }

    private boolean shouldEnterReview(String text,
                                      ActionConversationStore.PendingAction pending,
                                      ActionSignal signal) {
        if (pending != null && containsAny(text, CANCEL_KEYWORDS)) {
            return true;
        }
        if (pending == null && signal == ActionSignal.NONE) {
            return false;
        }
        return pending == null
                || signal != ActionSignal.NONE
                || containsAny(text, CONFIRM_KEYWORDS)
                || looksLikePendingSupplement(pending, text);
    }

    private boolean shouldDropPendingForFreshQuery(String text,
                                                   ParsedIntent parsedIntent,
                                                   ActionConversationStore.PendingAction pending) {
        if (pending == null || !StringUtils.hasText(text)) {
            return false;
        }
        if (containsAny(text, CANCEL_KEYWORDS) || containsAny(text, CONFIRM_KEYWORDS)) {
            return false;
        }
        if (looksLikePendingSupplement(pending, text)) {
            return false;
        }
        return looksLikeActivityReadQuery(text, parsedIntent);
    }

    private ActionReviewResult reviewLocal(String sessionId,
                                           String text,
                                           ParsedIntent parsedIntent,
                                           String authorization,
                                           ActionConversationStore.PendingAction pending,
                                           ActionSignal signal,
                                           SessionState.SessionContext sessionContext) {
        ActionReviewResult result = new ActionReviewResult();
        if (pending != null && containsAny(text, CANCEL_KEYWORDS)) {
            actionConversationStore.clear(sessionId);
            result.setHandled(true);
            result.setOutcome(ActionOutcome.CANCELLED);
            result.setPendingAction(pending.copy());
            return result;
        }

        ActionConversationStore.PendingAction working = pending == null ? null : pending.copy();
        if (signal != ActionSignal.NONE) {
            working = createPendingAction(sessionId, signal, text);
        }
        if (working == null) {
            return result;
        }

        mergeExtractedParams(working, text);
        applySessionProductReference(working, text, parsedIntent, sessionContext);
        recomputeMissingFields(working);
        result.setHandled(true);
        result.setPendingAction(working.copy());

        if (!CollectionUtils.isEmpty(working.getMissingFields())) {
            working.setAwaitingConfirmation(false);
            actionConversationStore.put(sessionId, working);
            result.setOutcome(ActionOutcome.NEED_CLARIFICATION);
            return result;
        }

        if (containsAny(text, CONFIRM_KEYWORDS)) {
            BackendApiProxyService.InvocationResult invocationResult = invoke(working, authorization);
            actionConversationStore.clear(sessionId);
            result.setInvocationResult(invocationResult);
            result.setOutcome(isInvocationSuccess(invocationResult) ? ActionOutcome.EXECUTED : ActionOutcome.FAILED);
            return result;
        }

        working.setAwaitingConfirmation(true);
        actionConversationStore.put(sessionId, working);
        result.setOutcome(ActionOutcome.NEED_CONFIRMATION);
        return result;
    }

    private ActionReviewResult tryPythonReview(String sessionId,
                                               String text,
                                               ParsedIntent parsedIntent,
                                               String authorization,
                                               ActionConversationStore.PendingAction pending,
                                               ActionSignal signal,
                                               SessionState.SessionContext sessionContext) {
        if (agentActionReviewPythonProperties == null
                || !agentActionReviewPythonProperties.isEnabled()
                || pythonActionReviewClient == null
                || pythonSidecarMapper == null) {
            return null;
        }
        try {
            List<ApiRoute> availableRoutes = resolveAvailableRoutes(pending, signal);
            PythonSidecarModels.ActionReviewResponsePayload response = pythonActionReviewClient.review(
                    text,
                    parsedIntent,
                    pending == null ? null : pending.copy(),
                    availableRoutes
            );
            if (response == null || !Boolean.TRUE.equals(response.getHandled())) {
                return null;
            }
            return applyPythonReviewResult(sessionId, text, parsedIntent, authorization, pending, signal, sessionContext, availableRoutes, response);
        } catch (PythonSidecarException ex) {
            return null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private ActionReviewResult applyPythonReviewResult(String sessionId,
                                                       String text,
                                                       ParsedIntent parsedIntent,
                                                       String authorization,
                                                       ActionConversationStore.PendingAction pending,
                                                       ActionSignal signal,
                                                       SessionState.SessionContext sessionContext,
                                                       List<ApiRoute> availableRoutes,
                                                       PythonSidecarModels.ActionReviewResponsePayload response) {
        String outcome = response.getOutcome() == null ? "" : response.getOutcome().trim().toLowerCase(Locale.ROOT);
        ActionConversationStore.PendingAction working = pythonSidecarMapper.toPendingAction(response.getPendingAction());
        if (working == null && pending != null) {
            working = pending.copy();
        }
        if ("cancelled".equals(outcome)) {
            actionConversationStore.clear(sessionId);
            ActionReviewResult result = new ActionReviewResult();
            result.setHandled(true);
            result.setOutcome(ActionOutcome.CANCELLED);
            result.setPendingAction(working);
            return result;
        }
        if (working == null) {
            return null;
        }

        working.setSessionId(sessionId);
        applyPendingDefaults(working, signal);
        mergeExtractedParams(working, text);
        applySessionProductReference(working, text, parsedIntent, sessionContext);
        ensureRoute(working, availableRoutes);
        recomputeMissingFields(working);

        ActionReviewResult result = new ActionReviewResult();
        result.setHandled(true);
        result.setPendingAction(working.copy());

        if ("ready_to_execute".equals(outcome)) {
            if (!CollectionUtils.isEmpty(working.getMissingFields())) {
                working.setAwaitingConfirmation(false);
                actionConversationStore.put(sessionId, working);
                result.setPendingAction(working.copy());
                result.setOutcome(ActionOutcome.NEED_CLARIFICATION);
                return result;
            }
            BackendApiProxyService.InvocationResult invocationResult = invoke(working, authorization);
            actionConversationStore.clear(sessionId);
            result.setInvocationResult(invocationResult);
            result.setOutcome(isInvocationSuccess(invocationResult) ? ActionOutcome.EXECUTED : ActionOutcome.FAILED);
            return result;
        }
        if ("need_confirmation".equals(outcome) && CollectionUtils.isEmpty(working.getMissingFields())) {
            working.setAwaitingConfirmation(true);
            actionConversationStore.put(sessionId, working);
            result.setPendingAction(working.copy());
            result.setOutcome(ActionOutcome.NEED_CONFIRMATION);
            return result;
        }
        if ("need_clarification".equals(outcome) || !CollectionUtils.isEmpty(working.getMissingFields())) {
            working.setAwaitingConfirmation(false);
            actionConversationStore.put(sessionId, working);
            result.setPendingAction(working.copy());
            result.setOutcome(ActionOutcome.NEED_CLARIFICATION);
            return result;
        }
        return null;
    }

    private boolean looksLikePendingSupplement(ActionConversationStore.PendingAction pendingAction, String text) {
        if (pendingAction == null || !StringUtils.hasText(text)) {
            return false;
        }
        if (canApplyShortReplyByMissingFields(pendingAction, text)) {
            return true;
        }
        if (!CollectionUtils.isEmpty(extractImageUrls(text))) {
            return true;
        }
        if (extractProductId(text) != null || extractPrice(text) != null || extractLocation(text) != null) {
            return true;
        }
        if ("create".equalsIgnoreCase(pendingAction.getAction())) {
            return StringUtils.hasText(extractTitle(text))
                    || extractCreateStock(text) != null
                    || extractCategoryId(text) != null
                    || StringUtils.hasText(extractDescription(text));
        }
        if ("increase_stock".equalsIgnoreCase(pendingAction.getAction())) {
            return extractDelta(text) != null;
        }
        return false;
    }

    private ActionConversationStore.PendingAction createPendingAction(String sessionId,
                                                                      ActionSignal signal,
                                                                      String text) {
        ActionConversationStore.PendingAction pendingAction = new ActionConversationStore.PendingAction();
        pendingAction.setSessionId(sessionId);
        pendingAction.setResource("product");
        pendingAction.setOriginalText(text);
        pendingAction.setOperationType(signal.getOperationType());
        pendingAction.setAction(signal.getPreferredAction());
        pendingAction.setDisplayName(signal.getDisplayName());
        pendingAction.setRouteKeywords(new ArrayList<String>(signal.getKeywords()));
        ApiRoute route = selectRoute("product", signal.getOperationType(), signal.getPreferredAction(), signal.getKeywords());
        if (route == null) {
            pendingAction.getMissingFields().add("\u53ef\u7528\u6267\u884c\u8def\u7531");
            pendingAction.setRouteDescription("\u5f53\u524d\u6570\u636e\u5e93\u6ca1\u6709\u53ef\u7528\u7684 product \u5199\u64cd\u4f5c route");
            return pendingAction;
        }
        pendingAction.setRouteId(route.getId());
        pendingAction.setAction(route.getAction());
        pendingAction.setOperationType(route.getOperationType());
        pendingAction.setRouteDescription(route.getDescription());
        return pendingAction;
    }

    private ApiRoute selectRoute(String resource,
                                 String operationType,
                                 String preferredAction,
                                 List<String> keywords) {
        return selectRoute(resolveRoutes(resource, operationType), operationType, preferredAction, keywords);
    }

    private ApiRoute selectRoute(List<ApiRoute> routes,
                                 String operationType,
                                 String preferredAction,
                                 List<String> keywords) {
        if (CollectionUtils.isEmpty(routes)) {
            return null;
        }
        ApiRoute best = null;
        double bestScore = -1D;
        for (ApiRoute route : routes) {
            double score = 0.0D;
            if (route == null) {
                continue;
            }
            if (StringUtils.hasText(preferredAction) && preferredAction.equalsIgnoreCase(route.getAction())) {
                score += 0.65D;
            }
            if (StringUtils.hasText(route.getDescription())) {
                String description = route.getDescription().toLowerCase(Locale.ROOT);
                for (String keyword : keywords) {
                    if (description.contains(keyword.toLowerCase(Locale.ROOT))) {
                        score += 0.12D;
                    }
                }
            }
            if (StringUtils.hasText(route.getOperationType()) && route.getOperationType().equalsIgnoreCase(operationType)) {
                score += 0.15D;
            }
            if (StringUtils.hasText(route.getPathTemplate()) && route.getPathTemplate().contains("/api/products/")) {
                score += 0.08D;
            }
            score += Math.min(Math.max(routePriority(route), 0), 100) / 1000.0D;
            if (score > bestScore) {
                bestScore = score;
                best = route;
            }
        }
        return bestScore >= 0.4D ? best : null;
    }

    private int routePriority(ApiRoute route) {
        return route == null || route.getMatchPriority() == null ? 0 : route.getMatchPriority();
    }

    private List<ApiRoute> resolveAvailableRoutes(ActionConversationStore.PendingAction pending,
                                                  ActionSignal signal) {
        String resource = pending != null && StringUtils.hasText(pending.getResource()) ? pending.getResource() : "product";
        List<ApiRoute> routes = new ArrayList<ApiRoute>();
        addRoutes(routes, resolveRoutes(resource, pending == null ? null : pending.getOperationType()));
        if (signal != null && signal != ActionSignal.NONE) {
            addRoutes(routes, resolveRoutes(resource, signal.getOperationType()));
        }
        if (routes.isEmpty()) {
            addRoutes(routes, resolveRoutes(resource, "CREATE"));
            addRoutes(routes, resolveRoutes(resource, "UPDATE"));
        }
        return routes;
    }

    private List<ApiRoute> resolveRoutes(String resource, String operationType) {
        try {
            if (StringUtils.hasText(operationType)) {
                List<ApiRoute> routes = apiRouteService.listEnabledRoutes(resource, operationType);
                return routes == null ? Collections.<ApiRoute>emptyList() : routes;
            }
            List<ApiRoute> routes = apiRouteService.listAllEnabledRoutes();
            if (CollectionUtils.isEmpty(routes)) {
                return Collections.emptyList();
            }
            List<ApiRoute> filtered = new ArrayList<ApiRoute>();
            for (ApiRoute route : routes) {
                if (route != null && resource.equalsIgnoreCase(route.getResource())) {
                    filtered.add(route);
                }
            }
            return filtered;
        } catch (Exception ignore) {
            return Collections.emptyList();
        }
    }

    private void addRoutes(List<ApiRoute> target, List<ApiRoute> candidates) {
        if (target == null || CollectionUtils.isEmpty(candidates)) {
            return;
        }
        for (ApiRoute candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            boolean exists = false;
            for (ApiRoute current : target) {
                if (current != null
                        && current.getId() != null
                        && current.getId().equals(candidate.getId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                target.add(candidate);
            }
        }
    }

    private void ensureRoute(ActionConversationStore.PendingAction pendingAction, List<ApiRoute> availableRoutes) {
        if (pendingAction == null) {
            return;
        }
        ApiRoute route = selectRoute(availableRoutes,
                pendingAction.getOperationType(),
                pendingAction.getAction(),
                pendingAction.getRouteKeywords());
        if (route == null && StringUtils.hasText(pendingAction.getResource())) {
            route = selectRoute(pendingAction.getResource(),
                    pendingAction.getOperationType(),
                    pendingAction.getAction(),
                    pendingAction.getRouteKeywords());
        }
        if (route != null) {
            pendingAction.setRouteId(route.getId());
            pendingAction.setRouteDescription(route.getDescription());
            pendingAction.setAction(route.getAction());
            pendingAction.setOperationType(route.getOperationType());
            if (!StringUtils.hasText(pendingAction.getResource())) {
                pendingAction.setResource(route.getResource());
            }
        }
    }

    private void applyPendingDefaults(ActionConversationStore.PendingAction pendingAction, ActionSignal signal) {
        if (pendingAction == null) {
            return;
        }
        if (!StringUtils.hasText(pendingAction.getResource())) {
            pendingAction.setResource("product");
        }
        if (!StringUtils.hasText(pendingAction.getAction()) && signal != null && signal != ActionSignal.NONE) {
            pendingAction.setAction(signal.getPreferredAction());
        }
        if (!StringUtils.hasText(pendingAction.getOperationType()) && signal != null && signal != ActionSignal.NONE) {
            pendingAction.setOperationType(signal.getOperationType());
        }
        if (!StringUtils.hasText(pendingAction.getDisplayName())) {
            pendingAction.setDisplayName(resolveDisplayName(pendingAction.getAction(), signal));
        }
        if (CollectionUtils.isEmpty(pendingAction.getRouteKeywords())) {
            List<String> keywords = new ArrayList<String>();
            if (signal != null && signal != ActionSignal.NONE && !CollectionUtils.isEmpty(signal.getKeywords())) {
                keywords.addAll(signal.getKeywords());
            } else if (StringUtils.hasText(pendingAction.getAction())) {
                keywords.add(pendingAction.getAction());
            }
            pendingAction.setRouteKeywords(keywords);
        }
    }

    private String resolveDisplayName(String action, ActionSignal signal) {
        if (signal != null && signal != ActionSignal.NONE) {
            return signal.getDisplayName();
        }
        if ("create".equalsIgnoreCase(action)) {
            return "\u53d1\u5e03\u5546\u54c1";
        }
        if ("take_down".equalsIgnoreCase(action)) {
            return "\u4e0b\u67b6\u5546\u54c1";
        }
        if ("update_price".equalsIgnoreCase(action)) {
            return "\u4fee\u6539\u5546\u54c1\u4ef7\u683c";
        }
        if ("update_location".equalsIgnoreCase(action)) {
            return "\u4fee\u6539\u5546\u54c1\u5730\u70b9";
        }
        if ("increase_stock".equalsIgnoreCase(action)) {
            return "\u589e\u52a0\u5546\u54c1\u5e93\u5b58";
        }
        return "\u5199\u64cd\u4f5c";
    }

    private boolean canApplyShortReplyByMissingFields(ActionConversationStore.PendingAction pendingAction, String text) {
        if (pendingAction == null || CollectionUtils.isEmpty(pendingAction.getMissingFields())) {
            return false;
        }
        String normalized = normalizeShortReply(text);
        if (!StringUtils.hasText(normalized) || looksLikeQuestionOrLearningIntent(normalized)) {
            return false;
        }
        if (pendingAction.getMissingFields().contains("\u56fe\u7247 URL") && !extractImageUrls(normalized).isEmpty()) {
            return true;
        }
        if (pendingAction.getMissingFields().contains("\u4ef7\u683c") && extractLooseDecimal(normalized) != null) {
            return true;
        }
        if ((pendingAction.getMissingFields().contains("\u5e93\u5b58")
                || pendingAction.getMissingFields().contains("\u589e\u52a0\u5e93\u5b58\u6570\u91cf"))
                && extractLooseInteger(normalized) != null) {
            return true;
        }
        if (pendingAction.getMissingFields().contains("\u5546\u54c1 ID")
                && (extractProductId(normalized) != null || extractLooseLong(normalized) != null)) {
            return true;
        }
        if (pendingAction.getMissingFields().contains("\u5546\u54c1\u5206\u7c7b")
                && (extractCategoryId(normalized) != null
                || matchCategoryIdByName(normalized) != null
                || inferCategoryIdFromText(normalized) != null)) {
            return true;
        }
        return pendingAction.getMissingFields().contains("\u5730\u70b9") && looksLikePlainFieldValue(normalized);
    }

    private void mergeShortReplyByMissingFields(ActionConversationStore.PendingAction pendingAction, String text) {
        if (pendingAction == null || CollectionUtils.isEmpty(pendingAction.getMissingFields())) {
            return;
        }
        String normalized = normalizeShortReply(text);
        if (!StringUtils.hasText(normalized) || looksLikeQuestionOrLearningIntent(normalized)) {
            return;
        }
        if ("create".equalsIgnoreCase(pendingAction.getAction())) {
            mergeCreateShortReply(pendingAction, normalized);
            return;
        }
        mergeUpdateShortReply(pendingAction, normalized);
    }

    private void mergeCreateShortReply(ActionConversationStore.PendingAction pendingAction, String text) {
        List<String> missing = pendingAction.getMissingFields();
        if (missing.contains("\u4ef7\u683c") && !pendingAction.getPayload().containsKey("price")) {
            BigDecimal price = extractLooseDecimal(text);
            if (price != null) {
                pendingAction.getPayload().put("price", price);
                return;
            }
        }
        if (missing.contains("\u5e93\u5b58") && !pendingAction.getPayload().containsKey("stockQuantity")) {
            Integer stockQuantity = extractLooseInteger(text);
            if (stockQuantity != null) {
                pendingAction.getPayload().put("stockQuantity", stockQuantity);
                return;
            }
        }
        if (missing.contains("\u5546\u54c1\u5206\u7c7b") && !pendingAction.getPayload().containsKey("categoryId")) {
            Integer categoryId = extractCategoryId(text);
            if (categoryId == null) {
                categoryId = matchCategoryIdByName(text);
            }
            if (categoryId == null) {
                categoryId = inferCategoryIdFromText(text);
            }
            if (categoryId != null) {
                pendingAction.getPayload().put("categoryId", categoryId);
                return;
            }
        }
        if (missing.contains("\u5730\u70b9") && !pendingAction.getPayload().containsKey("location")) {
            String location = extractLocation(text);
            if (!StringUtils.hasText(location) && looksLikePlainFieldValue(text)) {
                location = text;
            }
            if (StringUtils.hasText(location)) {
                pendingAction.getPayload().put("location", location);
                return;
            }
        }
        if (missing.contains("\u56fe\u7247 URL") && !pendingAction.getPayload().containsKey("imageUrls")) {
            List<String> imageUrls = extractImageUrls(text);
            if (!imageUrls.isEmpty()) {
                pendingAction.getPayload().put("imageUrls", toImagePayload(imageUrls));
            }
        }
    }

    private void mergeUpdateShortReply(ActionConversationStore.PendingAction pendingAction, String text) {
        List<String> missing = pendingAction.getMissingFields();
        if (missing.contains("\u5546\u54c1 ID") && !pendingAction.getParams().containsKey("productId")) {
            Long productId = extractProductId(text);
            if (productId == null && !missing.contains("\u4ef7\u683c") && !missing.contains("\u589e\u52a0\u5e93\u5b58\u6570\u91cf")) {
                productId = extractLooseLong(text);
            }
            if (productId != null) {
                pendingAction.getParams().put("productId", productId);
                return;
            }
        }
        if (missing.contains("\u4ef7\u683c") && !pendingAction.getParams().containsKey("price")) {
            BigDecimal price = extractLooseDecimal(text);
            if (price != null) {
                pendingAction.getParams().put("price", price);
                return;
            }
        }
        if (missing.contains("\u5730\u70b9") && !pendingAction.getParams().containsKey("location")) {
            String location = extractLocation(text);
            if (!StringUtils.hasText(location) && looksLikePlainFieldValue(text)) {
                location = text;
            }
            if (StringUtils.hasText(location)) {
                pendingAction.getParams().put("location", location);
                return;
            }
        }
        if (missing.contains("\u589e\u52a0\u5e93\u5b58\u6570\u91cf") && !pendingAction.getParams().containsKey("delta")) {
            Integer delta = extractLooseInteger(text);
            if (delta != null) {
                pendingAction.getParams().put("delta", delta);
            }
        }
    }

    private void mergeExtractedParams(ActionConversationStore.PendingAction pendingAction, String text) {
        mergeShortReplyByMissingFields(pendingAction, text);
        if ("create".equalsIgnoreCase(pendingAction.getAction())) {
            mergeCreatePayload(pendingAction, text);
            return;
        }
        Long productId = extractProductId(text);
        if (productId != null) {
            pendingAction.getParams().put("productId", productId);
        }
        if ("update_price".equalsIgnoreCase(pendingAction.getAction())) {
            BigDecimal price = extractPrice(text);
            if (price != null) {
                pendingAction.getParams().put("price", price);
            }
            return;
        }
        if ("update_location".equalsIgnoreCase(pendingAction.getAction())) {
            String location = extractLocation(text);
            if (StringUtils.hasText(location)) {
                pendingAction.getParams().put("location", location);
            }
            return;
        }
        if ("increase_stock".equalsIgnoreCase(pendingAction.getAction())) {
            Integer delta = extractDelta(text);
            if (delta != null) {
                pendingAction.getParams().put("delta", delta);
            }
        }
    }

    private void applySessionProductReference(ActionConversationStore.PendingAction pendingAction,
                                              String text,
                                              ParsedIntent parsedIntent,
                                              SessionState.SessionContext sessionContext) {
        if (pendingAction == null
                || "create".equalsIgnoreCase(pendingAction.getAction())
                || pendingAction.getParams().containsKey("productId")) {
            return;
        }
        Long productId = resolveSessionProductId(text, parsedIntent, sessionContext);
        if (productId != null) {
            pendingAction.getParams().put("productId", productId);
        }
    }

    private Long resolveSessionProductId(String text,
                                         ParsedIntent parsedIntent,
                                         SessionState.SessionContext sessionContext) {
        Long explicitProductId = extractProductId(text);
        if (explicitProductId != null) {
            return explicitProductId;
        }
        CandidateSlots slots = parsedIntent == null ? null : parsedIntent.getCandidateSlots();
        if (slots != null
                && StringUtils.hasText(slots.getEntityType())
                && !"product".equalsIgnoreCase(slots.getEntityType())) {
            return null;
        }
        if (slots != null && StringUtils.hasText(slots.getEntityRef())) {
            Long referencedId = parseEntityId(resolveEntityIdByReference(sessionContext, slots.getEntityRef()));
            if (referencedId != null) {
                return referencedId;
            }
        }
        if (!hasReferenceToPriorEntity(text)) {
            return null;
        }
        return parseEntityId(resolveEntityIdByReference(sessionContext, text));
    }

    private String resolveEntityIdByReference(SessionState.SessionContext sessionContext, String entityRef) {
        if (sessionContext == null || !StringUtils.hasText(entityRef)) {
            return null;
        }
        if (entityRef.contains("\u7b2c\u4e00\u4e2a") || entityRef.contains("\u7b2c1\u4e2a")) {
            return resolveCandidateEntityId(sessionContext, 0);
        }
        if (entityRef.contains("\u7b2c\u4e8c\u4e2a") || entityRef.contains("\u7b2c2\u4e2a")) {
            return resolveCandidateEntityId(sessionContext, 1);
        }
        Matcher numericIndexMatcher = Pattern.compile("\\u7b2c\\s*(\\d+)\\s*\\u4e2a").matcher(entityRef);
        if (numericIndexMatcher.find()) {
            try {
                return resolveCandidateEntityId(sessionContext, Integer.valueOf(numericIndexMatcher.group(1)) - 1);
            } catch (NumberFormatException ignore) {
                return null;
            }
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

    private Long parseEntityId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        Matcher matcher = ENTITY_ID_VALUE_PATTERN.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private void mergeCreatePayload(ActionConversationStore.PendingAction pendingAction, String text) {
        String title = extractTitle(text);
        if (StringUtils.hasText(title)) {
            pendingAction.getPayload().put("title", title);
        }
        BigDecimal price = extractPrice(text);
        if (price != null) {
            pendingAction.getPayload().put("price", price);
        }
        Integer stockQuantity = extractCreateStock(text);
        if (stockQuantity != null) {
            pendingAction.getPayload().put("stockQuantity", stockQuantity);
        }
        String location = extractLocation(text);
        if (StringUtils.hasText(location)) {
            pendingAction.getPayload().put("location", location);
        }
        String description = extractDescription(text);
        if (StringUtils.hasText(description)) {
            pendingAction.getPayload().put("description", description);
        }
        String condition = extractCondition(text);
        if (StringUtils.hasText(condition)) {
            pendingAction.getPayload().put("condition", condition);
        }
        Integer categoryId = extractCategoryId(text);
        if (categoryId == null) {
            categoryId = resolveCategoryId(text, pendingAction.getPayload());
        }
        if (categoryId != null) {
            pendingAction.getPayload().put("categoryId", categoryId);
        }
        List<String> imageUrls = extractImageUrls(text);
        if (!imageUrls.isEmpty()) {
            pendingAction.getPayload().put("imageUrls", toImagePayload(imageUrls));
        }
    }

    private void recomputeMissingFields(ActionConversationStore.PendingAction pendingAction) {
        List<String> missingFields = new ArrayList<String>();
        if (!StringUtils.hasText(pendingAction.getRouteDescription()) && pendingAction.getRouteId() == null) {
            missingFields.add("\u53ef\u7528\u6267\u884c\u8def\u7531");
        }
        if ("create".equalsIgnoreCase(pendingAction.getAction())) {
            requirePayloadField(pendingAction, missingFields, "title", "\u5546\u54c1\u540d\u79f0");
            requirePayloadField(pendingAction, missingFields, "price", "\u4ef7\u683c");
            requirePayloadField(pendingAction, missingFields, "stockQuantity", "\u5e93\u5b58");
            requirePayloadField(pendingAction, missingFields, "categoryId", "\u5546\u54c1\u5206\u7c7b");
            requirePayloadField(pendingAction, missingFields, "location", "\u5730\u70b9");
            requirePayloadField(pendingAction, missingFields, "imageUrls", "\u56fe\u7247 URL");
        } else {
            requireParamField(pendingAction, missingFields, "productId", "\u5546\u54c1 ID");
            if ("update_price".equalsIgnoreCase(pendingAction.getAction())) {
                requireParamField(pendingAction, missingFields, "price", "\u4ef7\u683c");
            } else if ("update_location".equalsIgnoreCase(pendingAction.getAction())) {
                requireParamField(pendingAction, missingFields, "location", "\u5730\u70b9");
            } else if ("increase_stock".equalsIgnoreCase(pendingAction.getAction())) {
                requireParamField(pendingAction, missingFields, "delta", "\u589e\u52a0\u5e93\u5b58\u6570\u91cf");
            }
        }
        pendingAction.setMissingFields(missingFields);
    }

    private BackendApiProxyService.InvocationResult invoke(ActionConversationStore.PendingAction pendingAction,
                                                           String authorization) {
        BackendApiProxyService.InvocationRequest request = new BackendApiProxyService.InvocationRequest();
        request.setResource(pendingAction.getResource());
        request.setAction(pendingAction.getAction());
        request.setAuthorization(authorization);
        if (!CollectionUtils.isEmpty(pendingAction.getParams())) {
            request.setParams(new LinkedHashMap<String, Object>(pendingAction.getParams()));
        }
        if (!CollectionUtils.isEmpty(pendingAction.getPayload())) {
            request.setPayload(new LinkedHashMap<String, Object>(pendingAction.getPayload()));
        }
        return backendApiProxyService.invoke(request, authorization);
    }

    private boolean isInvocationSuccess(BackendApiProxyService.InvocationResult invocationResult) {
        if (invocationResult == null) {
            return false;
        }
        if (!"direct_response".equals(invocationResult.getPresentationHint())) {
            return false;
        }
        Object data = invocationResult.getData();
        if (!(data instanceof Map)) {
            return true;
        }
        Object code = ((Map<?, ?>) data).get("code");
        if (code instanceof Number) {
            return ((Number) code).intValue() == 200;
        }
        if (code != null) {
            return "200".equals(String.valueOf(code));
        }
        return true;
    }

    private ActionSignal detectActionSignal(String text, ParsedIntent parsedIntent) {
        if (!StringUtils.hasText(text)) {
            return ActionSignal.NONE;
        }
        String normalized = text.trim();
        if (looksLikeQuestionOrLearningIntent(normalized)) {
            return ActionSignal.NONE;
        }
        if (looksLikeActivityReadQuery(normalized, parsedIntent)) {
            return ActionSignal.NONE;
        }
        if (containsAny(normalized, CREATE_KEYWORDS) && looksLikeProductContext(normalized, parsedIntent)) {
            return ActionSignal.CREATE_PRODUCT;
        }
        if (containsAny(normalized, TAKE_DOWN_KEYWORDS) && looksLikeProductContext(normalized, parsedIntent)) {
            return ActionSignal.TAKE_DOWN_PRODUCT;
        }
        if (containsAny(normalized, UPDATE_PRICE_KEYWORDS) || (normalized.contains("\u4ef7\u683c") && normalized.contains("\u6539"))) {
            return ActionSignal.UPDATE_PRODUCT_PRICE;
        }
        if (containsAny(normalized, UPDATE_LOCATION_KEYWORDS) || (normalized.contains("\u5730\u70b9") && normalized.contains("\u6539"))) {
            return ActionSignal.UPDATE_PRODUCT_LOCATION;
        }
        Long productId = extractProductId(normalized);
        boolean hasPriorEntityReference = hasReferenceToPriorEntity(normalized);
        if (productId != null
                && (normalized.contains("\u4ef7\u683c") || normalized.contains("\u552e\u4ef7"))
                && extractPrice(normalized) != null) {
            return ActionSignal.UPDATE_PRODUCT_PRICE;
        }
        if (hasPriorEntityReference
                && (normalized.contains("\u4ef7\u683c") || normalized.contains("\u552e\u4ef7"))
                && extractPrice(normalized) != null) {
            return ActionSignal.UPDATE_PRODUCT_PRICE;
        }
        if (productId != null
                && (normalized.contains("\u5730\u70b9") || normalized.contains("\u4f4d\u7f6e"))
                && StringUtils.hasText(extractLocation(normalized))) {
            return ActionSignal.UPDATE_PRODUCT_LOCATION;
        }
        if (hasPriorEntityReference
                && (normalized.contains("\u5730\u70b9") || normalized.contains("\u4f4d\u7f6e"))
                && StringUtils.hasText(extractLocation(normalized))) {
            return ActionSignal.UPDATE_PRODUCT_LOCATION;
        }
        if (containsAny(normalized, INCREASE_STOCK_KEYWORDS)) {
            return ActionSignal.INCREASE_PRODUCT_STOCK;
        }
        return ActionSignal.NONE;
    }

    private boolean looksLikeProductContext(String text, ParsedIntent parsedIntent) {
        if (looksLikeActivityReadQuery(text, parsedIntent)) {
            return false;
        }
        if (containsAny(text, PRODUCT_HINT_KEYWORDS)) {
            return true;
        }
        CandidateSlots slots = parsedIntent == null ? null : parsedIntent.getCandidateSlots();
        return slots == null || !StringUtils.hasText(slots.getEntityType()) || "product".equalsIgnoreCase(slots.getEntityType());
    }

    private boolean looksLikeActivityReadQuery(String text, ParsedIntent parsedIntent) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String compact = text.trim().replaceAll("\\s+", "");
        boolean hasActivityScope = compact.contains("\u6d3b\u52a8")
                || compact.contains("\u6536\u85cf")
                || compact.contains("\u62a5\u540d")
                || compact.contains("\u6211\u7684\u53d1\u5e03")
                || compact.contains("\u6211\u53d1\u5e03\u7684")
                || compact.contains("\u6211\u53d1\u5e03\u4e86");
        if (hasActivityScope && containsAny(compact, ACTIVITY_READ_QUERY_KEYWORDS)) {
            return true;
        }
        CandidateSlots slots = parsedIntent == null ? null : parsedIntent.getCandidateSlots();
        if (slots != null && StringUtils.hasText(slots.getEntityType())) {
            String entityType = slots.getEntityType();
            if (("event".equalsIgnoreCase(entityType) || "activity".equalsIgnoreCase(entityType) || "local_activity".equalsIgnoreCase(entityType))
                    && containsAny(compact, ACTIVITY_READ_QUERY_KEYWORDS)) {
                return true;
            }
        }
        return parsedIntent != null
                && parsedIntent.getTaskType() != null
                && "event_search".equalsIgnoreCase(parsedIntent.getTaskType().getCode())
                && containsAny(compact, ACTIVITY_READ_QUERY_KEYWORDS);
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (!StringUtils.hasText(text) || CollectionUtils.isEmpty(keywords)) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasReferenceToPriorEntity(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        return containsAny(text, REFERENCE_KEYWORDS) || text.matches(".*\\u7b2c\\s*[\\u4e00\\u4e8c\\u4e09\\u56db\\u4e94\\u516d\\u4e03\\u516b\\u4e5d\\u5341\\d]+\\s*\\u4e2a.*");
    }

    private Long extractProductId(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = PRODUCT_ID_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Long.valueOf(matcher.group(1));
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        Matcher shortMatcher = PRODUCT_ID_SHORT_PATTERN.matcher(text);
        if (shortMatcher.find()) {
            try {
                return Long.valueOf(shortMatcher.group(1));
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal extractPrice(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher priceMatcher = PRICE_LABEL_PATTERN.matcher(text);
        BigDecimal lastValue = null;
        while (priceMatcher.find()) {
            lastValue = new BigDecimal(priceMatcher.group(1));
        }
        if (lastValue != null) {
            return lastValue;
        }
        Matcher actionMatcher = PRICE_ACTION_PATTERN.matcher(text);
        while (actionMatcher.find()) {
            lastValue = new BigDecimal(actionMatcher.group(1));
        }
        if (lastValue != null) {
            return lastValue;
        }
        Matcher currencyMatcher = PRICE_CURRENCY_PATTERN.matcher(text);
        while (currencyMatcher.find()) {
            lastValue = new BigDecimal(currencyMatcher.group(1));
        }
        return lastValue;
    }

    private Integer extractDelta(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = STOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        Matcher pieceMatcher = PIECE_COUNT_PATTERN.matcher(text);
        if (pieceMatcher.find()) {
            return Integer.valueOf(pieceMatcher.group(1));
        }
        return null;
    }

    private Integer extractCreateStock(String text) {
        return extractDelta(text);
    }

    private String extractLocation(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = LOCATION_PATTERN.matcher(text);
        if (matcher.find()) {
            return cleanLocationValue(matcher.group(1));
        }
        Matcher moveMatcher = MOVE_LOCATION_PATTERN.matcher(text);
        if (moveMatcher.find()) {
            return cleanLocationValue(moveMatcher.group(1));
        }
        return null;
    }

    private String extractTitle(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = CREATE_TITLE_PATTERN.matcher(text);
        if (matcher.find()) {
            return normalizeTitle(cleanFieldValue(matcher.group(1)));
        }
        Matcher inlineMatcher = CREATE_INLINE_TITLE_PATTERN.matcher(text);
        if (inlineMatcher.find()) {
            String candidate = normalizeTitle(cleanFieldValue(inlineMatcher.group(1)));
            if (StringUtils.hasText(candidate)
                    && !containsAny(candidate, CREATE_KEYWORDS)
                    && !"\u5546\u54c1".equals(candidate)
                    && !"\u4e1c\u897f".equals(candidate)
                    && !"\u7269\u54c1".equals(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String extractDescription(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = DESCRIPTION_PATTERN.matcher(text);
        if (matcher.find()) {
            return cleanFieldValue(matcher.group(1));
        }
        return null;
    }

    private String extractCondition(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = CONDITION_PATTERN.matcher(text);
        if (matcher.find()) {
            return cleanFieldValue(matcher.group(1));
        }
        if (text.contains("\u5168\u65b0")) {
            return "\u5168\u65b0";
        }
        if (text.contains("\u4e5d\u6210\u65b0")) {
            return "\u4e5d\u6210\u65b0";
        }
        if (text.contains("\u516b\u6210\u65b0")) {
            return "\u516b\u6210\u65b0";
        }
        if (text.contains("\u4e03\u6210\u65b0")) {
            return "\u4e03\u6210\u65b0";
        }
        if (text.contains("\u4e8c\u624b")) {
            return "\u4e8c\u624b";
        }
        return null;
    }

    private Integer extractCategoryId(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = CATEGORY_PATTERN.matcher(text);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private Integer resolveCategoryId(String text, Map<String, Object> payload) {
        Integer explicit = extractCategoryId(text);
        if (explicit != null) {
            return explicit;
        }
        String explicitName = extractCategoryName(text);
        if (StringUtils.hasText(explicitName)) {
            Integer matched = matchCategoryIdByName(explicitName);
            if (matched != null) {
                return matched;
            }
        }
        String title = payload == null ? null : asText(payload.get("title"));
        Integer byTitleKeyword = inferCategoryIdFromText(firstNonBlank(title, text));
        if (byTitleKeyword != null) {
            return byTitleKeyword;
        }
        return null;
    }

    private String extractCategoryName(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = CATEGORY_NAME_PATTERN.matcher(text);
        if (matcher.find()) {
            return cleanFieldValue(matcher.group(1));
        }
        return null;
    }

    private Integer inferCategoryIdFromText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = normalizeCategoryText(text);
        if (normalized.contains("\u624b\u673a") || normalized.contains("iphone") || normalized.contains("\u82f9\u679c\u624b\u673a")) {
            Integer matched = findCategoryIdByKeyword("\u624b\u673a");
            if (matched != null) {
                return matched;
            }
        }
        if (normalized.contains("\u6c34\u679c")) {
            Integer matched = findCategoryIdByKeyword("\u6c34\u679c");
            if (matched != null) {
                return matched;
            }
        }
        if (normalized.contains("\u7535\u8111")) {
            Integer matched = findCategoryIdByKeyword("\u7535\u8111");
            if (matched != null) {
                return matched;
            }
        }
        if (normalized.contains("\u5bb6\u5177") || normalized.contains("\u5bb6\u5c45")) {
            Integer matched = findCategoryIdByKeyword("\u5bb6\u5177");
            if (matched != null) {
                return matched;
            }
        }
        return null;
    }

    private Integer findCategoryIdByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword) || categoryService == null) {
            return null;
        }
        List<Category> categories = safeCategories();
        for (Category category : categories) {
            String normalizedName = normalizeCategoryText(category == null ? null : category.getName());
            if (normalizedName.contains(keyword)) {
                Integer parsed = parseCategoryId(category == null ? null : category.getId());
                if (parsed != null) {
                    return parsed;
                }
            }
        }
        return null;
    }

    private Integer matchCategoryIdByName(String text) {
        if (!StringUtils.hasText(text) || categoryService == null) {
            return null;
        }
        String normalizedInput = normalizeCategoryText(text);
        if (!StringUtils.hasText(normalizedInput)) {
            return null;
        }
        List<Category> categories = safeCategories();
        Integer bestId = null;
        int bestScore = 0;
        for (Category category : categories) {
            String normalizedName = normalizeCategoryText(category == null ? null : category.getName());
            if (!StringUtils.hasText(normalizedName)) {
                continue;
            }
            int score = 0;
            if (normalizedInput.equals(normalizedName)) {
                score = 100;
            } else if (normalizedInput.contains(normalizedName)) {
                score = 80 + normalizedName.length();
            } else if (normalizedName.contains(normalizedInput)) {
                score = 60 + normalizedInput.length();
            } else {
                Set<String> inputTokens = tokenizeCategoryText(normalizedInput);
                Set<String> nameTokens = tokenizeCategoryText(normalizedName);
                inputTokens.retainAll(nameTokens);
                if (!inputTokens.isEmpty()) {
                    score = inputTokens.size() * 10;
                }
            }
            if (score > bestScore) {
                Integer parsed = parseCategoryId(category.getId());
                if (parsed != null) {
                    bestId = parsed;
                    bestScore = score;
                }
            }
        }
        return bestScore >= 20 ? bestId : null;
    }

    private List<Category> safeCategories() {
        try {
            List<Category> categories = categoryService.getAllCategories();
            return categories == null ? Collections.<Category>emptyList() : categories;
        } catch (Exception ignore) {
            return Collections.emptyList();
        }
    }

    private Integer parseCategoryId(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private String normalizeCategoryText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text
                .toLowerCase(Locale.ROOT)
                .replaceAll("[/\\\\()（）,，.。;；:_\\-\\s]+", "")
                .trim();
    }

    private Set<String> tokenizeCategoryText(String text) {
        Set<String> tokens = new HashSet<String>();
        if (!StringUtils.hasText(text)) {
            return tokens;
        }
        String compact = normalizeCategoryText(text);
        if (!StringUtils.hasText(compact)) {
            return tokens;
        }
        for (int i = 0; i < compact.length(); i++) {
            for (int j = i + 2; j <= compact.length() && j <= i + 4; j++) {
                tokens.add(compact.substring(i, j));
            }
        }
        return tokens;
    }

    private String normalizeTitle(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = value.trim();
        normalized = normalized.replaceFirst("^(叫|是)", "");
        return cleanFieldValue(normalized);
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : null;
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

    private List<String> extractImageUrls(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        List<String> imageUrls = new ArrayList<String>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            imageUrls.add(matcher.group());
        }
        return imageUrls;
    }

    private String toImagePayload(List<String> imageUrls) {
        try {
            return objectMapper.writeValueAsString(imageUrls);
        } catch (JsonProcessingException ignore) {
            return imageUrls.isEmpty() ? null : imageUrls.get(0);
        }
    }

    private void requireParamField(ActionConversationStore.PendingAction pendingAction,
                                   List<String> missingFields,
                                   String key,
                                   String label) {
        Object value = pendingAction.getParams().get(key);
        if (value == null || (value instanceof String && !StringUtils.hasText((String) value))) {
            missingFields.add(label);
        }
    }

    private void requirePayloadField(ActionConversationStore.PendingAction pendingAction,
                                     List<String> missingFields,
                                     String key,
                                     String label) {
        Object value = pendingAction.getPayload().get(key);
        if (value == null || (value instanceof String && !StringUtils.hasText((String) value))) {
            missingFields.add(label);
        }
    }

    private String cleanFieldValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String cleaned = value.trim();
        cleaned = cleaned.replaceAll("^[\\uFF1A:\\s]+", "");
        cleaned = cleaned.replaceAll("[,\\uff0c.\\u3002;\\uff1b\\s]+$", "");
        return StringUtils.hasText(cleaned) ? cleaned : null;
    }

    private String cleanLocationValue(String value) {
        String cleaned = cleanFieldValue(value);
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }
        cleaned = cleaned.replaceFirst("^(?:\\u653e\\u5230|\\u79fb\\u5230|\\u6539\\u5230|\\u6539\\u6210|\\u6539\\u4e3a|\\u8c03\\u6574\\u4e3a|\\u662f|\\u4e3a)\\s*", "");
        return cleanFieldValue(cleaned);
    }

    private String normalizeShortReply(String text) {
        String cleaned = cleanFieldValue(text);
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }
        return cleaned.replaceAll("^\\s*(?:\\u5c31|\\u662f|\\u586b|\\u8865|\\u6539\\u6210|\\u6539\\u4e3a|\\u8bbe\\u4e3a)\\s*", "").trim();
    }

    private BigDecimal extractLooseDecimal(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        BigDecimal price = extractPrice(text);
        if (price != null) {
            return price;
        }
        Matcher matcher = NUMBER_ONLY_PATTERN.matcher(text);
        return matcher.matches() ? new BigDecimal(matcher.group(1)) : null;
    }

    private Integer extractLooseInteger(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Integer value = extractDelta(text);
        if (value != null) {
            return value;
        }
        Matcher matcher = INTEGER_ONLY_PATTERN.matcher(text);
        return matcher.matches() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private Long extractLooseLong(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = INTEGER_ONLY_PATTERN.matcher(text);
        return matcher.matches() ? Long.valueOf(matcher.group(1)) : null;
    }

    private boolean looksLikePlainFieldValue(String text) {
        if (!StringUtils.hasText(text) || text.length() > 30) {
            return false;
        }
        if (looksLikeQuestionOrLearningIntent(text) || containsAny(text, CONFIRM_KEYWORDS) || containsAny(text, CANCEL_KEYWORDS)) {
            return false;
        }
        return !text.contains(" ") || text.length() <= 12;
    }

    private boolean looksLikeQuestionOrLearningIntent(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = text.trim();
        if (containsAny(normalized, LEARNING_INTENT_KEYWORDS)) {
            return true;
        }
        boolean hasQuestionMarker = containsAny(normalized, QUESTION_MARKERS)
                || normalized.endsWith("?")
                || normalized.endsWith("\uff1f");
        if (!hasQuestionMarker) {
            return false;
        }
        if (containsAny(normalized, REQUEST_COMMAND_PREFIXES)
                && (containsAny(normalized, CREATE_KEYWORDS)
                || containsAny(normalized, TAKE_DOWN_KEYWORDS)
                || containsAny(normalized, UPDATE_PRICE_KEYWORDS)
                || containsAny(normalized, UPDATE_LOCATION_KEYWORDS)
                || containsAny(normalized, INCREASE_STOCK_KEYWORDS))) {
            return false;
        }
        return true;
    }

    @Data
    public static class ActionReviewResult {
        private boolean handled;
        private ActionOutcome outcome = ActionOutcome.NONE;
        private ActionConversationStore.PendingAction pendingAction;
        private BackendApiProxyService.InvocationResult invocationResult;
    }

    public enum ActionOutcome {
        NONE,
        NEED_CLARIFICATION,
        NEED_CONFIRMATION,
        EXECUTED,
        FAILED,
        CANCELLED
    }

    private enum ActionSignal {
        NONE("", "", "", Collections.<String>emptyList()),
        CREATE_PRODUCT("CREATE", "create", "\u53d1\u5e03\u5546\u54c1", CREATE_KEYWORDS),
        TAKE_DOWN_PRODUCT("UPDATE", "take_down", "\u4e0b\u67b6\u5546\u54c1", TAKE_DOWN_KEYWORDS),
        UPDATE_PRODUCT_PRICE("UPDATE", "update_price", "\u4fee\u6539\u5546\u54c1\u4ef7\u683c", UPDATE_PRICE_KEYWORDS),
        UPDATE_PRODUCT_LOCATION("UPDATE", "update_location", "\u4fee\u6539\u5546\u54c1\u5730\u70b9", UPDATE_LOCATION_KEYWORDS),
        INCREASE_PRODUCT_STOCK("UPDATE", "increase_stock", "\u589e\u52a0\u5546\u54c1\u5e93\u5b58", INCREASE_STOCK_KEYWORDS);

        private final String operationType;
        private final String preferredAction;
        private final String displayName;
        private final List<String> keywords;

        ActionSignal(String operationType, String preferredAction, String displayName, List<String> keywords) {
            this.operationType = operationType;
            this.preferredAction = preferredAction;
            this.displayName = displayName;
            this.keywords = keywords;
        }

        public String getOperationType() {
            return operationType;
        }

        public String getPreferredAction() {
            return preferredAction;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<String> getKeywords() {
            return keywords;
        }
    }
}
