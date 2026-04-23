package com.example.demo.demos.Agent.Python;

import com.example.demo.demos.Agent.Config.QueryParserPythonProperties;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.common.enums.TaskType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PythonQueryParserClientTest {

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
    void parseShouldDeserializePythonSidecarResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"task_type\":\"mixed_search_realtime\",\"intent_confidence\":0.93,"
                        + "\"query_text\":\"buy apple now\",\"candidate_slots\":{\"keyword\":\"apple\",\"entity_type\":\"product\"},"
                        + "\"need_realtime\":true,\"need_recommendation\":true,\"is_follow_up\":false,\"is_negation\":false,\"negated_entities\":[]}"));

        PythonQueryParserClient client = new PythonQueryParserClient(properties(), new PythonSidecarHttpClient(new RestTemplateBuilder()), new PythonSidecarMapper());
        AgentChatMessage history = new AgentChatMessage();
        history.setRole("assistant");
        history.setContent("what would you like to buy?");

        ParsedIntent result = client.parse("buy apple now", Collections.singletonList(history));

        assertEquals(TaskType.MIXED_SEARCH_REALTIME, result.getTaskType());
        assertEquals("apple", result.getCandidateSlots().getKeyword());
        assertTrue(result.isNeedRealtime());

        RecordedRequest request = server.takeRequest();
        assertEquals("/parse_intent", request.getPath());
        String requestBody = request.getBody().readUtf8();
        assertTrue(requestBody.contains("\"current_message\":\"buy apple now\""));
        assertTrue(requestBody.contains("\"recent_messages\""));
    }

    @Test
    void parseShouldRejectMissingTaskType() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"intent_confidence\":0.6,\"query_text\":\"buy apple now\"}"));

        PythonQueryParserClient client = new PythonQueryParserClient(properties(), new PythonSidecarHttpClient(new RestTemplateBuilder()), new PythonSidecarMapper());

        assertThrows(PythonSidecarException.class, () -> client.parse("buy apple now", Collections.<AgentChatMessage>emptyList()));
    }

    private QueryParserPythonProperties properties() {
        QueryParserPythonProperties properties = new QueryParserPythonProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(server.url("/").toString());
        properties.setParsePath("/parse_intent");
        properties.setConnectTimeoutMs(300L);
        properties.setReadTimeoutMs(300L);
        return properties;
    }
}
