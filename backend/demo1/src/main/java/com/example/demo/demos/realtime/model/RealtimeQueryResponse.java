package com.example.demo.demos.realtime.model;

import com.example.demo.demos.common.enums.RealtimeStatus;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class RealtimeQueryResponse {

    private List<RealtimeResultItem> items = new ArrayList<RealtimeResultItem>();
    private List<Long> partialFailedIds = new ArrayList<Long>();
    private RealtimeStatus realtimeStatus = RealtimeStatus.FAILED;
    private Map<String, Object> queryMeta = new LinkedHashMap<String, Object>();
}
