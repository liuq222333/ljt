package com.example.demo.demos.realtime.service;

import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;

import java.util.List;

public interface RealtimeFallbackProvider {

    String getEntityType();

    RealtimeQueryResponse query(RealtimeQueryRequest request, List<Long> targetIds, String fallbackReason);
}
