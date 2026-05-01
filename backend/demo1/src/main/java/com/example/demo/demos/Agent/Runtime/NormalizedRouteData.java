package com.example.demo.demos.Agent.Runtime;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class NormalizedRouteData {

    private String entityType;
    private long total;
    private List<RouteEntityCandidate> items = new ArrayList<RouteEntityCandidate>();
    private String sourceRouteId;
    private String sourceResource;
    private String sourceAction;
    private String presentationHint;
    private String rawStatus;
    private boolean degraded;
    private String errorMessage;
    private Map<String, Object> rawMeta = new LinkedHashMap<String, Object>();
}
