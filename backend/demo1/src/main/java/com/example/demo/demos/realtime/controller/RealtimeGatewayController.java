package com.example.demo.demos.realtime.controller;

import com.example.demo.demos.generic.Resp;
import com.example.demo.demos.realtime.gateway.InternalRealtimeGatewayService;
import com.example.demo.demos.realtime.model.RealtimeBatchQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeBatchQueryResponse;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/realtime/gateway")
public class RealtimeGatewayController {

    private final InternalRealtimeGatewayService internalRealtimeGatewayService;

    public RealtimeGatewayController(InternalRealtimeGatewayService internalRealtimeGatewayService) {
        this.internalRealtimeGatewayService = internalRealtimeGatewayService;
    }

    @GetMapping("/health")
    public Resp<Map<String, Object>> health() {
        return Resp.success(internalRealtimeGatewayService.health());
    }

    @PostMapping("/query")
    public Resp<RealtimeQueryResponse> query(@RequestBody RealtimeQueryRequest request) {
        return Resp.success(internalRealtimeGatewayService.query(request));
    }

    @PostMapping("/query/batch")
    public Resp<RealtimeBatchQueryResponse> batchQuery(@RequestBody(required = false) RealtimeBatchQueryRequest request) {
        return Resp.success(internalRealtimeGatewayService.batchQuery(request));
    }
}
