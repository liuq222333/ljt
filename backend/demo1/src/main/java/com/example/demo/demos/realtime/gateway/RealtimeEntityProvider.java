package com.example.demo.demos.realtime.gateway;

import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;

import java.util.Map;

public interface RealtimeEntityProvider {

    String getEntityType();

    RealtimeQueryResponse query(RealtimeQueryRequest request);

    Map<String, Object> health();
}
