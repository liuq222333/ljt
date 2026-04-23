package com.example.demo.demos.Agent.Python;

import com.example.demo.demos.Agent.Config.AgentRouterPythonProperties;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Runtime.SessionState;
import com.example.demo.demos.common.enums.PlanType;
import com.example.demo.demos.common.enums.TaskType;
import com.example.demo.demos.common.enums.ToolName;
import com.example.demo.demos.common.schema.ToolPlan;
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

class PythonToolRouterClientTest {

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
    void routeShouldDeserializeToolPlan() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"plan_id\":\"plan-1\",\"plan_type\":\"search_then_realtime\",\"execution_mode\":\"serial\","
                        + "\"routing_reason\":\"python_router\",\"steps\":[{\"step_id\":\"search\",\"tool_name\":\"structured_search\"},"
                        + "{\"step_id\":\"realtime\",\"tool_name\":\"realtime_query\",\"depends_on\":[\"search\"]}]}"));

        PythonToolRouterClient client = new PythonToolRouterClient(properties(), new PythonSidecarHttpClient(new RestTemplateBuilder()), new PythonSidecarMapper());
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.MIXED_SEARCH_REALTIME);
        parsedIntent.setNeedRealtime(true);

        ToolPlan result = client.route(parsedIntent, "apple", "product", true, Collections.<String>emptyList(), Collections.<String>emptyList(), new SessionState.SessionContext());

        assertEquals(PlanType.SEARCH_THEN_REALTIME, result.getPlanType());
        assertEquals(2, result.getSteps().size());
        assertEquals(ToolName.REALTIME_QUERY, result.getSteps().get(1).getToolName());

        RecordedRequest request = server.takeRequest();
        assertEquals("/route_tools", request.getPath());
        String requestBody = request.getBody().readUtf8();
        assertTrue(requestBody.contains("\"normalized_params\""));
        assertTrue(requestBody.contains("\"keywords\":\"apple\""));
    }

    @Test
    void routeShouldRejectMissingPlanType() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"execution_mode\":\"serial\"}"));

        PythonToolRouterClient client = new PythonToolRouterClient(properties(), new PythonSidecarHttpClient(new RestTemplateBuilder()), new PythonSidecarMapper());
        ParsedIntent parsedIntent = new ParsedIntent();
        parsedIntent.setTaskType(TaskType.PRODUCT_SEARCH);

        assertThrows(PythonSidecarException.class,
                () -> client.route(parsedIntent, "apple", "product", true, Collections.<String>emptyList(), Collections.<String>emptyList(), new SessionState.SessionContext()));
    }

    private AgentRouterPythonProperties properties() {
        AgentRouterPythonProperties properties = new AgentRouterPythonProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(server.url("/").toString());
        properties.setRoutePath("/route_tools");
        properties.setConnectTimeoutMs(300L);
        properties.setReadTimeoutMs(300L);
        return properties;
    }
}
