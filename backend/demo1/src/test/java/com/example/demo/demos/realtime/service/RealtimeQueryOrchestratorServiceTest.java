package com.example.demo.demos.realtime.service;

import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.common.error.BizException;
import com.example.demo.demos.common.error.ErrorCode;
import com.example.demo.demos.common.ratelimit.UserRateLimiter;
import com.example.demo.demos.realtime.config.RealtimeQueryProperties;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.model.RealtimeResultItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealtimeQueryOrchestratorServiceTest {

    @Mock
    private RealtimeGatewayClient realtimeGatewayClient;
    @Mock
    private ProductRealtimeFallbackService productRealtimeFallbackService;
    @Mock
    private UserRateLimiter userRateLimiter;

    private RealtimeQueryProperties properties;
    private RealtimeQueryOrchestratorService service;

    @BeforeEach
    void setUp() {
        properties = new RealtimeQueryProperties();
        properties.setEnabled(true);
        properties.setGatewayEnabled(false);
        properties.setCacheTtlSeconds(30);
        properties.setMaxCandidates(5);
        properties.setCircuitFailureThreshold(2);
        properties.setCircuitOpenSeconds(60);
        service = new RealtimeQueryOrchestratorService(
                properties,
                realtimeGatewayClient,
                productRealtimeFallbackService,
                userRateLimiter
        );
    }

    @Test
    void queryShouldUseFallbackWithoutCachingWhenGatewayDisabled() {
        RealtimeQueryRequest request = request(1L);
        RealtimeQueryResponse fallback = fallbackResponse(1L, RealtimeStatus.DEGRADED);
        when(productRealtimeFallbackService.query(any(RealtimeQueryRequest.class), any(java.util.List.class), any(String.class)))
                .thenReturn(fallback);

        RealtimeQueryResponse first = service.query(request);
        RealtimeQueryResponse second = service.query(request);

        assertEquals(RealtimeStatus.DEGRADED, first.getRealtimeStatus());
        assertEquals(Boolean.FALSE, first.getQueryMeta().get("cacheHit"));
        assertEquals(Boolean.FALSE, second.getQueryMeta().get("cacheHit"));
        verify(productRealtimeFallbackService, times(2))
                .query(any(RealtimeQueryRequest.class), any(java.util.List.class), any(String.class));
    }

    @Test
    void queryShouldCacheSuccessfulGatewayResponse() {
        properties.setGatewayEnabled(true);
        RealtimeQueryRequest request = request(1L);

        RealtimeQueryResponse gateway = new RealtimeQueryResponse();
        gateway.setRealtimeStatus(RealtimeStatus.SUCCESS);
        gateway.getItems().add(item(1L, false));
        when(realtimeGatewayClient.query(any(RealtimeQueryRequest.class))).thenReturn(gateway);

        RealtimeQueryResponse first = service.query(request);
        RealtimeQueryResponse second = service.query(request);

        assertEquals(Boolean.FALSE, first.getQueryMeta().get("cacheHit"));
        assertEquals(Boolean.TRUE, second.getQueryMeta().get("cacheHit"));
        verify(realtimeGatewayClient, times(1)).query(any(RealtimeQueryRequest.class));
    }

    @Test
    void queryShouldMergeGatewayAndFallbackWhenGatewayPartial() {
        properties.setGatewayEnabled(true);
        RealtimeQueryRequest request = request(1L, 2L);

        RealtimeQueryResponse gateway = new RealtimeQueryResponse();
        gateway.setRealtimeStatus(RealtimeStatus.PARTIAL_SUCCESS);
        gateway.getItems().add(item(1L, false));
        when(realtimeGatewayClient.query(any(RealtimeQueryRequest.class))).thenReturn(gateway);

        RealtimeQueryResponse fallback = fallbackResponse(2L, RealtimeStatus.DEGRADED);
        when(productRealtimeFallbackService.query(any(RealtimeQueryRequest.class), any(java.util.List.class), any(String.class)))
                .thenReturn(fallback);

        RealtimeQueryResponse response = service.query(request);

        assertEquals(2, response.getItems().size());
        assertEquals(RealtimeStatus.PARTIAL_SUCCESS, response.getRealtimeStatus());
        assertTrue(response.getPartialFailedIds().isEmpty());
        assertEquals(Boolean.TRUE, response.getQueryMeta().get("fallbackUsed"));
    }

    @Test
    void queryShouldTreatGatewayFailedStatusAsFailure() {
        properties.setGatewayEnabled(true);
        RealtimeQueryRequest request = request(1L);

        RealtimeQueryResponse gateway = new RealtimeQueryResponse();
        gateway.setRealtimeStatus(RealtimeStatus.FAILED);
        when(realtimeGatewayClient.query(any(RealtimeQueryRequest.class))).thenReturn(gateway);

        RealtimeQueryResponse fallback = fallbackResponse(1L, RealtimeStatus.DEGRADED);
        when(productRealtimeFallbackService.query(any(RealtimeQueryRequest.class), any(java.util.List.class), any(String.class)))
                .thenReturn(fallback);

        RealtimeQueryResponse response = service.query(request);
        Map<?, ?> metrics = (Map<?, ?>) service.status().get("metrics");

        assertEquals(RealtimeStatus.DEGRADED, response.getRealtimeStatus());
        assertEquals(0L, metrics.get("gatewaySuccessCount"));
        assertEquals(1L, metrics.get("gatewayFailureCount"));
    }

    @Test
    void queryShouldOpenCircuitAfterGatewayFailures() {
        properties.setGatewayEnabled(true);
        RealtimeQueryRequest request = request(1L);
        request.setForceRefresh(true);
        RealtimeQueryResponse fallback = fallbackResponse(1L, RealtimeStatus.DEGRADED);
        when(productRealtimeFallbackService.query(any(RealtimeQueryRequest.class), any(java.util.List.class), any(String.class)))
                .thenReturn(fallback);
        when(realtimeGatewayClient.query(any(RealtimeQueryRequest.class)))
                .thenThrow(new BizException(ErrorCode.REALTIME_TIMEOUT, "timeout"));

        service.query(request);
        service.query(request);
        RealtimeQueryResponse response = service.query(request);

        assertEquals(RealtimeStatus.DEGRADED, response.getRealtimeStatus());
        assertEquals(Boolean.TRUE, response.getQueryMeta().get("gatewaySkipped"));
        verify(realtimeGatewayClient, times(2)).query(any(RealtimeQueryRequest.class));
        assertEquals("OPEN", ((Map<?, ?>) service.status().get("circuit")).get("state"));
    }

    @Test
    void resetCircuitShouldCloseBreakerAndClearFailures() {
        properties.setGatewayEnabled(true);
        RealtimeQueryRequest request = request(1L);
        request.setForceRefresh(true);
        RealtimeQueryResponse fallback = fallbackResponse(1L, RealtimeStatus.DEGRADED);
        when(productRealtimeFallbackService.query(any(RealtimeQueryRequest.class), any(java.util.List.class), any(String.class)))
                .thenReturn(fallback);
        when(realtimeGatewayClient.query(any(RealtimeQueryRequest.class)))
                .thenThrow(new BizException(ErrorCode.REALTIME_TIMEOUT, "timeout"));

        service.query(request);
        service.query(request);

        Map<String, Object> circuit = service.resetCircuit();
        assertEquals("CLOSED", circuit.get("state"));
        assertEquals(0, circuit.get("consecutiveFailures"));
        assertFalse((Long) circuit.get("remainingOpenMs") > 0L);
    }

    @Test
    void smokeGatewayShouldReturnStructuredSuccessWhenGatewayWorks() {
        properties.setGatewayEnabled(true);
        RealtimeQueryResponse gateway = new RealtimeQueryResponse();
        gateway.setRealtimeStatus(RealtimeStatus.SUCCESS);
        gateway.getItems().add(item(1L, false));
        when(realtimeGatewayClient.query(any(RealtimeQueryRequest.class))).thenReturn(gateway);

        Map<String, Object> result = service.smokeGateway(request(1L));

        assertEquals(Boolean.TRUE, result.get("success"));
        assertEquals("success", result.get("realtimeStatus"));
        assertEquals(1, result.get("itemCount"));
    }

    @Test
    void smokeGatewayShouldReturnFailureWhenGatewayStatusFailed() {
        properties.setGatewayEnabled(true);
        RealtimeQueryResponse gateway = new RealtimeQueryResponse();
        gateway.setRealtimeStatus(RealtimeStatus.FAILED);
        when(realtimeGatewayClient.query(any(RealtimeQueryRequest.class))).thenReturn(gateway);

        Map<String, Object> result = service.smokeGateway(request(1L));

        assertEquals(Boolean.FALSE, result.get("success"));
        assertEquals("failed", result.get("realtimeStatus"));
        assertEquals("gateway_failed", result.get("reason"));
    }

    @Test
    void smokeGatewayShouldExplainWhenGatewayDisabled() {
        Map<String, Object> result = service.smokeGateway(null);

        assertEquals(Boolean.FALSE, result.get("success"));
        assertEquals("gateway_disabled", result.get("reason"));
    }

    private RealtimeQueryRequest request(Long... ids) {
        RealtimeQueryRequest request = new RealtimeQueryRequest();
        request.setEntityType("product");
        request.setEntityIds(Arrays.asList(ids));
        request.setQueryType("inventory");
        request.setUserId("u-1");
        return request;
    }

    private RealtimeQueryResponse fallbackResponse(Long id, RealtimeStatus status) {
        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(status);
        response.getItems().add(item(id, true));
        response.getQueryMeta().put("source", "snapshot_fallback");
        return response;
    }

    private RealtimeResultItem item(Long id, boolean degraded) {
        RealtimeResultItem item = new RealtimeResultItem();
        item.setEntityId(String.valueOf(id));
        item.setSuccess(true);
        item.setDegraded(degraded);
        item.setInventoryStatus("available");
        item.setInventoryCount(10);
        item.setSellStatus("on_sale");
        item.setBookable(true);
        item.setPrice(new BigDecimal("10.00"));
        item.setCurrency("CNY");
        item.setSource(degraded ? "snapshot_fallback" : "gateway");
        return item;
    }
}
