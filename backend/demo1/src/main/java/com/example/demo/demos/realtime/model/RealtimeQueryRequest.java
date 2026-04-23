package com.example.demo.demos.realtime.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RealtimeQueryRequest {

    private String entityType;
    private List<Long> entityIds = new ArrayList<Long>();
    private String queryType;
    private String date;
    private String timeSlot;
    private String traceId;
    private Integer timeoutMs;
    private String userId;
    private boolean forceRefresh;
}
