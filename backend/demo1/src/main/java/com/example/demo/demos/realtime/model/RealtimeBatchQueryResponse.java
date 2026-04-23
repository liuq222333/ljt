package com.example.demo.demos.realtime.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class RealtimeBatchQueryResponse {

    private List<RealtimeQueryResponse> results = new ArrayList<RealtimeQueryResponse>();
    private Map<String, Object> batchMeta = new LinkedHashMap<String, Object>();
}
