package com.example.demo.demos.Agent.Service;

import com.example.demo.demos.Agent.Config.DeepSeekProperties;
import com.example.demo.demos.Agent.Config.QueryParserPythonProperties;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Python.PythonQueryParserClient;
import com.example.demo.demos.Agent.Python.PythonSidecarException;
import com.example.demo.demos.Agent.Service.llm.DeepSeekClient;
import com.example.demo.demos.common.enums.TaskType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryParserServiceTest {

    @Mock
    private PythonQueryParserClient pythonQueryParserClient;
    @Mock
    private RuleFallbackParser ruleFallbackParser;

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
    void parseShouldPreferPythonSidecarWhenAvailable() {
        ParsedIntent pythonIntent = new ParsedIntent();
        pythonIntent.setTaskType(TaskType.FAQ_QUERY);
        pythonIntent.setIntentConfidence(0.88);
        when(pythonQueryParserClient.parse(eq("refund policy"), anyList(), isNull(), isNull())).thenReturn(pythonIntent);

        ParsedIntent result = newService(enabledPythonProperties(), deepSeekProperties(false)).parse(
                "refund policy",
                Collections.<AgentChatMessage>emptyList()
        );

        assertSame(pythonIntent, result);
        verify(ruleFallbackParser, never()).parse(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void parseShouldFallbackToLocalLlmWhenPythonSidecarFails() {
        when(pythonQueryParserClient.parse(eq("refund policy"), anyList(), isNull(), isNull()))
                .thenThrow(new PythonSidecarException("python down"));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"content\":\"{\\\"task_type\\\":\\\"faq_query\\\",\\\"intent_confidence\\\":0.77,\\\"need_explanation\\\":true,\\\"candidate_slots\\\":{}}\"}}]}"));

        ParsedIntent result = newService(enabledPythonProperties(), deepSeekProperties(true)).parse(
                "refund policy",
                Collections.<AgentChatMessage>emptyList()
        );

        assertEquals(TaskType.FAQ_QUERY, result.getTaskType());
        assertTrue(result.isNeedExplanation());
        verify(ruleFallbackParser, never()).parse(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void parseShouldFallbackToRuleParserWhenPythonSidecarFailsAndLocalLlmDisabled() {
        when(pythonQueryParserClient.parse(eq("buy apples now"), anyList(), isNull(), isNull()))
                .thenThrow(new PythonSidecarException("python down"));
        ParsedIntent fallbackIntent = new ParsedIntent();
        fallbackIntent.setTaskType(TaskType.MIXED_SEARCH_REALTIME);
        when(ruleFallbackParser.parse("buy apples now")).thenReturn(fallbackIntent);

        ParsedIntent result = newService(enabledPythonProperties(), deepSeekProperties(false)).parse(
                "buy apples now",
                Collections.<AgentChatMessage>emptyList()
        );

        assertSame(fallbackIntent, result);
        verify(ruleFallbackParser).parse("buy apples now");
    }

    private QueryParserService newService(QueryParserPythonProperties pythonProperties,
                                          DeepSeekProperties deepSeekProperties) {
        DeepSeekClient deepSeekClient = new DeepSeekClient(new RestTemplateBuilder(), deepSeekProperties);
        return new QueryParserService(
                deepSeekClient,
                pythonProperties,
                pythonQueryParserClient,
                ruleFallbackParser
        );
    }

    private QueryParserPythonProperties enabledPythonProperties() {
        QueryParserPythonProperties properties = new QueryParserPythonProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(server.url("/").toString());
        properties.setParsePath("/parse_intent");
        properties.setConnectTimeoutMs(200L);
        properties.setReadTimeoutMs(200L);
        return properties;
    }

    private DeepSeekProperties deepSeekProperties(boolean enabled) {
        DeepSeekProperties properties = new DeepSeekProperties();
        properties.setUrl(server.url("/chat/completions").toString());
        properties.setConnectTimeoutMs(200L);
        properties.setReadTimeoutMs(200L);
        properties.setKey(enabled ? "test-key" : null);
        return properties;
    }
}
