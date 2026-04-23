package com.example.demo.demos.realtime.service;

import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.model.RealtimeResultItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class RealtimeMockGatewayService {

    public RealtimeQueryResponse query(RealtimeQueryRequest request) {
        RealtimeQueryResponse response = new RealtimeQueryResponse();
        if (request == null || request.getEntityIds() == null || request.getEntityIds().isEmpty()) {
            response.setRealtimeStatus(RealtimeStatus.FAILED);
            response.getQueryMeta().put("source", "mock_gateway");
            response.getQueryMeta().put("reason", "empty_ids");
            return response;
        }
        for (Long entityId : request.getEntityIds()) {
            if (entityId == null || entityId <= 0L) {
                response.getPartialFailedIds().add(entityId);
                continue;
            }
            response.getItems().add(buildItem(entityId));
        }
        if (response.getItems().isEmpty()) {
            response.setRealtimeStatus(RealtimeStatus.FAILED);
        } else if (!response.getPartialFailedIds().isEmpty()) {
            response.setRealtimeStatus(RealtimeStatus.PARTIAL_SUCCESS);
        } else {
            response.setRealtimeStatus(RealtimeStatus.SUCCESS);
        }
        response.getQueryMeta().put("source", "mock_gateway");
        response.getQueryMeta().put("queryType", request.getQueryType());
        response.getQueryMeta().put("traceId", request.getTraceId());
        response.getQueryMeta().put("queryCount", request.getEntityIds().size());
        return response;
    }

    private RealtimeResultItem buildItem(Long entityId) {
        RealtimeResultItem item = new RealtimeResultItem();
        item.setEntityId(String.valueOf(entityId));
        item.setQueryTs(LocalDateTime.now());
        item.setSuccess(true);
        item.setDegraded(false);
        item.setSource("mock_gateway");
        item.setInventoryCount((int) ((entityId % 7L) + 1L));
        item.setInventoryStatus(entityId % 5L == 0L ? "low_stock" : "available");
        item.setSellStatus(entityId % 11L == 0L ? "off_shelf" : "on_sale");
        item.setAvailabilityStatus(entityId % 11L == 0L ? "unavailable" : "available");
        item.setBookable(entityId % 11L != 0L);
        item.setPrice(new BigDecimal("99.00").add(new BigDecimal(entityId % 10L)));
        item.setCurrency("CNY");
        item.setBusinessStatus("open");
        item.setOpenNow(Boolean.TRUE);
        return item;
    }
}
