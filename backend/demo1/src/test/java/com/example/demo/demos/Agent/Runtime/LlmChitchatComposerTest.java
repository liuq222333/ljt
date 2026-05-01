package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Config.AgentLlmProperties;
import com.example.demo.demos.Agent.Config.DeepSeekProperties;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Service.llm.DeepSeekClient;
import com.example.demo.demos.common.enums.AnswerType;
import com.example.demo.demos.common.enums.TaskType;
import com.example.demo.demos.common.schema.FinalAnswer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmChitchatComposerTest {

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
    void composeShouldCallDeepSeekAndReturnLlmGeneratedText() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"你好呀，想看附近什么商品都可以告诉我。\"}}]}"));

        LlmChitchatComposer composer = newComposer(true, true);
        AgentChatMessage userMsg = userMessage("你好");
        ParsedIntent intent = chitchatIntent();

        FinalAnswer answer = composer.compose(userMsg, intent);

        assertNotNull(answer);
        assertEquals(AnswerType.FAQ_ANSWER, answer.getAnswerType());
        assertTrue(answer.getAnswerText().contains("你好呀"));
        assertTrue(answer.getComposerMeta().getUsedSources().contains("deepseek_llm"));
        assertEquals("llm_chitchat", answer.getComposerMeta().getMetadata().get("composeStrategy"));
    }

    @Test
    void composeShouldReturnNullWhenChitchatDisabled() {
        LlmChitchatComposer composer = newComposer(false, true);
        FinalAnswer answer = composer.compose(userMessage("你好"), chitchatIntent());
        assertNull(answer);
    }

    @Test
    void composeShouldReturnNullWhenDeepSeekKeyMissing() {
        LlmChitchatComposer composer = newComposer(true, false);
        FinalAnswer answer = composer.compose(userMessage("你好"), chitchatIntent());
        assertNull(answer);
    }

    @Test
    void composeShouldReturnNullWhenLlmThrows() {
        // 无 enqueue → MockWebServer 会让请求等待并最终超时
        LlmChitchatComposer composer = newComposer(true, true);
        // 显式给一个会立刻 500 的响应，避免阻塞测试
        server.enqueue(new MockResponse().setResponseCode(500));
        FinalAnswer answer = composer.compose(userMessage("你好"), chitchatIntent());
        assertNull(answer);
    }

    private LlmChitchatComposer newComposer(boolean chitchatEnabled, boolean keyConfigured) {
        DeepSeekProperties properties = new DeepSeekProperties();
        properties.setUrl(server.url("/chat/completions").toString());
        properties.setKey(keyConfigured ? "test-key" : null);
        properties.setConnectTimeoutMs(200L);
        properties.setReadTimeoutMs(500L);
        DeepSeekClient client = new DeepSeekClient(new RestTemplateBuilder(), properties);

        AgentLlmProperties llmProperties = new AgentLlmProperties();
        llmProperties.setChitchatEnabled(chitchatEnabled);
        llmProperties.setChitchatTemperature(0.7);
        llmProperties.setChitchatMaxTokens(120);

        return new LlmChitchatComposer(client, llmProperties);
    }

    private AgentChatMessage userMessage(String content) {
        AgentChatMessage msg = new AgentChatMessage();
        msg.setRole("user");
        msg.setContent(content);
        return msg;
    }

    private ParsedIntent chitchatIntent() {
        ParsedIntent intent = new ParsedIntent();
        intent.setTaskType(TaskType.CHITCHAT);
        return intent;
    }
}
