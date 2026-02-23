package com.example.demo.demos.LocalActive.Pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LocalActivity {
    private Long id;
    private Integer organizerUserId;
    private String title;
    private String subtitle;
    private String categoryCode;
    private String description;
    private String locationText;
    private Double latitude;
    private Double longitude;
    private String address;
    private String coverUrl;
    private Integer capacity;
    private String feeType;
    private BigDecimal feeAmount;
    private Boolean allowWaitlist;
    private Boolean requireCheckin;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer reminderMinutes;
    private String reviewNote;
}
