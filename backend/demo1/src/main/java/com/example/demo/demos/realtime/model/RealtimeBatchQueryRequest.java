package com.example.demo.demos.realtime.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RealtimeBatchQueryRequest {

    private List<RealtimeQueryRequest> requests = new ArrayList<RealtimeQueryRequest>();
}
