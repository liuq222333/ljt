package com.example.demo.demos.realtime.service;

import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;

public interface RealtimeGatewayClient {

    RealtimeQueryResponse query(RealtimeQueryRequest request);
}
