package com.example.demo.demos.realtime.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RealtimeResultItem {

    private String entityId;
    private String inventoryStatus;
    private Integer inventoryCount;
    private String sellStatus;
    private String availabilityStatus;
    private Boolean bookable;
    private BigDecimal price;
    private String currency;
    private String businessStatus;
    private Boolean openNow;
    private LocalDateTime queryTs;
    private boolean success;
    private boolean degraded;
    private String source;
    private String errorCode;
    private String errorMessage;
}
