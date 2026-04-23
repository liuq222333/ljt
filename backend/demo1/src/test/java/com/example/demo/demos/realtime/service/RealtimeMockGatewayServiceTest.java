package com.example.demo.demos.realtime.service;

import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealtimeMockGatewayServiceTest {

    private final RealtimeMockGatewayService service = new RealtimeMockGatewayService();

    @Test
    void queryShouldReturnSuccessForValidIds() {
        RealtimeQueryRequest request = new RealtimeQueryRequest();
        request.setEntityType("product");
        request.setEntityIds(Arrays.asList(1L, 2L));
        request.setQueryType("availability");

        RealtimeQueryResponse response = service.query(request);

        assertEquals(RealtimeStatus.SUCCESS, response.getRealtimeStatus());
        assertEquals(2, response.getItems().size());
        assertEquals("mock_gateway", response.getQueryMeta().get("source"));
    }

    @Test
    void queryShouldReturnPartialSuccessWhenContainsInvalidId() {
        RealtimeQueryRequest request = new RealtimeQueryRequest();
        request.setEntityType("product");
        request.setEntityIds(Arrays.asList(1L, -1L));
        request.setQueryType("availability");

        RealtimeQueryResponse response = service.query(request);

        assertEquals(RealtimeStatus.PARTIAL_SUCCESS, response.getRealtimeStatus());
        assertEquals(1, response.getItems().size());
        assertEquals(1, response.getPartialFailedIds().size());
    }

    @Test
    void queryShouldFailWhenIdsEmpty() {
        RealtimeQueryRequest request = new RealtimeQueryRequest();
        request.setEntityType("product");
        request.setEntityIds(Collections.<Long>emptyList());

        RealtimeQueryResponse response = service.query(request);

        assertEquals(RealtimeStatus.FAILED, response.getRealtimeStatus());
        assertEquals("empty_ids", response.getQueryMeta().get("reason"));
    }
}
