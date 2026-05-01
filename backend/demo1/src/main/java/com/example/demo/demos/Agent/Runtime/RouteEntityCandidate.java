package com.example.demo.demos.Agent.Runtime;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class RouteEntityCandidate {

    private String entityId;
    private String title;
    private String subtitle;
    private String priceText;
    private String imageUrl;
    private String locationText;
    private String realtimeStatusText;
    private List<String> tags = new ArrayList<String>();
    private List<String> highlights = new ArrayList<String>();
    private Map<String, Object> raw = new LinkedHashMap<String, Object>();
}
