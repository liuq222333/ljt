package com.example.demo.demos.LocalActive.DTO;

import lombok.Data;

@Data
public class NeighborSupportTaskRequest {
    private String username;
    private String title;
    private String category;
    private String description;
    private String location;
    private Double latitude;
    private Double longitude;
    private String startTime; // ISO datetime string
    private String endTime;   // ISO datetime string
    private Integer volunteerSlots;
    private String priority; // LOW/MEDIUM/HIGH
    private Integer rewardPoints;
}
