package com.example.demo.demos.LocalActive.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LocalActivityDetail {
    private Long id;
    private Integer organizerUserId;
    private String organizer;
    private String title;
    private String subtitle;
    private String category;
    private String description;
    private String location;
    private Double latitude;
    private Double longitude;
    private String address;
    private String coverUrl;
    private Integer capacity;
    private Integer reserved;
    private String feeType;
    private BigDecimal feeAmount;
    private Boolean allowWaitlist;
    private Boolean requireCheckin;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer reminderMinutes;
    private String reviewNote;
    private List<String> tags;
    private String enrollmentStatus;
}
