package com.example.demo.demos.realtime.service;

import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.common.error.BizException;
import com.example.demo.demos.common.error.ErrorCode;
import com.example.demo.demos.realtime.config.RealtimeQueryProperties;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRealtimeGatewayClientTest {

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
    void queryShouldDeserializeRealtimeGatewayResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"items\":[{\"entityId\":\"123\",\"inventoryStatus\":\"available\",\"inventoryCount\":6,"
                        + "\"sellStatus\":\"on_sale\",\"availabilityStatus\":\"available\",\"bookable\":true,"
                        + "\"price\":19.9,\"currency\":\"CNY\",\"businessStatus\":\"open\",\"openNow\":true,"
                        + "\"success\":true,\"degraded\":false,\"source\":\"gateway\"}],"
                        + "\"partialFailedIds\":[],\"realtimeStatus\":\"SUCCESS\",\"queryMeta\":{\"source\":\"gateway\"}}"));

        HttpRealtimeGatewayClient client = new HttpRealtimeGatewayClient(properties(true), new RestTemplateBuilder());
        RealtimeQueryResponse response = client.query(request());

        assertEquals(RealtimeStatus.SUCCESS, response.getRealtimeStatus());
        assertEquals(1, response.getItems().size());
        assertEquals("123", response.getItems().get(0).getEntityId());
        assertEquals(Boolean.TRUE, response.getQueryMeta().get("gatewayEnabled"));
        assertTrue(server.takeRequest().getPath().contains("/query"));
    }

    @Test
    void queryShouldMapGatewayFailureToBizException() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));

        HttpRealtimeGatewayClient client = new HttpRealtimeGatewayClient(properties(true), new RestTemplateBuilder());

        BizException ex = assertThrows(BizException.class, () -> client.query(request()));
        assertEquals(ErrorCode.REALTIME_UNAVAILABLE, ex.getCode());
    }

    @Test
    void queryShouldReturnDegradedResponseWhenGatewayDisabled() {
        HttpRealtimeGatewayClient client = new HttpRealtimeGatewayClient(properties(false), new RestTemplateBuilder());

        RealtimeQueryResponse response = client.query(request());

        assertEquals(RealtimeStatus.DEGRADED, response.getRealtimeStatus());
        assertEquals(Boolean.FALSE, response.getQueryMeta().get("gatewayEnabled"));
    }

    private RealtimeQueryProperties properties(boolean gatewayEnabled) {
        RealtimeQueryProperties properties = new RealtimeQueryProperties();
        properties.setGatewayEnabled(gatewayEnabled);
        properties.setGatewayBaseUrl(server.url("/").toString());
        properties.setGatewayQueryPath("/query");
        properties.setGatewayRetryCount(0);
        properties.setGatewayConnectTimeoutMs(300);
        properties.setGatewayReadTimeoutMs(300);
        return properties;
    }

    private RealtimeQueryRequest request() {
        RealtimeQueryRequest request = new RealtimeQueryRequest();
        request.setEntityType("product");
        request.setEntityIds(Collections.singletonList(123L));
        request.setQueryType("availability");
        request.setTraceId("trace-1");
        return request;
    }
}
