package com.example.demo.demos.LocalActive.Pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NeighborSupportTask {
    private Long id;
    private Integer requesterUserId;
    private Integer assigneeUserId;
    private String title;
    private String categoryCode;
    private String description;
    private String locationText;
    private Double latitude;
    private Double longitude;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer volunteerSlots;
    private Integer filledSlots;
    private String priority;
    private Integer rewardPoints;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
