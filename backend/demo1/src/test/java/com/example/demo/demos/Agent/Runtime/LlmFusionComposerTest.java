package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Config.AgentLlmProperties;
import com.example.demo.demos.Agent.Config.DeepSeekProperties;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalResponse;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Service.llm.DeepSeekClient;
import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.common.enums.TaskType;
import com.example.demo.demos.common.schema.FinalAnswer;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.model.RealtimeResultItem;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import com.example.demo.demos.search.model.ProductSearchQuery;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 覆盖方案 Verification 中三个核心场景：
 *  1. 纯搜索（仅结构层）→ prompt 不含【实时数据】具体值，必须显式标注"不得提及具体库存数量"
 *  2. 搜索 + 实时 → prompt 同时包含两层
 *  3. 知识层条件接入：needExplanation=false 时不出现【知识规则】段
 */
class LlmFusionComposerTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void composeShouldFeedStructuralOnlyAndForbidInventoryNumber() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"我们有红富士苹果在卖，欢迎选购。\"}}]}"));

        LlmFusionComposer composer = newComposer(true);
        LlmFusionComposer.LlmFusionContext ctx = baseContext("我想买苹果");
        ctx.searchResults = Collections.singletonList(snapshot(101L, "红富士苹果", "水果生鲜", new BigDecimal("12.50"), "果园直营"));
        ctx.searchTotal = 1;
        // realtime + knowledge 都是 null

        FinalAnswer answer = composer.compose(ctx);

        assertNotNull(answer);
        assertTrue(answer.getAnswerText().contains("红富士苹果"));
        assertTrue(answer.getComposerMeta().getUsedSources().contains("deepseek_llm"));

        RecordedRequest recorded = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(recorded);
        String body = recorded.getBody().readUtf8();
        assertTrue(body.contains("商品快照"), "structure block must be present");
        assertTrue(body.contains("回复中不得提及具体库存数量"), "must instruct LLM no inventory number when realtime missing");
        assertFalse(body.contains("【知识规则】"), "must NOT include knowledge block when needExplanation=false");
    }

    @Test
    void composeShouldFeedStructuralAndRealtimeWhenBothPresent() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"红富士还有 3 件，可下单。\"}}]}"));

        LlmFusionComposer composer = newComposer(true);
        LlmFusionComposer.LlmFusionContext ctx = baseContext("苹果还有货吗");
        ctx.searchResults = Collections.singletonList(snapshot(101L, "红富士苹果", "水果", new BigDecimal("12.50"), "果园"));
        ctx.searchTotal = 1;
        ctx.realtimeResponse = realtimeWith("101", 3, "in_stock");

        FinalAnswer answer = composer.compose(ctx);

        assertNotNull(answer);
        assertTrue(answer.getAnswerText().contains("3 件"));
        assertTrue(answer.getComposerMeta().getUsedSources().contains("deepseek_llm"));
        assertEquals("结构+实时", answer.getComposerMeta().getMetadata().get("layersUsed"));

        RecordedRequest recorded = server.takeRequest(2, TimeUnit.SECONDS);
        String body = recorded.getBody().readUtf8();
        assertTrue(body.contains("商品快照"));
        assertTrue(body.contains("库存:3"));
        assertFalse(body.contains("【知识规则】"));
    }

    @Test
    void composeShouldIncludeKnowledgeBlockOnlyWhenNeedExplanation() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"红富士苹果可以保鲜 30 天。\"}}]}"));

        LlmFusionComposer composer = newComposer(true);
        LlmFusionComposer.LlmFusionContext ctx = baseContext("红富士能放多久");
        ctx.searchResults = Collections.singletonList(snapshot(101L, "红富士苹果", "水果", new BigDecimal("12.50"), "果园"));
        ctx.searchTotal = 1;
        ctx.knowledgeResponse = knowledgeWith("红富士保鲜规则", "新鲜采摘后 0~4 度可保存约 30 天。");
        ctx.parsedIntent.setNeedExplanation(true);

        FinalAnswer answer = composer.compose(ctx);

        assertNotNull(answer);
        RecordedRequest recorded = server.takeRequest(2, TimeUnit.SECONDS);
        String body = recorded.getBody().readUtf8();
        assertTrue(body.contains("【知识规则】"), "must include knowledge block heading when needExplanation=true");
        assertTrue(body.contains("红富士保鲜规则"));
    }

    @Test
    void composeShouldReturnNullWhenFusionDisabled() {
        LlmFusionComposer composer = newComposer(false);
        LlmFusionComposer.LlmFusionContext ctx = baseContext("我想买苹果");
        ctx.searchResults = Collections.singletonList(snapshot(1L, "苹果", "水果", BigDecimal.TEN, "店铺"));
        ctx.searchTotal = 1;
        FinalAnswer answer = composer.compose(ctx);
        assertNull(answer);
    }

    @Test
    void composeShouldReturnNullWhenNoSearchAndNoKnowledge() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"不应到达这里。\"}}]}"));
        LlmFusionComposer composer = newComposer(true);
        LlmFusionComposer.LlmFusionContext ctx = baseContext("我想买苹果");
        ctx.searchResults = Collections.emptyList();
        ctx.searchTotal = 0;
        // knowledgeResponse 也为 null
        FinalAnswer answer = composer.compose(ctx);
        assertNull(answer);
    }

    /* ---------- helpers ---------- */

    private LlmFusionComposer newComposer(boolean fusionEnabled) {
        DeepSeekProperties properties = new DeepSeekProperties();
        properties.setUrl(server.url("/chat/completions").toString());
        properties.setKey("test-key");
        properties.setConnectTimeoutMs(200L);
        properties.setReadTimeoutMs(500L);
        DeepSeekClient client = new DeepSeekClient(new RestTemplateBuilder(), properties);

        AgentLlmProperties llmProperties = new AgentLlmProperties();
        llmProperties.setFusionEnabled(fusionEnabled);
        llmProperties.setFusionTemperature(0.3);
        llmProperties.setFusionMaxTokens(200);
        llmProperties.setFusionReadTimeoutMs(500L);

        return new LlmFusionComposer(client, llmProperties, new RuntimeAnswerComposer());
    }

    private LlmFusionComposer.LlmFusionContext baseContext(String userText) {
        LlmFusionComposer.LlmFusionContext ctx = new LlmFusionComposer.LlmFusionContext();
        AgentChatMessage msg = new AgentChatMessage();
        msg.setRole("user");
        msg.setContent(userText);
        ctx.latestMessage = msg;
        ParsedIntent intent = new ParsedIntent();
        intent.setTaskType(TaskType.PRODUCT_SEARCH);
        intent.setNeedExplanation(false);
        ctx.parsedIntent = intent;
        ctx.productQuery = new ProductSearchQuery();
        ctx.productQuery.setKeyword("苹果");
        ctx.searchResults = new ArrayList<>();
        return ctx;
    }

    private ProductSearchSnapshot snapshot(long productId, String title, String category, BigDecimal price, String store) {
        ProductSearchSnapshot s = new ProductSearchSnapshot();
        s.setProductId(productId);
        s.setTitle(title);
        s.setCategoryName(category);
        s.setDisplayPrice(price);
        s.setBasePrice(price);
        s.setStoreName(store);
        s.setCurrency("CNY");
        s.setSearchableStatus("searchable");
        return s;
    }

    private RealtimeQueryResponse realtimeWith(String entityId, int inventory, String availability) {
        RealtimeQueryResponse resp = new RealtimeQueryResponse();
        RealtimeResultItem item = new RealtimeResultItem();
        item.setEntityId(entityId);
        item.setInventoryCount(inventory);
        item.setAvailabilityStatus(availability);
        item.setBookable(Boolean.TRUE);
        resp.setItems(Collections.singletonList(item));
        resp.setRealtimeStatus(RealtimeStatus.SUCCESS);
        return resp;
    }

    private KnowledgeRetrievalResponse knowledgeWith(String title, String summary) {
        KnowledgeRetrievalResponse resp = new KnowledgeRetrievalResponse();
        com.example.demo.demos.Agent.Entity.KnowledgeBase kb = new com.example.demo.demos.Agent.Entity.KnowledgeBase();
        kb.setTitle(title);
        kb.setSummary(summary);
        kb.setCategory("rule");
        resp.setItems(Collections.singletonList(kb));
        resp.setHitCount(1);
        return resp;
    }
}
