package com.example.demo.demos.realtime.controller;

import com.example.demo.demos.generic.Resp;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.service.RealtimeQueryOrchestratorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/realtime")
public class RealtimeAdminController {

    private final RealtimeQueryOrchestratorService realtimeQueryOrchestratorService;

    public RealtimeAdminController(RealtimeQueryOrchestratorService realtimeQueryOrchestratorService) {
        this.realtimeQueryOrchestratorService = realtimeQueryOrchestratorService;
    }

    @GetMapping("/admin/status")
    public Resp<Map<String, Object>> status() {
        return Resp.success(realtimeQueryOrchestratorService.status());
    }

    @PostMapping("/admin/cache/clear")
    public Resp<Map<String, Object>> clearCache() {
        return Resp.success(realtimeQueryOrchestratorService.clearCache());
    }

    @PostMapping("/admin/circuit/reset")
    public Resp<Map<String, Object>> resetCircuit() {
        return Resp.success(realtimeQueryOrchestratorService.resetCircuit());
    }

    @PostMapping("/admin/smoke/gateway")
    public Resp<Map<String, Object>> smokeGateway(@RequestBody(required = false) RealtimeQueryRequest request) {
        return Resp.success(realtimeQueryOrchestratorService.smokeGateway(request));
    }

    @PostMapping("/query")
    public Resp<RealtimeQueryResponse> query(@RequestBody RealtimeQueryRequest request) {
        return Resp.success(realtimeQueryOrchestratorService.query(request));
    }
}
