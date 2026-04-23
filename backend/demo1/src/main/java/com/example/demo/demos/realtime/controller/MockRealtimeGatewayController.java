package com.example.demo.demos.realtime.controller;

import com.example.demo.demos.generic.Resp;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.service.RealtimeMockGatewayService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/realtime/mock")
@ConditionalOnProperty(prefix = "agent.realtime", name = "mock-gateway-enabled", havingValue = "true")
public class MockRealtimeGatewayController {

    private final RealtimeMockGatewayService realtimeMockGatewayService;

    public MockRealtimeGatewayController(RealtimeMockGatewayService realtimeMockGatewayService) {
        this.realtimeMockGatewayService = realtimeMockGatewayService;
    }

    @GetMapping("/status")
    public Resp<Map<String, Object>> status() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("enabled", true);
        result.put("mode", "mock_gateway");
        result.put("hint", "Point agent.realtime.gateway-base-url to this service for local smoke tests.");
        return Resp.success(result);
    }

    @PostMapping("/query")
    public RealtimeQueryResponse query(@RequestBody RealtimeQueryRequest request) {
        return realtimeMockGatewayService.query(request);
    }
}
