package com.example.demo.demos.realtime.gateway;

import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.service.RealtimeQueryOrchestratorService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ProductRealtimeProvider implements RealtimeEntityProvider {

    private final RealtimeQueryOrchestratorService orchestratorService;

    public ProductRealtimeProvider(RealtimeQueryOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @Override
    public String getEntityType() {
        return "product";
    }

    @Override
    public RealtimeQueryResponse query(RealtimeQueryRequest request) {
        return orchestratorService.query(request);
    }

    @Override
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("implemented", true);
        result.put("entityType", getEntityType());
        result.put("orchestratorEnabled", true);
        return result;
    }
}
