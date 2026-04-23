package com.example.demo.demos.Agent.Python;

import com.example.demo.demos.Agent.Config.AgentActionReviewPythonProperties;
import com.example.demo.demos.Agent.Entity.ApiRoute;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Runtime.ActionConversationStore;
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

class PythonActionReviewClientTest {

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
    void reviewShouldDeserializeActionReviewPayload() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"handled\":true,\"outcome\":\"need_confirmation\",\"pending_action\":{"
                        + "\"resource\":\"product\",\"action\":\"create\",\"operation_type\":\"CREATE\","
                        + "\"display_name\":\"发布商品\",\"payload\":{\"title\":\"Apple\"}}}"));

        PythonActionReviewClient client = new PythonActionReviewClient(
                properties(true),
                new PythonSidecarHttpClient(new RestTemplateBuilder()),
                new PythonSidecarMapper()
        );
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);
        ActionConversationStore.PendingAction pendingAction = new ActionConversationStore.PendingAction();
        pendingAction.setSessionId("s-1");
        pendingAction.setResource("product");
        pendingAction.setAction("create");
        pendingAction.setOperationType("CREATE");
        ApiRoute route = new ApiRoute();
        route.setId(9L);
        route.setResource("product");
        route.setAction("create");
        route.setOperationType("CREATE");
        route.setDescription("发布一个商品");

        PythonSidecarModels.ActionReviewResponsePayload result = client.review(
                "帮我发布一个商品",
                parsedIntent,
                pendingAction,
                Collections.singletonList(route)
        );

        assertTrue(result.getHandled());
        assertEquals("need_confirmation", result.getOutcome());
        assertEquals("create", result.getPendingAction().getAction());
        assertEquals("Apple", result.getPendingAction().getPayload().get("title"));

        RecordedRequest request = server.takeRequest();
        assertEquals("/review_action", request.getPath());
        String requestBody = request.getBody().readUtf8();
        assertTrue(requestBody.contains("\"current_message\":\"帮我发布一个商品\""));
        assertTrue(requestBody.contains("\"available_routes\""));
    }

    @Test
    void reviewShouldRejectWhenSidecarDisabled() {
        PythonActionReviewClient client = new PythonActionReviewClient(
                properties(false),
                new PythonSidecarHttpClient(new RestTemplateBuilder()),
                new PythonSidecarMapper()
        );

        assertThrows(PythonSidecarException.class,
                () -> client.review("帮我发布一个商品", new ParsedIntent(), null, Collections.<ApiRoute>emptyList()));
    }

    private AgentActionReviewPythonProperties properties(boolean enabled) {
        AgentActionReviewPythonProperties properties = new AgentActionReviewPythonProperties();
        properties.setEnabled(enabled);
        properties.setBaseUrl(server.url("/").toString());
        properties.setReviewPath("/review_action");
        properties.setConnectTimeoutMs(300L);
        properties.setReadTimeoutMs(300L);
        return properties;
    }
}
