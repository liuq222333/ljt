package com.example.demo.demos.Agent.Python;

import com.example.demo.demos.Agent.Config.AgentComposerPythonProperties;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalResponse;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Runtime.SessionState;
import com.example.demo.demos.common.enums.AnswerType;
import com.example.demo.demos.common.enums.TaskType;
import com.example.demo.demos.common.schema.FinalAnswer;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
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

class PythonResponseComposerClientTest {

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
    void composeShouldDeserializeFinalAnswer() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"answer_type\":\"recommendation\",\"answer_text\":\"python composed answer\",\"summary\":\"summary\","
                        + "\"cards\":[{\"entity_id\":\"1\",\"entity_type\":\"product\",\"title\":\"Apple\"}],"
                        + "\"disclaimers\":[],\"citations\":[],\"next_actions\":[\"next\"],"
                        + "\"composer_meta\":{\"used_sources\":[\"search_results\"],\"degraded\":false}}"));

        PythonResponseComposerClient client = new PythonResponseComposerClient(properties(), new PythonSidecarHttpClient(new RestTemplateBuilder()), new PythonSidecarMapper());
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        snapshot.setProductId(1L);
        snapshot.setTitle("Apple");

        FinalAnswer result = client.compose(
                parsedIntent,
                "apple",
                "product",
                Collections.singletonList(snapshot),
                1L,
                new KnowledgeRetrievalResponse(),
                null,
                new SessionState.SessionContext(),
                new SessionState.ExecutionMeta(),
                false,
                null,
                null,
                null
        );

        assertEquals(AnswerType.RECOMMENDATION, result.getAnswerType());
        assertEquals("python composed answer", result.getAnswerText());
        assertEquals(1, result.getCards().size());

        RecordedRequest request = server.takeRequest();
        assertEquals("/compose_response", request.getPath());
        String requestBody = request.getBody().readUtf8();
        assertTrue(requestBody.contains("\"search_results\""));
        assertTrue(requestBody.contains("\"keywords\":\"apple\""));
    }

    @Test
    void composeShouldRejectMissingAnswerType() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"answer_text\":\"missing type\"}"));

        PythonResponseComposerClient client = new PythonResponseComposerClient(properties(), new PythonSidecarHttpClient(new RestTemplateBuilder()), new PythonSidecarMapper());
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);

        assertThrows(PythonSidecarException.class,
                () -> client.compose(parsedIntent, "apple", "product", Collections.<ProductSearchSnapshot>emptyList(), 0L,
                        new KnowledgeRetrievalResponse(), null, new SessionState.SessionContext(), new SessionState.ExecutionMeta(), false, null, null, null));
    }

    private AgentComposerPythonProperties properties() {
        AgentComposerPythonProperties properties = new AgentComposerPythonProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(server.url("/").toString());
        properties.setComposePath("/compose_response");
        properties.setConnectTimeoutMs(300L);
        properties.setReadTimeoutMs(300L);
        return properties;
    }
}
