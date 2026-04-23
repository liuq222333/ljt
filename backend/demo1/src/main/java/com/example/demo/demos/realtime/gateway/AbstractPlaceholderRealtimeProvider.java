package com.example.demo.demos.realtime.gateway;

import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class AbstractPlaceholderRealtimeProvider implements RealtimeEntityProvider {

    @Override
    public RealtimeQueryResponse query(RealtimeQueryRequest request) {
        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.DEGRADED);
        response.setPartialFailedIds(new ArrayList<Long>(request.getEntityIds()));
        response.getQueryMeta().put("implemented", false);
        response.getQueryMeta().put("reason", "provider_not_implemented");
        response.getQueryMeta().put("entityType", getEntityType());
        response.getQueryMeta().put("source", "internal_placeholder_provider");
        return response;
    }

    @Override
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("implemented", false);
        result.put("entityType", getEntityType());
        result.put("reason", "provider_not_implemented");
        return result;
    }
}
