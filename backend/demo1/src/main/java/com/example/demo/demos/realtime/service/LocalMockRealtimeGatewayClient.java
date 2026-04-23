package com.example.demo.demos.realtime.service;

import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@ConditionalOnProperty(prefix = "agent.realtime", name = "mock-gateway-enabled", havingValue = "true")
public class LocalMockRealtimeGatewayClient implements RealtimeGatewayClient {

    private final RealtimeMockGatewayService realtimeMockGatewayService;

    public LocalMockRealtimeGatewayClient(RealtimeMockGatewayService realtimeMockGatewayService) {
        this.realtimeMockGatewayService = realtimeMockGatewayService;
    }

    @Override
    public RealtimeQueryResponse query(RealtimeQueryRequest request) {
        return realtimeMockGatewayService.query(request);
    }
}
