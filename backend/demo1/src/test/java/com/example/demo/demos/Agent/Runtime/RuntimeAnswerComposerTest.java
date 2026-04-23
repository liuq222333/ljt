package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Pojo.CandidateSlots;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.common.enums.AnswerType;
import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.common.enums.TaskType;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.model.RealtimeResultItem;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeAnswerComposerTest {

    private final RuntimeAnswerComposer runtimeAnswerComposer = new RuntimeAnswerComposer();

    @Test
    void buildRealtimeAnswerShouldMergeDuplicateEntitiesAndMarkConflicts() {
        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.SUCCESS);
        response.getItems().add(item("123", "gateway-a", "bookable", new BigDecimal("19.90"), true, true));
        response.getItems().add(item("123", "gateway-b", "sold_out", new BigDecimal("21.00"), true, false));

        RealtimeQueryRequest request = new RealtimeQueryRequest();
        request.setEntityType("product");
        com.example.demo.demos.common.schema.FinalAnswer finalAnswer =
                runtimeAnswerComposer.buildRealtimeAnswer(response, request);

        assertEquals(AnswerType.REALTIME_CONFIRMATION, finalAnswer.getAnswerType());
        assertEquals(1, finalAnswer.getCards().size());
        assertTrue(finalAnswer.getComposerMeta().isConflictDetected());
        assertTrue(finalAnswer.getDisclaimers().stream().anyMatch(text -> text.contains("实时字段冲突")));
        assertNotNull(finalAnswer.getComposerMeta().getMetadata().get("conflicts"));
        assertTrue(finalAnswer.getCards().get(0).getHighlights().stream().anyMatch(text -> text.contains("合并来源数")));
        assertTrue(finalAnswer.getCards().get(0).getSubtitle().contains("存在冲突"));
        assertTrue(finalAnswer.getCitations().get(0).getTitle().contains("合并实时结果"));
    }

    @Test
    void finalizeAnswerShouldCopyConflictFlagToDebugTraceMetadata() {
        com.example.demo.demos.common.schema.FinalAnswer finalAnswer =
                runtimeAnswerComposer.buildRealtimeUnavailableAnswer(new RealtimeQueryRequest(), "realtime_timeout", "timeout");
        finalAnswer.getComposerMeta().setConflictDetected(true);

        SessionState state = new SessionState();
        SessionState.ExecutionMeta executionMeta = state.getExecutionMeta();
        executionMeta.getCompletedNodes().add("realtime_query");
        executionMeta.setDurationMs(12L);
        com.example.demo.demos.common.schema.FinalAnswer.DebugTrace debugTrace =
                new com.example.demo.demos.common.schema.FinalAnswer.DebugTrace();

        runtimeAnswerComposer.finalizeAnswer(finalAnswer, state, executionMeta, null, debugTrace);

        assertTrue(Boolean.TRUE.equals(finalAnswer.getDebugTrace().getMetadata().get("conflictDetected")));
        assertFalse(finalAnswer.getComposerMeta().getUsedSources().isEmpty());
    }

    @Test
    void buildFallbackAnswerShouldBecomeIntentAwareForFaq() {
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.FAQ_QUERY);
        CandidateSlots slots = new CandidateSlots();
        slots.setKeyword("refund rules");
        parsedIntent.setCandidateSlots(slots);

        AgentChatRequest request = new AgentChatRequest();
        AgentChatMessage message = new AgentChatMessage();
        message.setRole("user");
        message.setContent("what are the refund rules");
        request.setMessages(Arrays.asList(message));

        com.example.demo.demos.common.schema.FinalAnswer finalAnswer =
                runtimeAnswerComposer.buildFallbackAnswer(request, message, parsedIntent);

        assertEquals(AnswerType.FAQ_ANSWER, finalAnswer.getAnswerType());
        assertTrue(finalAnswer.getSummary().contains("知识问答"));
        assertTrue(finalAnswer.getAnswerText().contains("refund rules"));
        assertTrue(finalAnswer.getDisclaimers().stream().anyMatch(text -> text.contains("还没有真正落到知识检索结果")));
        assertTrue(finalAnswer.getNextActions().stream().anyMatch(text -> text.contains("知识检索")));
        assertEquals("agent_runtime", finalAnswer.getComposerMeta().getUsedSources().get(0));
    }

    @Test
    void finalizeAnswerShouldNormalizeDisclaimersActionsAndCitations() {
        com.example.demo.demos.common.schema.FinalAnswer finalAnswer =
                runtimeAnswerComposer.buildRealtimeUnavailableAnswer(new RealtimeQueryRequest(), "realtime_timeout", "timeout");
        finalAnswer.getDisclaimers().add("当前状态未能实时确认，如需依赖该结果，请稍后重试。");
        finalAnswer.getDisclaimers().add("  ");
        finalAnswer.getNextActions().add("稍后重试这次实时查询。");
        finalAnswer.getNextActions().add("稍后重试这次实时查询。");
        finalAnswer.getCitations().add(finalAnswer.getCitations().get(0));

        SessionState state = new SessionState();
        SessionState.ExecutionMeta executionMeta = state.getExecutionMeta();
        executionMeta.setDurationMs(8L);
        com.example.demo.demos.common.schema.FinalAnswer.DebugTrace debugTrace =
                new com.example.demo.demos.common.schema.FinalAnswer.DebugTrace();

        runtimeAnswerComposer.finalizeAnswer(finalAnswer, state, executionMeta, null, debugTrace);

        assertEquals(1, finalAnswer.getCitations().size());
        assertEquals(1, finalAnswer.getDisclaimers().size());
        assertTrue(finalAnswer.getNextActions().size() >= 2);
    }

    @Test
    void buildChitchatAnswerShouldReturnGreetingInChinese() {
        AgentChatMessage message = new AgentChatMessage();
        message.setRole("user");
        message.setContent("你好");

        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.CHITCHAT);

        com.example.demo.demos.common.schema.FinalAnswer finalAnswer =
                runtimeAnswerComposer.buildChitchatAnswer(message, parsedIntent);

        assertEquals(AnswerType.FAQ_ANSWER, finalAnswer.getAnswerType());
        assertTrue(finalAnswer.getSummary().contains("闲聊"));
        assertTrue(finalAnswer.getAnswerText().contains("你好"));
        assertTrue(finalAnswer.getNextActions().stream().anyMatch(text -> text.contains("推荐几个社区市场里的蔬菜")));
    }

    @Test
    void buildSearchRealtimeAnswerShouldEnrichSearchCardsWithRealtimeStatus() {
        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        snapshot.setProductId(11L);
        snapshot.setTitle("新鲜苹果");
        snapshot.setCurrency("CNY");

        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.SUCCESS);
        RealtimeResultItem item = new RealtimeResultItem();
        item.setEntityId("11");
        item.setAvailabilityStatus("在售");
        item.setInventoryStatus("available");
        item.setInventoryCount(5);
        item.setBookable(true);
        item.setSource("snapshot_fallback");
        response.getItems().add(item);

        com.example.demo.demos.common.schema.FinalAnswer finalAnswer =
                runtimeAnswerComposer.buildSearchRealtimeAnswer(
                        Arrays.asList(snapshot),
                        1L,
                        null,
                        response,
                        null,
                        new ParsedIntent()
                );

        assertEquals(AnswerType.RECOMMENDATION, finalAnswer.getAnswerType());
        assertTrue(finalAnswer.getSummary().contains("已补充实时状态"));
        assertEquals("在售", finalAnswer.getCards().get(0).getRealtimeStatusText());
        assertTrue(finalAnswer.getCards().get(0).getHighlights().stream().anyMatch(text -> text.contains("实时状态")));
        assertTrue(finalAnswer.getComposerMeta().getUsedSources().contains("realtime_service"));
    }

    private RealtimeResultItem item(String entityId,
                                    String source,
                                    String availability,
                                    BigDecimal price,
                                    boolean success,
                                    boolean openNow) {
        RealtimeResultItem item = new RealtimeResultItem();
        item.setEntityId(entityId);
        item.setSource(source);
        item.setAvailabilityStatus(availability);
        item.setInventoryStatus(success ? "available" : "unknown");
        item.setInventoryCount(success ? 3 : 0);
        item.setSellStatus(success ? "on_sale" : "off_sale");
        item.setPrice(price);
        item.setCurrency("CNY");
        item.setBookable(success);
        item.setOpenNow(openNow);
        item.setSuccess(success);
        item.setDegraded(false);
        return item;
    }
}
