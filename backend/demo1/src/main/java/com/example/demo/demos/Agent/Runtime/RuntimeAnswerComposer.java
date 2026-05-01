package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Pojo.CandidateSlots;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalResponse;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Service.BackendApiProxyService;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.common.enums.AnswerType;
import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.common.enums.TaskType;
import com.example.demo.demos.common.schema.FinalAnswer;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.model.RealtimeResultItem;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import com.example.demo.demos.search.model.ProductSearchQuery;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Component
public class RuntimeAnswerComposer {

    public FinalAnswer buildChitchatAnswer(AgentChatMessage latestMessage, ParsedIntent parsedIntent) {
        String queryText = latestMessage == null ? null : latestMessage.getContent();
        String normalized = queryText == null ? "" : queryText.trim().toLowerCase(Locale.ROOT);
        FinalAnswer finalAnswer = baseAnswer(AnswerType.FAQ_ANSWER);
        finalAnswer.setSummary("已识别为闲聊或问候。");
        if (containsAny(normalized, "谢谢", "感谢", "thx", "thanks")) {
            finalAnswer.setAnswerText("不客气。如果你想查商品、规则说明或实时状态，可以直接告诉我。");
        } else if (containsAny(normalized, "再见", "拜拜", "bye", "晚安")) {
            finalAnswer.setAnswerText("好的，随时需要时再来找我。");
        } else {
            finalAnswer.setAnswerText("你好，我在。你可以直接告诉我想查的商品、规则、知识库内容或实时状态。");
        }
        finalAnswer.getNextActions().add("例如：推荐几个社区市场里的蔬菜。");
        finalAnswer.getNextActions().add("例如：查询商品 id 1 的实时状态。");
        finalAnswer.getComposerMeta().getUsedSources().add("intent_parser");
        finalAnswer.getComposerMeta().getUsedSources().add("agent_runtime");
        finalAnswer.getComposerMeta().getMetadata().put("taskType",
                parsedIntent == null || parsedIntent.getTaskType() == null ? null : parsedIntent.getTaskType().getCode());
        if (StringUtils.hasText(queryText)) {
            finalAnswer.getComposerMeta().getMetadata().put("queryText", queryText);
        }
        return finalAnswer;
    }

    public FinalAnswer buildUnsupportedActionAnswer(AgentChatMessage latestMessage, ParsedIntent parsedIntent) {
        FinalAnswer finalAnswer = baseAnswer(AnswerType.CLARIFICATION);
        finalAnswer.setSummary("\u5f53\u524d\u804a\u5929\u5165\u53e3\u4e3b\u8981\u652f\u6301\u68c0\u7d22\u3001\u95ee\u7b54\u548c\u5b9e\u65f6\u72b6\u6001\u67e5\u8be2\u3002");
        finalAnswer.setAnswerText("\u6211\u80fd\u5e2e\u4f60\u67e5\u5546\u54c1\u3001\u77e5\u8bc6\u89c4\u5219\u548c\u5b9e\u65f6\u72b6\u6001\uff0c\u4f46\u76ee\u524d\u4e0d\u652f\u6301\u5728\u8fd9\u4e2a\u804a\u5929\u7a97\u53e3\u91cc\u76f4\u63a5\u6267\u884c\u53d1\u5e03\u5546\u54c1\u3001\u4e0b\u5355\u6216\u5176\u4ed6\u5199\u64cd\u4f5c\u3002");
        finalAnswer.getDisclaimers().add("\u8fd9\u7c7b\u9700\u8981\u6539\u5199\u4e1a\u52a1\u6570\u636e\u7684\u64cd\u4f5c\uff0c\u5f53\u524d\u4ecd\u9700\u901a\u8fc7\u7ba1\u7406\u9875\u6216\u4e13\u7528\u63a5\u53e3\u5b8c\u6210\u3002");
        finalAnswer.getNextActions().add("\u5982\u9700\u53d1\u5e03\u5546\u54c1\uff0c\u8bf7\u8fdb\u5165\u5546\u54c1\u7ba1\u7406/\u53d1\u5e03\u9875\u6216\u8c03\u7528\u76f8\u5e94\u540e\u7aef\u63a5\u53e3\u3002");
        finalAnswer.getNextActions().add("\u5982\u679c\u4f60\u53ea\u662f\u60f3\u67e5\u8be2\u73b0\u6709\u5546\u54c1\uff0c\u53ef\u4ee5\u76f4\u63a5\u544a\u8bc9\u6211\u60f3\u627e\u4ec0\u4e48\u3002");
        finalAnswer.getComposerMeta().getUsedSources().add("intent_parser");
        finalAnswer.getComposerMeta().getUsedSources().add("agent_runtime");
        if (latestMessage != null && StringUtils.hasText(latestMessage.getContent())) {
            finalAnswer.getComposerMeta().getMetadata().put("queryText", latestMessage.getContent());
        }
        finalAnswer.getComposerMeta().getMetadata().put("taskType",
                parsedIntent == null || parsedIntent.getTaskType() == null ? null : parsedIntent.getTaskType().getCode());
        return finalAnswer;
    }

    public FinalAnswer buildActionClarificationAnswer(ActionIntentReviewService.ActionReviewResult reviewResult) {
        FinalAnswer finalAnswer = baseAnswer(AnswerType.CLARIFICATION);
        ActionConversationStore.PendingAction pendingAction = reviewResult == null ? null : reviewResult.getPendingAction();
        finalAnswer.setSummary("\u8fd9\u4e2a\u8bf7\u6c42\u5df2\u8bc6\u522b\u4e3a" + safeText(pendingAction == null ? null : pendingAction.getDisplayName(), "\u5199\u64cd\u4f5c") + "\uff0c\u4f46\u8fd8\u7f3a\u5c11\u5fc5\u8981\u53c2\u6570\u3002");
        finalAnswer.setAnswerText(buildActionClarificationText(pendingAction));
        finalAnswer.getDisclaimers().add("\u5199\u64cd\u4f5c\u4f1a\u76f4\u63a5\u8c03\u7528\u540e\u7aef\u63a5\u53e3\uff0c\u5148\u8865\u9f50\u53c2\u6570\u540e\u624d\u80fd\u7ee7\u7eed\u3002");
        finalAnswer.getNextActions().add("\u76f4\u63a5\u56de\u590d\u7f3a\u5c11\u7684\u53c2\u6570\uff0c\u6211\u4f1a\u57fa\u4e8e\u5f53\u524d session \u63a5\u7740\u8865\u5168\u3002");
        finalAnswer.getNextActions().add("\u5982\u679c\u4e0d\u60f3\u6267\u884c\uff0c\u56de\u590d\u201c\u53d6\u6d88\u201d\u5373\u53ef\u3002");
        finalAnswer.getComposerMeta().getUsedSources().add("api_routes");
        finalAnswer.getComposerMeta().getUsedSources().add("action_review");
        if (pendingAction != null) {
            finalAnswer.getComposerMeta().getMetadata().put("action", pendingAction.getAction());
            finalAnswer.getComposerMeta().getMetadata().put("resource", pendingAction.getResource());
            finalAnswer.getComposerMeta().getMetadata().put("missingFields", pendingAction.getMissingFields());
        }
        return finalAnswer;
    }

    public FinalAnswer buildActionConfirmationAnswer(ActionIntentReviewService.ActionReviewResult reviewResult) {
        FinalAnswer finalAnswer = baseAnswer(AnswerType.CLARIFICATION);
        ActionConversationStore.PendingAction pendingAction = reviewResult == null ? null : reviewResult.getPendingAction();
        finalAnswer.setSummary("\u5df2\u5b8c\u6210" + safeText(pendingAction == null ? null : pendingAction.getDisplayName(), "\u5199\u64cd\u4f5c") + "\u7684\u53c2\u6570\u6536\u96c6\uff0c\u7b49\u4f60\u786e\u8ba4\u540e\u5c31\u4f1a\u771f\u6b63\u6267\u884c\u3002");
        finalAnswer.setAnswerText(buildActionConfirmationText(pendingAction));
        finalAnswer.getDisclaimers().add("\u8fd9\u662f\u771f\u5b9e\u5199\u64cd\u4f5c\uff0c\u786e\u8ba4\u540e\u4f1a\u76f4\u63a5\u8c03\u7528\u540e\u7aef\u63a5\u53e3\u3002");
        finalAnswer.getNextActions().add("\u56de\u590d\u201c\u786e\u8ba4\u6267\u884c\u201d\u7ee7\u7eed\u3002");
        finalAnswer.getNextActions().add("\u56de\u590d\u201c\u53d6\u6d88\u201d\u653e\u5f03\u8fd9\u6b21\u64cd\u4f5c\u3002");
        finalAnswer.getComposerMeta().getUsedSources().add("api_routes");
        finalAnswer.getComposerMeta().getUsedSources().add("action_review");
        if (pendingAction != null) {
            finalAnswer.getComposerMeta().getMetadata().put("action", pendingAction.getAction());
            finalAnswer.getComposerMeta().getMetadata().put("resource", pendingAction.getResource());
        }
        return finalAnswer;
    }

    public FinalAnswer buildActionExecutionAnswer(ActionIntentReviewService.ActionReviewResult reviewResult) {
        ActionConversationStore.PendingAction pendingAction = reviewResult == null ? null : reviewResult.getPendingAction();
        BackendApiProxyService.InvocationResult invocationResult = reviewResult == null ? null : reviewResult.getInvocationResult();
        boolean success = reviewResult != null && reviewResult.getOutcome() == ActionIntentReviewService.ActionOutcome.EXECUTED;
        FinalAnswer finalAnswer = baseAnswer(success ? AnswerType.FAQ_ANSWER : AnswerType.PARTIAL_RESULT);
        String displayName = safeText(pendingAction == null ? null : pendingAction.getDisplayName(), "\u5199\u64cd\u4f5c");
        String backendMessage = extractInvocationMessage(invocationResult);
        finalAnswer.setSummary(success
                ? displayName + "\u5df2\u6267\u884c"
                : displayName + "\u6267\u884c\u5931\u8d25");
        finalAnswer.setAnswerText(success
                ? "\u5df2\u4e3a\u4f60\u6267\u884c" + displayName + "\u3002" + appendBackendMessage(backendMessage)
                : "\u6211\u5c1d\u8bd5\u6267\u884c" + displayName + "\uff0c\u4f46\u540e\u7aef\u6ca1\u6709\u8fd4\u56de\u6210\u529f\u7ed3\u679c\u3002" + appendBackendMessage(backendMessage));
        if (!success) {
            finalAnswer.getDisclaimers().add("\u8fd9\u6b21\u5199\u64cd\u4f5c\u672a\u6210\u529f\u843d\u5e93\uff0c\u8bf7\u5148\u68c0\u67e5\u53c2\u6570\u6216\u767b\u5f55\u72b6\u6001\u3002");
            finalAnswer.getComposerMeta().setDegraded(true);
            finalAnswer.getComposerMeta().setDegradeReason("action_execute_failed");
        }
        finalAnswer.getNextActions().add(success
                ? "\u5982\u679c\u8fd8\u8981\u7ee7\u7eed\u8c03\u6574\uff0c\u53ef\u4ee5\u76f4\u63a5\u63cf\u8ff0\u4e0b\u4e00\u4e2a\u5199\u64cd\u4f5c\u3002"
                : "\u53ef\u4ee5\u76f4\u63a5\u4fee\u6b63\u53c2\u6570\u540e\u91cd\u65b0\u53d1\u8d77\u64cd\u4f5c\u3002");
        finalAnswer.getComposerMeta().getUsedSources().add("api_routes");
        finalAnswer.getComposerMeta().getUsedSources().add("backend_api_proxy");
        if (pendingAction != null) {
            finalAnswer.getComposerMeta().getMetadata().put("action", pendingAction.getAction());
            finalAnswer.getComposerMeta().getMetadata().put("resource", pendingAction.getResource());
        }
        finalAnswer.getComposerMeta().getMetadata().put("presentationHint", invocationResult == null ? null : invocationResult.getPresentationHint());
        finalAnswer.getComposerMeta().getMetadata().put("backendMessage", backendMessage);
        return finalAnswer;
    }

    public FinalAnswer buildActionCancelledAnswer(ActionIntentReviewService.ActionReviewResult reviewResult) {
        FinalAnswer finalAnswer = baseAnswer(AnswerType.FAQ_ANSWER);
        ActionConversationStore.PendingAction pendingAction = reviewResult == null ? null : reviewResult.getPendingAction();
        finalAnswer.setSummary("\u5df2\u53d6\u6d88" + safeText(pendingAction == null ? null : pendingAction.getDisplayName(), "\u5199\u64cd\u4f5c") + "\u3002");
        finalAnswer.setAnswerText("\u597d\u7684\uff0c\u8fd9\u6b21" + safeText(pendingAction == null ? null : pendingAction.getDisplayName(), "\u64cd\u4f5c") + "\u5df2\u53d6\u6d88\uff0c\u4e0d\u4f1a\u7ee7\u7eed\u6267\u884c\u3002");
        finalAnswer.getNextActions().add("\u5982\u679c\u9700\u8981\uff0c\u53ef\u4ee5\u91cd\u65b0\u63cf\u8ff0\u65b0\u7684\u5199\u64cd\u4f5c\u6216\u68c0\u7d22\u9700\u6c42\u3002");
        finalAnswer.getComposerMeta().getUsedSources().add("action_review");
        return finalAnswer;
    }

    public FinalAnswer buildRealtimeClarification(ParsedIntent parsedIntent) {
        FinalAnswer finalAnswer = baseAnswer(AnswerType.CLARIFICATION);
        finalAnswer.setSummary("进行实时查询前，还需要明确的实体 ID。");
        finalAnswer.setAnswerText("我可以帮你查实时状态，但还需要你提供明确的商品 ID。");
        finalAnswer.getNextActions().add("例如回复：商品 id 123 现在还能买吗？");
        finalAnswer.getNextActions().add("如果你不知道 ID，可以先让我帮你搜索结果。");
        finalAnswer.getComposerMeta().getUsedSources().add("intent_parser");
        finalAnswer.getComposerMeta().getMetadata().put("taskType",
                parsedIntent == null || parsedIntent.getTaskType() == null ? null : parsedIntent.getTaskType().getCode());
        return finalAnswer;
    }

    public FinalAnswer buildRoutingClarificationAnswer(String clarificationPrompt, ParsedIntent parsedIntent) {
        FinalAnswer finalAnswer = baseAnswer(AnswerType.CLARIFICATION);
        String prompt = StringUtils.hasText(clarificationPrompt)
                ? clarificationPrompt
                : "我还需要你补充一点条件，才能准确选择要查询的工具。";
        finalAnswer.setSummary("当前查询条件还不够明确。");
        finalAnswer.setAnswerText(prompt);
        finalAnswer.getNextActions().add("可以补充你想查商品、活动还是门店。");
        finalAnswer.getNextActions().add("也可以补充关键词、地点、时间或价格范围。");
        finalAnswer.getComposerMeta().getUsedSources().add("tool_router");
        finalAnswer.getComposerMeta().getMetadata().put("taskType",
                parsedIntent == null || parsedIntent.getTaskType() == null ? null : parsedIntent.getTaskType().getCode());
        finalAnswer.getComposerMeta().getMetadata().put("clarificationPrompt", prompt);
        return finalAnswer;
    }

    public FinalAnswer buildRealtimeUnavailableAnswer(RealtimeQueryRequest request,
                                                      String errorCode,
                                                      String errorMessage) {
        FinalAnswer finalAnswer = baseAnswer(AnswerType.PARTIAL_RESULT);
        finalAnswer.setSummary("实时服务暂时不可用，本次结果已降级。");
        finalAnswer.setAnswerText("实时查询暂时不可用，当前无法确认最新状态。");
        finalAnswer.getDisclaimers().add("当前状态未能实时确认，如需依赖该结果，请稍后重试。");
        finalAnswer.getNextActions().add("稍后重试这次实时查询。");
        finalAnswer.getNextActions().add("如有需要，可通过实时管理接口做人工核验。");
        if (request != null && !CollectionUtils.isEmpty(request.getEntityIds())) {
            finalAnswer.getNextActions().add("可重试的实体 ID：" + request.getEntityIds());
        }
        FinalAnswer.Citation citation = new FinalAnswer.Citation();
        citation.setSourceType("realtime_service");
        citation.setSourceId(request == null ? null : request.getEntityType());
        citation.setTitle("实时网关");
        citation.setSnippet("实时查询失败，错误码：" + safeText(errorCode, "unknown"));
        citation.setConfidence(0.2D);
        finalAnswer.getCitations().add(citation);
        finalAnswer.getComposerMeta().setDegraded(true);
        finalAnswer.getComposerMeta().setDegradeReason(safeText(errorCode, "realtime_unavailable"));
        finalAnswer.getComposerMeta().getUsedSources().add("realtime_service");
        finalAnswer.getComposerMeta().getMetadata().put("errorCode", errorCode);
        finalAnswer.getComposerMeta().getMetadata().put("errorMessage", safeText(errorMessage, "unknown"));
        return finalAnswer;
    }

    public FinalAnswer buildRealtimeAnswer(RealtimeQueryResponse response, RealtimeQueryRequest request) {
        List<MergedRealtimeItem> mergedItems = mergeRealtimeItems(response == null ? null : response.getItems());
        FinalAnswer finalAnswer = baseAnswer(response != null && response.getRealtimeStatus() == RealtimeStatus.SUCCESS
                ? AnswerType.REALTIME_CONFIRMATION
                : AnswerType.PARTIAL_RESULT);
        finalAnswer.setSummary(buildRealtimeSummary(response, mergedItems));
        finalAnswer.setAnswerText(buildRealtimeNarrative(response, mergedItems));
        finalAnswer.getComposerMeta().setDegraded(response == null || response.getRealtimeStatus() != RealtimeStatus.SUCCESS);
        finalAnswer.getComposerMeta().setDegradeReason(response != null && response.getRealtimeStatus() == RealtimeStatus.SUCCESS
                ? null
                : response == null || response.getRealtimeStatus() == null ? RealtimeStatus.FAILED.getCode() : response.getRealtimeStatus().getCode());
        finalAnswer.getComposerMeta().getUsedSources().add("realtime_service");
        finalAnswer.getComposerMeta().getMetadata().put("queryMeta", response == null ? Collections.emptyMap() : response.getQueryMeta());
        finalAnswer.getComposerMeta().getMetadata().put("partialFailedIds", response == null ? Collections.emptyList() : response.getPartialFailedIds());
        finalAnswer.getComposerMeta().getMetadata().put("mergedEntityCount", mergedItems.size());

        List<String> conflictSummaries = new ArrayList<String>();
        for (MergedRealtimeItem mergedItem : mergedItems) {
            RealtimeResultItem item = mergedItem.getItem();
            FinalAnswer.EntityCard card = new FinalAnswer.EntityCard();
            card.setEntityId(item.getEntityId());
            card.setEntityType(request == null ? "product" : request.getEntityType());
            card.setTitle("实体 #" + item.getEntityId());
            card.setSubtitle(buildCardSubtitle(item, mergedItem.hasConflict()));
            card.setPriceText(item.getPrice() == null ? null : item.getPrice().toPlainString() + " " + safeText(item.getCurrency(), "CNY"));
            card.setRealtimeStatusText(safeText(item.getAvailabilityStatus(), "未知"));
            card.setSourceLabel(mergedItem.getSourceLabel());
            card.getHighlights().add("可用状态：" + safeText(item.getAvailabilityStatus(), "未知"));
            if (item.getInventoryCount() != null) {
                card.getHighlights().add("库存：" + item.getInventoryCount());
            }
            if (item.getPrice() != null) {
                card.getHighlights().add("价格：" + item.getPrice().toPlainString());
            }
            if (mergedItem.getSourceCount() > 1) {
                card.getHighlights().add("合并来源数：" + mergedItem.getSourceCount());
            }
            if (mergedItem.hasConflict()) {
                card.getHighlights().add("检测到实时来源字段冲突");
            }
            if (item.getOpenNow() != null) {
                card.getTags().add(item.getOpenNow() ? "营业中" : "未营业");
            }
            for (String source : mergedItem.getSources()) {
                card.getTags().add("来源:" + source);
            }
            card.setRecommendReason(buildRecommendReason(item, mergedItem));
            finalAnswer.getCards().add(card);
            finalAnswer.getCitations().add(buildRealtimeCitation(item, mergedItem));
            if (mergedItem.hasConflict()) {
                conflictSummaries.addAll(mergedItem.getConflictSummaries());
            }
        }

        if (response != null && response.getRealtimeStatus() == RealtimeStatus.SUCCESS) {
            finalAnswer.getNextActions().add("如果还需要确认一次，可以刷新实时结果。");
        } else {
            finalAnswer.getDisclaimers().add("部分实时结果不完整或已降级，请在执行操作前再次确认。");
            finalAnswer.getNextActions().add("如果这个状态很关键，建议强制刷新后再查一次。");
        }
        if (response != null && !CollectionUtils.isEmpty(response.getPartialFailedIds())) {
            finalAnswer.getDisclaimers().add("以下实体 ID 未能完成实时确认：" + response.getPartialFailedIds());
        }
        if (!conflictSummaries.isEmpty()) {
            finalAnswer.getComposerMeta().setConflictDetected(true);
            finalAnswer.getComposerMeta().getMetadata().put("conflicts", conflictSummaries);
            finalAnswer.getDisclaimers().add("部分实体存在实时字段冲突，请查看合并来源后再决定。");
        }
        finalAnswer.getNextActions().add("如需精确核验，可直接使用实时管理接口。");
        return finalAnswer;
    }

    public FinalAnswer buildProductSearchAnswer(List<ProductSearchSnapshot> items,
                                                long total,
                                                ProductSearchQuery query,
                                                AgentChatMessage latestMessage,
                                                ParsedIntent parsedIntent) {
        FinalAnswer finalAnswer = baseAnswer(items == null || items.isEmpty()
                ? AnswerType.NO_RESULT : AnswerType.RECOMMENDATION);
        if (CollectionUtils.isEmpty(items)) {
            return buildNoResultAnswer(latestMessage, parsedIntent, "search_only", "search_empty");
        }

        finalAnswer.setSummary("共找到 " + total + " 个商品候选结果。");
        finalAnswer.setAnswerText(buildSearchNarrative(items));
        finalAnswer.getComposerMeta().getUsedSources().add("product_search_snapshot");
        finalAnswer.getComposerMeta().getMetadata().put("searchTotal", total);
        finalAnswer.getComposerMeta().getMetadata().put("searchKeyword", query == null ? null : query.getKeyword());
        for (ProductSearchSnapshot item : items) {
            finalAnswer.getCards().add(buildProductCard(item));
            finalAnswer.getCitations().add(buildProductCitation(item));
        }
        if (!CollectionUtils.isEmpty(items)) {
            finalAnswer.getNextActions().add("如果要查实时状态，可以回复：商品 id " + items.get(0).getProductId() + "。");
        }
        finalAnswer.getNextActions().add("如果结果不够精确，可以补充价格、城市或类目条件。");
        if (items.size() < total) {
            finalAnswer.getDisclaimers().add("当前先展示前 " + items.size() + " 条结果。");
        }
        return finalAnswer;
    }

    public FinalAnswer buildRouteDataAnswer(NormalizedRouteData routeData,
                                             AgentChatMessage latestMessage,
                                             ParsedIntent parsedIntent) {
        if (routeData == null) {
            return buildNoResultAnswer(latestMessage, parsedIntent, "api_route", "route_empty");
        }
        boolean hasItems = !CollectionUtils.isEmpty(routeData.getItems());
        FinalAnswer finalAnswer = baseAnswer(hasItems ? AnswerType.RECOMMENDATION : AnswerType.NO_RESULT);
        String entityLabel = routeEntityLabel(routeData.getEntityType());
        long total = routeData.getTotal() > 0 ? routeData.getTotal() : routeData.getItems().size();
        finalAnswer.setSummary(hasItems
                ? "共找到 " + total + " 个" + entityLabel + "候选结果。"
                : "当前没有查询到匹配的" + entityLabel + "结果。");
        finalAnswer.setAnswerText(hasItems
                ? buildRouteDataNarrative(routeData.getItems(), entityLabel)
                : "我理解你想查" + entityLabel + "，但当前没有命中可返回的结果。");
        finalAnswer.getComposerMeta().getUsedSources().add("api_routes");
        finalAnswer.getComposerMeta().getUsedSources().add("backend_api_proxy");
        finalAnswer.getComposerMeta().getMetadata().put("routeEntityType", routeData.getEntityType());
        finalAnswer.getComposerMeta().getMetadata().put("routeSourceResource", routeData.getSourceResource());
        finalAnswer.getComposerMeta().getMetadata().put("routeSourceAction", routeData.getSourceAction());
        finalAnswer.getComposerMeta().getMetadata().put("routeSearchTotal", total);
        finalAnswer.getComposerMeta().getMetadata().put("presentationHint", routeData.getPresentationHint());
        if (routeData.isDegraded()) {
            finalAnswer.setAnswerType(AnswerType.PARTIAL_RESULT);
            finalAnswer.getComposerMeta().setDegraded(true);
            finalAnswer.getComposerMeta().setDegradeReason(routeData.getErrorMessage());
            finalAnswer.getDisclaimers().add("接口结果已降级：" + safeText(routeData.getErrorMessage(), "未知原因"));
        }
        for (RouteEntityCandidate item : routeData.getItems()) {
            finalAnswer.getCards().add(buildRouteCard(item, routeData));
            finalAnswer.getCitations().add(buildRouteCitation(item, routeData));
        }
        if (hasItems) {
            finalAnswer.getNextActions().add("可以继续补充时间、地点或关键词，我再帮你缩小范围。");
        } else {
            finalAnswer.getNextActions().add("可以换一个关键词、地点或时间范围再查一次。");
        }
        return finalAnswer;
    }

    public FinalAnswer buildKnowledgeAnswer(KnowledgeRetrievalResponse response,
                                            AgentChatMessage latestMessage,
                                            ParsedIntent parsedIntent) {
        if (response == null || CollectionUtils.isEmpty(response.getItems())) {
            return buildNoResultAnswer(latestMessage, parsedIntent, "knowledge_only", "knowledge_empty");
        }
        KnowledgeBase top = response.getItems().get(0);
        FinalAnswer finalAnswer = baseAnswer(AnswerType.FAQ_ANSWER);
        finalAnswer.setSummary(firstNonBlank(top.getSummary(), top.getTitle(), "已整理出相关知识答案。"));
        finalAnswer.setAnswerText(buildKnowledgeNarrative(response.getItems()));
        finalAnswer.getComposerMeta().getUsedSources().add("knowledge_base");
        finalAnswer.getComposerMeta().getMetadata().put("knowledgeHits", response.getHitCount());
        finalAnswer.getComposerMeta().getMetadata().put("queryVersion", response.getQueryVersion());
        for (KnowledgeBase item : response.getItems()) {
            finalAnswer.getCitations().add(buildKnowledgeCitation(item));
        }
        finalAnswer.getNextActions().add("如果还想追问细节，可以继续补充问题。");
        finalAnswer.getNextActions().add("如果你想找对应商品，也可以直接让我搜索。");
        return finalAnswer;
    }

    public FinalAnswer buildSearchKnowledgeAnswer(List<ProductSearchSnapshot> items,
                                                  long total,
                                                  ProductSearchQuery query,
                                                  KnowledgeRetrievalResponse knowledgeResponse,
                                                  AgentChatMessage latestMessage,
                                                  ParsedIntent parsedIntent) {
        FinalAnswer finalAnswer = buildProductSearchAnswer(items, total, query, latestMessage, parsedIntent);
        if (knowledgeResponse == null || CollectionUtils.isEmpty(knowledgeResponse.getItems())) {
            return finalAnswer;
        }
        KnowledgeBase top = knowledgeResponse.getItems().get(0);
        finalAnswer.setAnswerText(finalAnswer.getAnswerText()
                + " "
                + "补充说明："
                + firstNonBlank(top.getSummary(), trimText(top.getContent(), 120), top.getTitle()));
        finalAnswer.getComposerMeta().getUsedSources().add("knowledge_base");
        finalAnswer.getComposerMeta().getMetadata().put("knowledgeHits", knowledgeResponse.getHitCount());
        for (KnowledgeBase item : knowledgeResponse.getItems()) {
            finalAnswer.getCitations().add(buildKnowledgeCitation(item));
        }
        finalAnswer.getNextActions().add("如果想看规则、退款或使用说明，可以继续追问某个结果。");
        return finalAnswer;
    }

    public FinalAnswer buildSearchRealtimeAnswer(List<ProductSearchSnapshot> items,
                                                 long total,
                                                 ProductSearchQuery query,
                                                 RealtimeQueryResponse realtimeResponse,
                                                 AgentChatMessage latestMessage,
                                                 ParsedIntent parsedIntent) {
        FinalAnswer finalAnswer = buildProductSearchAnswer(items, total, query, latestMessage, parsedIntent);
        finalAnswer.getComposerMeta().getUsedSources().add("realtime_service");

        if (realtimeResponse == null) {
            finalAnswer.getComposerMeta().setDegraded(true);
            finalAnswer.getComposerMeta().setDegradeReason("realtime_unavailable");
            finalAnswer.getDisclaimers().add("当前未能补充实时状态，以下结果仅来自结构化检索。");
            return finalAnswer;
        }

        Map<String, RealtimeResultItem> realtimeByEntityId = new LinkedHashMap<String, RealtimeResultItem>();
        if (!CollectionUtils.isEmpty(realtimeResponse.getItems())) {
            for (RealtimeResultItem item : realtimeResponse.getItems()) {
                if (item != null && StringUtils.hasText(item.getEntityId()) && !realtimeByEntityId.containsKey(item.getEntityId())) {
                    realtimeByEntityId.put(item.getEntityId(), item);
                }
            }
        }

        int realtimeMatched = 0;
        for (FinalAnswer.EntityCard card : finalAnswer.getCards()) {
            if (!StringUtils.hasText(card.getEntityId())) {
                continue;
            }
            RealtimeResultItem realtimeItem = realtimeByEntityId.get(card.getEntityId());
            if (realtimeItem == null) {
                continue;
            }
            realtimeMatched++;
            card.setRealtimeStatusText(safeText(realtimeItem.getAvailabilityStatus(), "未知"));
            card.getHighlights().add("实时状态：" + safeText(realtimeItem.getAvailabilityStatus(), "未知"));
            if (realtimeItem.getInventoryCount() != null) {
                card.getHighlights().add("实时库存：" + realtimeItem.getInventoryCount());
            }
            if (realtimeItem.getPrice() != null) {
                card.getHighlights().add("实时价格：" + realtimeItem.getPrice().stripTrailingZeros().toPlainString() + " " + safeText(realtimeItem.getCurrency(), "CNY"));
            }
            if (Boolean.TRUE.equals(realtimeItem.getBookable())) {
                card.getTags().add("实时可下单");
            }
            if (StringUtils.hasText(realtimeItem.getSource())) {
                card.setSourceLabel(firstNonBlank(card.getSourceLabel(), "product_search_snapshot") + " + " + realtimeItem.getSource());
            }
            finalAnswer.getCitations().add(buildRealtimeCitation(realtimeItem, new MergedRealtimeItem(copyItem(realtimeItem))));
        }

        finalAnswer.getComposerMeta().getMetadata().put("realtimeMatchedCount", realtimeMatched);
        finalAnswer.getComposerMeta().getMetadata().put("realtimeStatus",
                realtimeResponse.getRealtimeStatus() == null ? null : realtimeResponse.getRealtimeStatus().getCode());
        finalAnswer.getComposerMeta().getMetadata().put("realtimeQueryMeta", realtimeResponse.getQueryMeta());
        finalAnswer.getComposerMeta().getMetadata().put("partialFailedIds", realtimeResponse.getPartialFailedIds());

        if (realtimeMatched > 0) {
            finalAnswer.setSummary("共找到 " + total + " 个商品，其中 " + realtimeMatched + " 个已补充实时状态。");
            finalAnswer.setAnswerText(buildSearchRealtimeNarrative(items, realtimeByEntityId));
            finalAnswer.getNextActions().add("如果你要确认某个商品，可以继续指定商品 ID 做实时核验。");
        } else {
            finalAnswer.getDisclaimers().add("已找到候选商品，但当前没有补充到可用的实时结果。");
        }

        if (realtimeResponse.getRealtimeStatus() != RealtimeStatus.SUCCESS) {
            finalAnswer.getComposerMeta().setDegraded(true);
            finalAnswer.getComposerMeta().setDegradeReason(realtimeResponse.getRealtimeStatus().getCode());
            finalAnswer.getDisclaimers().add("实时状态为部分结果或降级结果，执行操作前请再次确认。");
        }
        if (!CollectionUtils.isEmpty(realtimeResponse.getPartialFailedIds())) {
            finalAnswer.getDisclaimers().add("以下实体 ID 未能完成实时确认：" + realtimeResponse.getPartialFailedIds());
        }
        return finalAnswer;
    }

    public FinalAnswer buildNoResultAnswer(AgentChatMessage latestMessage,
                                           ParsedIntent parsedIntent,
                                           String routePlan,
                                           String reason) {
        FinalAnswer finalAnswer = baseAnswer(AnswerType.NO_RESULT);
        String queryText = latestMessage == null ? null : latestMessage.getContent();
        finalAnswer.setSummary("当前没有可直接返回的检索或知识结果。");
        finalAnswer.setAnswerText("我理解了你的需求，但当前没有命中可直接返回的检索结果或知识答案。");
        if (StringUtils.hasText(queryText)) {
            finalAnswer.getComposerMeta().getMetadata().put("queryText", queryText);
        }
        finalAnswer.getComposerMeta().getMetadata().put("routePlan", routePlan);
        finalAnswer.getComposerMeta().getMetadata().put("reason", reason);
        if (parsedIntent != null && parsedIntent.getTaskType() != null) {
            finalAnswer.getComposerMeta().getMetadata().put("taskType", parsedIntent.getTaskType().getCode());
        }
        finalAnswer.getComposerMeta().getUsedSources().add("agent_runtime");
        finalAnswer.getDisclaimers().add("当前分支没有命中匹配的结构化结果或知识结果。");
        finalAnswer.getNextActions().add("可以换一个更具体的关键词、商品 ID 或规则名称再试。");
        finalAnswer.getNextActions().add("如果要查实时状态，请显式提供商品 ID。");
        FinalAnswer.Citation citation = new FinalAnswer.Citation();
        citation.setSourceType("agent_runtime");
        citation.setSourceId(routePlan);
        citation.setTitle("结构化运行时");
        citation.setSnippet("未命中结果，原因：" + safeText(reason, "unknown"));
        citation.setConfidence(0.35D);
        finalAnswer.getCitations().add(citation);
        return finalAnswer;
    }

    public FinalAnswer buildFallbackAnswer(AgentChatRequest request,
                                           AgentChatMessage latestMessage,
                                           ParsedIntent parsedIntent) {
        TaskType taskType = parsedIntent == null || parsedIntent.getTaskType() == null
                ? TaskType.CHITCHAT
                : parsedIntent.getTaskType();
        CandidateSlots slots = parsedIntent == null || parsedIntent.getCandidateSlots() == null
                ? new CandidateSlots()
                : parsedIntent.getCandidateSlots();
        FinalAnswer finalAnswer = baseAnswer(resolveFallbackAnswerType(taskType));
        finalAnswer.setSummary(buildFallbackSummary(taskType, slots));
        finalAnswer.setAnswerText(buildFallbackNarrative(taskType, latestMessage, slots));
        finalAnswer.getComposerMeta().getUsedSources().add("agent_runtime");
        finalAnswer.getComposerMeta().getUsedSources().add("intent_parser");
        finalAnswer.getComposerMeta().getMetadata().put("taskType", taskType.getCode());
        finalAnswer.getComposerMeta().getMetadata().put("slotSummary", buildSlotSummary(slots));
        finalAnswer.getComposerMeta().getMetadata().put("needExplanation", parsedIntent != null && parsedIntent.isNeedExplanation());
        finalAnswer.getComposerMeta().getMetadata().put("needRecommendation", parsedIntent != null && parsedIntent.isNeedRecommendation());
        finalAnswer.getDisclaimers().add(buildFallbackDisclaimer(taskType));
        finalAnswer.getNextActions().addAll(buildFallbackNextActions(taskType, slots));
        finalAnswer.getCitations().add(buildFallbackCitation(taskType, latestMessage, slots));
        return finalAnswer;
    }

    public void finalizeAnswer(FinalAnswer finalAnswer,
                               SessionState state,
                               SessionState.ExecutionMeta executionMeta,
                               ParsedIntent parsedIntent,
                               FinalAnswer.DebugTrace debugTrace) {
        if (!StringUtils.hasText(finalAnswer.getSummary())) {
            finalAnswer.setSummary(finalAnswer.getAnswerText());
        }
        finalAnswer.getComposerMeta().setDegraded(executionMeta.isDegraded() || finalAnswer.getComposerMeta().isDegraded());
        if (!StringUtils.hasText(finalAnswer.getComposerMeta().getDegradeReason())) {
            finalAnswer.getComposerMeta().setDegradeReason(executionMeta.getErrorCode());
        }
        finalAnswer.getComposerMeta().getMetadata().put("completedNodes", executionMeta.getCompletedNodes());
        finalAnswer.getComposerMeta().getMetadata().put("taskType",
                parsedIntent == null || parsedIntent.getTaskType() == null ? null : parsedIntent.getTaskType().getCode());
        finalAnswer.getComposerMeta().getMetadata().put("intermediateData", state.getIntermediateData());

        debugTrace.setTotalDurationMs(executionMeta.getDurationMs());
        debugTrace.setDegraded(finalAnswer.getComposerMeta().isDegraded());
        debugTrace.setDegradeReason(finalAnswer.getComposerMeta().getDegradeReason());
        debugTrace.getMetadata().put("completedNodes", executionMeta.getCompletedNodes());
        debugTrace.getMetadata().put("failedNode", executionMeta.getFailedNode());
        debugTrace.getMetadata().put("errorCode", executionMeta.getErrorCode());
        debugTrace.getMetadata().put("conflictDetected", finalAnswer.getComposerMeta().isConflictDetected());
        normalizeOutput(finalAnswer);
        finalAnswer.setDebugTrace(debugTrace);
    }

    private List<MergedRealtimeItem> mergeRealtimeItems(List<RealtimeResultItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, MergedRealtimeItem> merged = new LinkedHashMap<String, MergedRealtimeItem>();
        for (RealtimeResultItem item : items) {
            if (item == null || !StringUtils.hasText(item.getEntityId())) {
                continue;
            }
            MergedRealtimeItem current = merged.get(item.getEntityId());
            if (current == null) {
                merged.put(item.getEntityId(), new MergedRealtimeItem(copyItem(item)));
            } else {
                current.merge(item);
            }
        }
        return new ArrayList<MergedRealtimeItem>(merged.values());
    }

    private String buildRealtimeSummary(RealtimeQueryResponse response, List<MergedRealtimeItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return "当前没有可用的实时结果。";
        }
        String prefix = response != null && response.getRealtimeStatus() == RealtimeStatus.SUCCESS
                ? "已完成实时确认，涉及 "
                : "实时结果已部分返回，涉及 ";
        return prefix + items.size() + " 个实体。";
    }

    private String buildRealtimeNarrative(RealtimeQueryResponse response, List<MergedRealtimeItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return "当前没有可用的实时结果。";
        }
        List<String> fragments = new ArrayList<String>();
        for (MergedRealtimeItem mergedItem : items) {
            fragments.add(buildRealtimeFragment(mergedItem.getItem(), mergedItem.hasConflict()));
        }
        String prefix = response != null && response.getRealtimeStatus() == RealtimeStatus.SUCCESS
                ? "实时状态如下："
                : "当前仅拿到部分实时状态：";
        return prefix + String.join("；", fragments);
    }

    private String buildRealtimeFragment(RealtimeResultItem item, boolean conflictDetected) {
        List<String> parts = new ArrayList<String>();
        parts.add("#" + item.getEntityId());
        parts.add("可用状态" + safeText(item.getAvailabilityStatus(), "未知"));
        if (item.getInventoryCount() != null) {
            parts.add("库存" + item.getInventoryCount());
        }
        if (item.getPrice() != null) {
            parts.add("价格" + item.getPrice().toPlainString() + " " + safeText(item.getCurrency(), "CNY"));
        }
        if (item.getOpenNow() != null) {
            parts.add(item.getOpenNow() ? "营业中" : "未营业");
        }
        if (conflictDetected) {
            parts.add("存在冲突");
        }
        return String.join("，", parts);
    }

    private String buildCardSubtitle(RealtimeResultItem item, boolean conflictDetected) {
        List<String> parts = new ArrayList<String>();
        parts.add("可用状态：" + safeText(item.getAvailabilityStatus(), "未知"));
        parts.add("库存状态：" + safeText(item.getInventoryStatus(), "未知"));
        parts.add("售卖状态：" + safeText(item.getSellStatus(), "未知"));
        if (conflictDetected) {
            parts.add("存在冲突：是");
        }
        return String.join(" | ", parts);
    }

    private String buildRecommendReason(RealtimeResultItem item, MergedRealtimeItem mergedItem) {
        List<String> reasons = new ArrayList<String>();
        if (Boolean.TRUE.equals(item.getBookable())) {
            reasons.add("当前可下单");
        }
        if (item.getInventoryCount() != null && item.getInventoryCount() > 0) {
            reasons.add("库存可用");
        }
        if (item.getPrice() != null) {
            reasons.add("已返回价格");
        }
        if (mergedItem.getSourceCount() > 1) {
            reasons.add("已合并 " + mergedItem.getSourceCount() + " 个实时来源");
        }
        return reasons.isEmpty() ? "已获取实时状态" : String.join("，", reasons);
    }

    private FinalAnswer.Citation buildRealtimeCitation(RealtimeResultItem item, MergedRealtimeItem mergedItem) {
        FinalAnswer.Citation citation = new FinalAnswer.Citation();
        citation.setSourceType("realtime_service");
        citation.setSourceId(item.getEntityId());
        citation.setTitle(mergedItem.getSourceCount() > 1
                ? "实体 #" + item.getEntityId() + " 的合并实时结果"
                : "实体 #" + item.getEntityId() + " 的实时结果");
        citation.setDocId(item.getEntityId());
        citation.setDocTitle("实时状态");
        citation.setSnippet(buildRealtimeFragment(item, mergedItem.hasConflict()) + " | 来源：" + mergedItem.getSourceLabel());
        citation.setConfidence(mergedItem.hasConflict() ? 0.55D : (item.isDegraded() ? 0.6D : 0.95D));
        return citation;
    }

    private RealtimeResultItem copyItem(RealtimeResultItem source) {
        RealtimeResultItem copy = new RealtimeResultItem();
        copy.setEntityId(source.getEntityId());
        copy.setInventoryStatus(source.getInventoryStatus());
        copy.setInventoryCount(source.getInventoryCount());
        copy.setSellStatus(source.getSellStatus());
        copy.setAvailabilityStatus(source.getAvailabilityStatus());
        copy.setBookable(source.getBookable());
        copy.setPrice(source.getPrice());
        copy.setCurrency(source.getCurrency());
        copy.setBusinessStatus(source.getBusinessStatus());
        copy.setOpenNow(source.getOpenNow());
        copy.setQueryTs(source.getQueryTs());
        copy.setSuccess(source.isSuccess());
        copy.setDegraded(source.isDegraded());
        copy.setSource(source.getSource());
        copy.setErrorCode(source.getErrorCode());
        copy.setErrorMessage(source.getErrorMessage());
        return copy;
    }

    private FinalAnswer baseAnswer(AnswerType answerType) {
        FinalAnswer finalAnswer = new FinalAnswer();
        finalAnswer.setAnswerType(answerType);
        return finalAnswer;
    }

    private String buildRouteDataNarrative(List<RouteEntityCandidate> items, String entityLabel) {
        List<String> fragments = new ArrayList<String>();
        for (int index = 0; index < Math.min(items.size(), 3); index++) {
            RouteEntityCandidate item = items.get(index);
            List<String> suffix = new ArrayList<String>();
            if (StringUtils.hasText(item.getLocationText())) {
                suffix.add(item.getLocationText());
            }
            if (!CollectionUtils.isEmpty(item.getHighlights())) {
                suffix.add(item.getHighlights().get(0));
            }
            String text = safeText(item.getTitle(), entityLabel + "#" + (index + 1));
            if (!suffix.isEmpty()) {
                text += "（" + String.join("，", suffix) + "）";
            }
            fragments.add(text);
        }
        return "优先命中的" + entityLabel + "有：" + String.join("；", fragments) + "。";
    }

    private FinalAnswer.EntityCard buildRouteCard(RouteEntityCandidate item, NormalizedRouteData routeData) {
        FinalAnswer.EntityCard card = new FinalAnswer.EntityCard();
        card.setEntityId(item.getEntityId());
        card.setEntityType(routeData == null ? null : routeData.getEntityType());
        card.setTitle(firstNonBlank(item.getTitle(), routeEntityLabel(routeData == null ? null : routeData.getEntityType())));
        card.setSubtitle(item.getSubtitle());
        card.setImageUrl(item.getImageUrl());
        card.setPriceText(item.getPriceText());
        card.setLocationText(item.getLocationText());
        card.setRealtimeStatusText(item.getRealtimeStatusText());
        card.setTags(dedupeAndLimit(item.getTags(), 6));
        card.setHighlights(dedupeAndLimit(item.getHighlights(), 6));
        card.setSourceLabel(routeData == null ? "api_routes" : firstNonBlank(routeData.getSourceResource(), "api_routes"));
        card.setRecommendReason("来自后端结构化接口 " + (routeData == null ? "" : safeText(routeData.getSourceAction(), "read")));
        return card;
    }

    private FinalAnswer.Citation buildRouteCitation(RouteEntityCandidate item, NormalizedRouteData routeData) {
        FinalAnswer.Citation citation = new FinalAnswer.Citation();
        citation.setSourceType("api_routes");
        citation.setSourceId(routeData == null ? null : routeData.getSourceRouteId());
        citation.setTitle(firstNonBlank(item.getTitle(), routeEntityLabel(routeData == null ? null : routeData.getEntityType())));
        citation.setDocId(item.getEntityId());
        citation.setDocTitle(routeData == null ? null : routeData.getSourceResource());
        citation.setSnippet(firstNonBlank(item.getSubtitle(), item.getLocationText(), "后端结构化接口返回结果"));
        citation.setConfidence(0.86D);
        return citation;
    }

    private String routeEntityLabel(String entityType) {
        if ("event".equalsIgnoreCase(entityType)) {
            return "活动";
        }
        if ("store".equalsIgnoreCase(entityType)) {
            return "门店";
        }
        if ("product".equalsIgnoreCase(entityType)) {
            return "商品";
        }
        return "结果";
    }

    private String buildSearchNarrative(List<ProductSearchSnapshot> items) {
        List<String> fragments = new ArrayList<String>();
        for (int index = 0; index < Math.min(items.size(), 3); index++) {
            ProductSearchSnapshot item = items.get(index);
            fragments.add(item.getTitle() + formatPrice(item) + formatCity(item));
        }
        return "优先命中的商品有：" + String.join("；", fragments);
    }

    private FinalAnswer.EntityCard buildProductCard(ProductSearchSnapshot item) {
        FinalAnswer.EntityCard card = new FinalAnswer.EntityCard();
        card.setEntityId(item.getProductId() == null ? null : String.valueOf(item.getProductId()));
        card.setEntityType("product");
        card.setTitle(firstNonBlank(item.getTitle(), "商品 #" + item.getProductId()));
        card.setSubtitle(firstNonBlank(item.getSubtitle(), item.getSummaryText(), item.getStoreName()));
        card.setImageUrl(item.getCoverImage());
        card.setPriceText(item.getDisplayPrice() == null ? null : item.getDisplayPrice().toPlainString() + " " + safeText(item.getCurrency(), "CNY"));
        card.setLocationText(firstNonBlank(item.getDistrictName(), item.getCityName()));
        card.setSourceLabel("product_search_snapshot");
        if (StringUtils.hasText(item.getCategoryName())) {
            card.getTags().add(item.getCategoryName());
        }
        if (StringUtils.hasText(item.getCityName())) {
            card.getTags().add(item.getCityName());
        }
        if (item.getRating() != null) {
            card.getHighlights().add("评分：" + item.getRating().stripTrailingZeros().toPlainString());
        }
        if (item.getSalesCount() != null) {
            card.getHighlights().add("销量：" + item.getSalesCount());
        }
        if (StringUtils.hasText(item.getSummaryText())) {
            card.getHighlights().add(trimText(item.getSummaryText(), 72));
        }
        card.setRecommendReason("根据结构化检索结果匹配。");
        return card;
    }

    private FinalAnswer.Citation buildProductCitation(ProductSearchSnapshot item) {
        FinalAnswer.Citation citation = new FinalAnswer.Citation();
        citation.setSourceType("product_search_snapshot");
        citation.setSourceId(item.getProductId() == null ? null : String.valueOf(item.getProductId()));
        citation.setTitle(firstNonBlank(item.getTitle(), "商品 #" + item.getProductId()));
        citation.setDocId(item.getId() == null ? null : String.valueOf(item.getId()));
        citation.setDocTitle(firstNonBlank(item.getCategoryName(), "商品快照"));
        citation.setSnippet(firstNonBlank(item.getSummaryText(), item.getSubtitle(), item.getStoreName()));
        citation.setConfidence(0.88D);
        return citation;
    }

    private String buildKnowledgeNarrative(List<KnowledgeBase> items) {
        List<String> fragments = new ArrayList<String>();
        for (int index = 0; index < Math.min(items.size(), 2); index++) {
            KnowledgeBase item = items.get(index);
            fragments.add(firstNonBlank(item.getSummary(), trimText(item.getContent(), 120), item.getTitle()));
        }
        return String.join(" ", fragments);
    }

    private String buildSearchRealtimeNarrative(List<ProductSearchSnapshot> items,
                                                Map<String, RealtimeResultItem> realtimeByEntityId) {
        List<String> fragments = new ArrayList<String>();
        for (int index = 0; index < Math.min(items.size(), 3); index++) {
            ProductSearchSnapshot item = items.get(index);
            StringBuilder builder = new StringBuilder();
            builder.append(firstNonBlank(item.getTitle(), "商品 #" + item.getProductId()));
            RealtimeResultItem realtimeItem = realtimeByEntityId.get(item.getProductId() == null ? null : String.valueOf(item.getProductId()));
            if (realtimeItem != null) {
                builder.append("（实时状态：")
                        .append(safeText(realtimeItem.getAvailabilityStatus(), "未知"));
                if (realtimeItem.getInventoryCount() != null) {
                    builder.append("，库存").append(realtimeItem.getInventoryCount());
                }
                builder.append("）");
            }
            fragments.add(builder.toString());
        }
        return "已为你先检索商品，再补充实时状态：" + String.join("；", fragments);
    }

    private FinalAnswer.Citation buildKnowledgeCitation(KnowledgeBase item) {
        FinalAnswer.Citation citation = new FinalAnswer.Citation();
        citation.setSourceType("knowledge_base");
        citation.setSourceId(item.getId() == null ? null : String.valueOf(item.getId()));
        citation.setTitle(firstNonBlank(item.getTitle(), "知识 #" + item.getId()));
        citation.setDocId(item.getId() == null ? null : String.valueOf(item.getId()));
        citation.setDocTitle(firstNonBlank(item.getDocType(), item.getCategory(), "知识文档"));
        citation.setSnippet(firstNonBlank(item.getSummary(), trimText(item.getContent(), 140), item.getKeywords()));
        citation.setConfidence(0.82D);
        return citation;
    }

    private String formatPrice(ProductSearchSnapshot item) {
        if (item.getDisplayPrice() == null) {
            return "";
        }
        return " (" + item.getDisplayPrice().stripTrailingZeros().toPlainString() + " " + safeText(item.getCurrency(), "CNY") + ")";
    }

    private String formatCity(ProductSearchSnapshot item) {
        if (!StringUtils.hasText(item.getCityName())) {
            return "";
        }
        return "（" + item.getCityName() + "）";
    }

    private String trimText(String value, int limit) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String safe = value.trim().replaceAll("\\s+", " ");
        if (safe.length() <= limit) {
            return safe;
        }
        return safe.substring(0, Math.max(0, limit - 3)) + "...";
    }

    private AnswerType resolveFallbackAnswerType(TaskType taskType) {
        if (taskType == null) {
            return AnswerType.NO_RESULT;
        }
        switch (taskType) {
            case PRODUCT_SEARCH:
            case EVENT_SEARCH:
            case STORE_SEARCH:
                return AnswerType.RECOMMENDATION;
            case FAQ_QUERY:
                return AnswerType.FAQ_ANSWER;
            case FOLLOW_UP:
            case MIXED_SEARCH_KNOWLEDGE:
            case MIXED_SEARCH_REALTIME:
                return AnswerType.PARTIAL_RESULT;
            case CLARIFICATION_RESPONSE:
                return AnswerType.CLARIFICATION;
            default:
                return AnswerType.NO_RESULT;
        }
    }

    private String buildFallbackSummary(TaskType taskType, CandidateSlots slots) {
        switch (taskType) {
            case PRODUCT_SEARCH:
                return "已识别为商品搜索意图，但这条分支还没有完成结果拼装。";
            case EVENT_SEARCH:
                return "已识别为活动搜索意图，但这条分支还没有完成推荐结果拼装。";
            case STORE_SEARCH:
                return "已识别为门店搜索意图，但这条分支还没有完成门店结果拼装。";
            case FAQ_QUERY:
                return "已识别为知识问答意图，但这条分支暂未返回可落地的 FAQ 答案。";
            case MIXED_SEARCH_KNOWLEDGE:
                return "已识别为搜索加知识的混合意图，但当前只生成了意图摘要。";
            case MIXED_SEARCH_REALTIME:
                return "已识别为搜索加实时的混合意图，但当前只生成了部分计划摘要。";
            case FOLLOW_UP:
                return "已识别为追问意图，但这条分支还需要更完整的续问拼装。";
            case CLARIFICATION_RESPONSE:
                return "已识别为澄清回复，但后续执行路径在这里还没有完整拼装。";
            default:
                return StringUtils.hasText(slots.getKeyword())
                        ? "已识别到请求上下文，但当前还没有为这个意图接上专门的回答路径。"
                        : "当前这条非实时请求仍在使用简化的兜底回答。";
        }
    }

    private String buildFallbackNarrative(TaskType taskType,
                                          AgentChatMessage latestMessage,
                                          CandidateSlots slots) {
        String queryText = latestMessage == null ? null : latestMessage.getContent();
        String focus = firstNonBlank(slots.getKeyword(), slots.getCategoryText(), slots.getCityText(), slots.getEntityRef(), queryText);
        switch (taskType) {
            case PRODUCT_SEARCH:
                return "我把这条请求识别为商品搜索"
                        + buildScopedPhrase(slots)
                        + "，但这条分支还没有把结构化商品结果完整拼装出来。";
            case EVENT_SEARCH:
                return "我把这条请求识别为活动搜索"
                        + buildScopedPhrase(slots)
                        + "，但这条分支还需要更完整的活动推荐拼装。";
            case STORE_SEARCH:
                return "我把这条请求识别为门店搜索"
                        + buildScopedPhrase(slots)
                        + "，但这条分支还需要更完整的门店推荐拼装。";
            case FAQ_QUERY:
                return "我把这条请求识别为知识或规则问题，主题是"
                        + safeText(focus, "当前主题")
                        + "，但这条兜底路径还没有拼装出可落地的 FAQ 答案。";
            case MIXED_SEARCH_KNOWLEDGE:
                return "我把这条请求识别为“搜索 + 说明”的混合意图"
                        + buildScopedPhrase(slots)
                        + "，当前这条兜底路径只能返回意图级的部分答案。";
            case MIXED_SEARCH_REALTIME:
                return "我把这条请求识别为“搜索 + 实时确认”的混合意图"
                        + buildScopedPhrase(slots)
                        + "，当前这条兜底路径只能返回意图级的部分答案。";
            case FOLLOW_UP:
                return "我把这条请求识别为关于"
                        + safeText(focus, "当前上下文")
                        + "的追问，但这条分支还需要更丰富的续问渲染。";
            case CLARIFICATION_RESPONSE:
                return "我把这条请求识别为关于"
                        + safeText(focus, "上一个问题")
                        + "的澄清回复，但这条分支的下一步回答拼装仍是简化版。";
            default:
                if (!StringUtils.hasText(queryText)) {
                    return "当前兜底路径收到了空消息。";
                }
                return "当前兜底路径识别到了这条请求，但还没有为该路径拼装出更完整的回答：" + queryText;
        }
    }

    private String buildFallbackDisclaimer(TaskType taskType) {
        switch (taskType) {
            case FAQ_QUERY:
                return "这是一条基于意图的兜底回答，当前还没有真正落到知识检索结果上。";
            case MIXED_SEARCH_KNOWLEDGE:
            case MIXED_SEARCH_REALTIME:
                return "这条回答只总结了识别出的意图，混合工具的拼装路径还不完整。";
            case PRODUCT_SEARCH:
            case EVENT_SEARCH:
            case STORE_SEARCH:
                return "这条回答只反映了已识别的搜索意图，当前路径还没有拼装出结果卡片。";
            default:
                return "当前仍在使用简化的 runtime 兜底回答。";
        }
    }

    private List<String> buildFallbackNextActions(TaskType taskType, CandidateSlots slots) {
        List<String> actions = new ArrayList<String>();
        switch (taskType) {
            case PRODUCT_SEARCH:
            case EVENT_SEARCH:
            case STORE_SEARCH:
                actions.add("继续为这个意图接通结构化搜索执行和推荐结果拼装。");
                if (!StringUtils.hasText(slots.getKeyword()) && !StringUtils.hasText(slots.getCategoryText())) {
                    actions.add("如果结果质量不稳定，优先让用户补充更明确的关键词或类目。");
                }
                break;
            case FAQ_QUERY:
                actions.add("先把这条路径真正接到知识检索，再依赖这里的回答。");
                actions.add("知识检索接通后，补回对应文档引用。");
                break;
            case MIXED_SEARCH_KNOWLEDGE:
            case MIXED_SEARCH_REALTIME:
                actions.add("把混合工具的执行结果接到更完整的 composer 回答里。");
                actions.add("在一条回答里同时返回搜索结果和说明或实时确认。");
                break;
            case FOLLOW_UP:
            case CLARIFICATION_RESPONSE:
                actions.add("先把上一轮上下文带入下一步执行，再拼装最终回答。");
                break;
            default:
                actions.add("继续为这个意图接通结构化搜索和知识拼装。");
                break;
        }
        return actions;
    }

    private FinalAnswer.Citation buildFallbackCitation(TaskType taskType,
                                                       AgentChatMessage latestMessage,
                                                       CandidateSlots slots) {
        FinalAnswer.Citation citation = new FinalAnswer.Citation();
        citation.setSourceType("agent_runtime");
        citation.setSourceId("runtime_fallback");
        citation.setTitle("运行时兜底");
        citation.setDocId(taskType == null ? null : taskType.getCode());
        citation.setDocTitle("意图级兜底回答");
        citation.setSnippet("兜底回答生成自 taskType="
                + (taskType == null ? "unknown" : taskType.getCode())
                + "，slots=" + buildSlotSummary(slots)
                + "，query=" + safeText(latestMessage == null ? null : latestMessage.getContent(), "empty"));
        citation.setConfidence(0.55D);
        return citation;
    }

    private String buildScopedPhrase(CandidateSlots slots) {
        List<String> scopes = new ArrayList<String>();
        if (StringUtils.hasText(slots.getKeyword())) {
            scopes.add("关键词=" + slots.getKeyword());
        }
        if (StringUtils.hasText(slots.getCategoryText())) {
            scopes.add("类目=" + slots.getCategoryText());
        }
        if (StringUtils.hasText(slots.getCityText())) {
            scopes.add("城市=" + slots.getCityText());
        }
        if (StringUtils.hasText(slots.getDateText())) {
            scopes.add("时间=" + slots.getDateText());
        }
        return scopes.isEmpty() ? "" : " (" + String.join(", ", scopes) + ")";
    }

    private Map<String, Object> buildSlotSummary(CandidateSlots slots) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<String, Object>();
        putIfPresent(summary, "keyword", slots.getKeyword());
        putIfPresent(summary, "categoryText", slots.getCategoryText());
        putIfPresent(summary, "crowdTagText", slots.getCrowdTagText());
        putIfPresent(summary, "sceneTagText", slots.getSceneTagText());
        putIfPresent(summary, "cityText", slots.getCityText());
        putIfPresent(summary, "districtText", slots.getDistrictText());
        putIfPresent(summary, "locationText", slots.getLocationText());
        putIfPresent(summary, "priceText", slots.getPriceText());
        putIfPresent(summary, "dateText", slots.getDateText());
        putIfPresent(summary, "sortText", slots.getSortText());
        putIfPresent(summary, "entityType", slots.getEntityType());
        putIfPresent(summary, "entityRef", slots.getEntityRef());
        return summary;
    }

    private void normalizeOutput(FinalAnswer finalAnswer) {
        finalAnswer.setDisclaimers(dedupeAndLimit(finalAnswer.getDisclaimers(), 4));
        finalAnswer.setNextActions(dedupeAndLimit(finalAnswer.getNextActions(), 4));
        for (FinalAnswer.EntityCard card : finalAnswer.getCards()) {
            card.setTags(dedupeAndLimit(card.getTags(), 6));
            card.setHighlights(dedupeAndLimit(card.getHighlights(), 6));
        }
        dedupeCitations(finalAnswer);
        if (finalAnswer.getNextActions().isEmpty()) {
            finalAnswer.getNextActions().add(defaultNextAction(finalAnswer.getAnswerType()));
        }
    }

    private List<String> dedupeAndLimit(List<String> values, int limit) {
        LinkedHashSet<String> unique = new LinkedHashSet<String>();
        if (values != null) {
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    unique.add(value.trim());
                }
                if (unique.size() >= limit) {
                    break;
                }
            }
        }
        return new ArrayList<String>(unique);
    }

    private void dedupeCitations(FinalAnswer finalAnswer) {
        LinkedHashMap<String, FinalAnswer.Citation> unique = new LinkedHashMap<String, FinalAnswer.Citation>();
        for (FinalAnswer.Citation citation : finalAnswer.getCitations()) {
            if (citation == null) {
                continue;
            }
            String key = safeText(citation.getSourceType(), "unknown")
                    + "|"
                    + safeText(firstNonBlank(citation.getSourceId(), citation.getDocId(), citation.getTitle()), "unknown");
            if (!unique.containsKey(key)) {
                unique.put(key, citation);
            }
        }
        finalAnswer.setCitations(new ArrayList<FinalAnswer.Citation>(unique.values()));
    }

    private String defaultNextAction(AnswerType answerType) {
        if (answerType == AnswerType.CLARIFICATION) {
            return "补充缺失信息后，我就可以继续执行下一步。";
        }
        if (answerType == AnswerType.PARTIAL_RESULT) {
            return "等缺失数据源恢复后，再重试这次请求。";
        }
        return "你也可以继续补充条件，我再继续处理。";
    }

    private void putIfPresent(Map<String, Object> target, String key, String value) {
        if (StringUtils.hasText(value)) {
            target.put(key, value);
        }
    }

    private String buildActionConfirmationText(ActionConversationStore.PendingAction pendingAction) {
        if (pendingAction == null) {
            return "\u6211\u5df2\u6536\u96c6\u5230\u8fd9\u6b21\u5199\u64cd\u4f5c\u7684\u53c2\u6570\uff0c\u56de\u590d\u201c\u786e\u8ba4\u6267\u884c\u201d\u540e\u6211\u5c31\u4f1a\u7ee7\u7eed\u3002";
        }
        List<String> parts = new ArrayList<String>();
        if (!CollectionUtils.isEmpty(pendingAction.getPayload())) {
            appendIfPresent(parts, "\u540d\u79f0", pendingAction.getPayload().get("title"));
            appendIfPresent(parts, "\u4ef7\u683c", pendingAction.getPayload().get("price"));
            appendIfPresent(parts, "\u5e93\u5b58", pendingAction.getPayload().get("stockQuantity"));
            appendIfPresent(parts, "\u5730\u70b9", pendingAction.getPayload().get("location"));
            appendIfPresent(parts, "\u56fe\u7247", summarizeImages(pendingAction.getPayload().get("imageUrls")));
            appendIfPresent(parts, "\u6d3b\u52a8\u5206\u7c7b", pendingAction.getPayload().get("category"));
            appendIfPresent(parts, "\u6d3b\u52a8\u65e5\u671f", pendingAction.getPayload().get("date"));
            appendIfPresent(parts, "\u5f00\u59cb\u65f6\u95f4", pendingAction.getPayload().get("timeStart"));
            appendIfPresent(parts, "\u7ed3\u675f\u65f6\u95f4", pendingAction.getPayload().get("timeEnd"));
            appendIfPresent(parts, "\u6d3b\u52a8\u63cf\u8ff0", pendingAction.getPayload().get("description"));
            appendIfPresent(parts, "\u4eba\u6570", pendingAction.getPayload().get("capacity"));
            appendIfPresent(parts, "\u8d39\u7528", pendingAction.getPayload().get("fee"));
        }
        if (!CollectionUtils.isEmpty(pendingAction.getParams())) {
            appendIfPresent(parts, "\u5546\u54c1ID", pendingAction.getParams().get("productId"));
            appendIfPresent(parts, "\u4ef7\u683c", pendingAction.getParams().get("price"));
            appendIfPresent(parts, "\u5730\u70b9", pendingAction.getParams().get("location"));
            appendIfPresent(parts, "\u5e93\u5b58\u589e\u91cf", pendingAction.getParams().get("delta"));
        }
        String detail = parts.isEmpty() ? "\u5f53\u524d\u53c2\u6570\u5df2\u5c31\u7eea" : String.join("\uff0c", parts);
        return "\u8bf7\u786e\u8ba4\u662f\u5426\u6267\u884c" + safeText(pendingAction.getDisplayName(), "\u8fd9\u4e2a\u64cd\u4f5c") + "\uff1a" + detail + "\u3002";
    }

    private String buildActionClarificationText(ActionConversationStore.PendingAction pendingAction) {
        if (pendingAction == null) {
            return "\u8fd8\u9700\u8981\u4f60\u8865\u5145\u8fd9\u6b21\u5199\u64cd\u4f5c\u7684\u5fc5\u8981\u53c2\u6570\u3002";
        }
        List<String> collected = buildCollectedActionFields(pendingAction);
        List<String> missing = pendingAction.getMissingFields() == null
                ? Collections.<String>emptyList()
                : pendingAction.getMissingFields();
        StringBuilder builder = new StringBuilder();
        builder.append("\u6211\u53ef\u4ee5\u7ee7\u7eed\u6267\u884c")
                .append(safeText(pendingAction.getDisplayName(), "\u8fd9\u4e2a\u64cd\u4f5c"))
                .append("\u3002");
        if (!collected.isEmpty()) {
            builder.append("\n\u5df2\u6536\u96c6\uff1a").append(String.join("\uff0c", collected)).append("\u3002");
        }
        if (!missing.isEmpty()) {
            builder.append("\n\u8fd8\u9700\u8865\u5145\uff1a");
            for (int i = 0; i < missing.size(); i++) {
                String field = missing.get(i);
                if (i > 0) {
                    builder.append("\uff1b");
                }
                builder.append(field);
                String example = actionFieldExample(field);
                if (StringUtils.hasText(example)) {
                    builder.append("\uff08\u4f8b\u5982\u201c").append(example).append("\u201d\uff09");
                }
            }
            builder.append("\u3002");
        }
        return builder.toString();
    }

    private List<String> buildCollectedActionFields(ActionConversationStore.PendingAction pendingAction) {
        List<String> parts = new ArrayList<String>();
        if (!CollectionUtils.isEmpty(pendingAction.getPayload())) {
            if ("local_activity".equalsIgnoreCase(pendingAction.getResource())) {
                appendIfPresent(parts, "\u6d3b\u52a8\u540d", pendingAction.getPayload().get("title"));
                appendIfPresent(parts, "\u6d3b\u52a8\u5206\u7c7b", pendingAction.getPayload().get("category"));
                appendIfPresent(parts, "\u6d3b\u52a8\u65e5\u671f", pendingAction.getPayload().get("date"));
                appendIfPresent(parts, "\u5f00\u59cb\u65f6\u95f4", pendingAction.getPayload().get("timeStart"));
                appendIfPresent(parts, "\u7ed3\u675f\u65f6\u95f4", pendingAction.getPayload().get("timeEnd"));
                appendIfPresent(parts, "\u6d3b\u52a8\u5730\u70b9", pendingAction.getPayload().get("location"));
            } else {
                appendIfPresent(parts, "\u5546\u54c1\u540d", pendingAction.getPayload().get("title"));
                appendIfPresent(parts, "\u4ef7\u683c", pendingAction.getPayload().get("price"));
                appendIfPresent(parts, "\u5e93\u5b58", pendingAction.getPayload().get("stockQuantity"));
                appendIfPresent(parts, "\u5206\u7c7b", pendingAction.getPayload().get("categoryId"));
                appendIfPresent(parts, "\u5730\u70b9", pendingAction.getPayload().get("location"));
                appendIfPresent(parts, "\u56fe\u7247", summarizeImages(pendingAction.getPayload().get("imageUrls")));
            }
        }
        if (!CollectionUtils.isEmpty(pendingAction.getParams())) {
            appendIfPresent(parts, "\u5546\u54c1ID", pendingAction.getParams().get("productId"));
            appendIfPresent(parts, "\u4ef7\u683c", pendingAction.getParams().get("price"));
            appendIfPresent(parts, "\u5730\u70b9", pendingAction.getParams().get("location"));
            appendIfPresent(parts, "\u5e93\u5b58\u589e\u91cf", pendingAction.getParams().get("delta"));
        }
        return parts;
    }

    private String actionFieldExample(String field) {
        if (!StringUtils.hasText(field)) {
            return null;
        }
        if (field.contains("\u4ef7\u683c")) {
            return "\u4ef7\u683c 999";
        }
        if (field.contains("\u5e93\u5b58") || field.contains("\u6570\u91cf")) {
            return "\u5e93\u5b58 10";
        }
        if (field.contains("\u5206\u7c7b")) {
            return "\u5206\u7c7b \u624b\u673a";
        }
        if (field.contains("\u5730\u70b9")) {
            return "\u5730\u70b9 \u4e00\u98df\u5802";
        }
        if (field.contains("\u56fe\u7247")) {
            return "https://example.com/a.jpg";
        }
        if (field.contains("ID") || field.contains("Id") || field.contains("id")) {
            return "\u5546\u54c1 ID 123";
        }
        if (field.contains("\u65e5\u671f")) {
            return "\u65e5\u671f 5\u67081\u65e5";
        }
        if (field.contains("\u65f6\u95f4")) {
            return "\u4e0b\u53482\u70b9\u52304\u70b9";
        }
        if (field.contains("\u63cf\u8ff0")) {
            return "\u63cf\u8ff0 \u9002\u5408\u65b0\u624b\u53c2\u4e0e";
        }
        if (field.contains("\u540d\u79f0") || field.contains("\u6d3b\u52a8")) {
            return "\u7fbd\u6bdb\u7403\u6d3b\u52a8";
        }
        return null;
    }

    private void appendIfPresent(List<String> parts, String label, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return;
        }
        parts.add(label + "=" + text);
    }

    private String summarizeImages(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.contains("http") ? "\u5df2\u63d0\u4f9b" : text;
    }

    private String extractInvocationMessage(BackendApiProxyService.InvocationResult invocationResult) {
        if (invocationResult == null || invocationResult.getData() == null) {
            return null;
        }
        Object data = invocationResult.getData();
        if (data instanceof Map) {
            Object message = ((Map<?, ?>) data).get("message");
            return message == null ? null : String.valueOf(message);
        }
        return String.valueOf(data);
    }

    private String appendBackendMessage(String backendMessage) {
        return StringUtils.hasText(backendMessage) ? "\u540e\u7aef\u8fd4\u56de\uff1a" + backendMessage : "";
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

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text) || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static class MergedRealtimeItem {
        private final RealtimeResultItem item;
        private final LinkedHashSet<String> sources = new LinkedHashSet<String>();
        private final List<String> conflictSummaries = new ArrayList<String>();

        private MergedRealtimeItem(RealtimeResultItem baseItem) {
            this.item = baseItem;
            if (StringUtils.hasText(baseItem.getSource())) {
                this.sources.add(baseItem.getSource());
            }
        }

        private void merge(RealtimeResultItem incoming) {
            if (StringUtils.hasText(incoming.getSource())) {
                this.sources.add(incoming.getSource());
            }

            mergeField("availabilityStatus", item.getAvailabilityStatus(), incoming.getAvailabilityStatus());
            mergeField("sellStatus", item.getSellStatus(), incoming.getSellStatus());
            mergeField("businessStatus", item.getBusinessStatus(), incoming.getBusinessStatus());
            mergeField("bookable", item.getBookable(), incoming.getBookable());
            mergeField("openNow", item.getOpenNow(), incoming.getOpenNow());
            mergeField("price", normalizePrice(item.getPrice()), normalizePrice(incoming.getPrice()));

            if (!StringUtils.hasText(item.getAvailabilityStatus())) {
                item.setAvailabilityStatus(incoming.getAvailabilityStatus());
            }
            if (!StringUtils.hasText(item.getInventoryStatus())) {
                item.setInventoryStatus(incoming.getInventoryStatus());
            }
            if (item.getInventoryCount() == null) {
                item.setInventoryCount(incoming.getInventoryCount());
            }
            if (!StringUtils.hasText(item.getSellStatus())) {
                item.setSellStatus(incoming.getSellStatus());
            }
            if (item.getBookable() == null) {
                item.setBookable(incoming.getBookable());
            }
            if (item.getPrice() == null) {
                item.setPrice(incoming.getPrice());
            }
            if (!StringUtils.hasText(item.getCurrency())) {
                item.setCurrency(incoming.getCurrency());
            }
            if (!StringUtils.hasText(item.getBusinessStatus())) {
                item.setBusinessStatus(incoming.getBusinessStatus());
            }
            if (item.getOpenNow() == null) {
                item.setOpenNow(incoming.getOpenNow());
            }
            if (item.getQueryTs() == null) {
                item.setQueryTs(incoming.getQueryTs());
            }
            item.setSuccess(item.isSuccess() || incoming.isSuccess());
            item.setDegraded(item.isDegraded() || incoming.isDegraded());
            if (!item.isSuccess() && incoming.isSuccess()) {
                item.setAvailabilityStatus(incoming.getAvailabilityStatus());
                item.setInventoryStatus(incoming.getInventoryStatus());
                item.setInventoryCount(incoming.getInventoryCount());
                item.setSellStatus(incoming.getSellStatus());
                item.setBookable(incoming.getBookable());
                item.setPrice(incoming.getPrice());
                item.setCurrency(incoming.getCurrency());
                item.setBusinessStatus(incoming.getBusinessStatus());
                item.setOpenNow(incoming.getOpenNow());
                item.setSource(incoming.getSource());
                item.setErrorCode(incoming.getErrorCode());
                item.setErrorMessage(incoming.getErrorMessage());
            }
        }

        private void mergeField(String fieldName, Object current, Object incoming) {
            if (current != null && incoming != null && !current.equals(incoming)) {
                String summary = "#" + item.getEntityId() + " " + fieldName + ": " + current + " vs " + incoming;
                if (!conflictSummaries.contains(summary)) {
                    conflictSummaries.add(summary);
                }
            }
        }

        private RealtimeResultItem getItem() {
            return item;
        }

        private boolean hasConflict() {
            return !conflictSummaries.isEmpty();
        }

        private List<String> getConflictSummaries() {
            return conflictSummaries;
        }

        private List<String> getSources() {
            return new ArrayList<String>(sources);
        }

        private String getSourceLabel() {
            return sources.isEmpty() ? "realtime_service" : String.join(", ", sources);
        }

        private int getSourceCount() {
            return sources.size();
        }

        private static String normalizePrice(BigDecimal value) {
            return value == null ? null : value.stripTrailingZeros().toPlainString();
        }
    }
}
