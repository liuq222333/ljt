package com.example.demo.demos.LocalActive.DTO;

import lombok.Data;

@Data
public class NeighborSupportTaskDTO {
    private Long id;
    private String title;
    private String category;
    private String description;
    private String location;
    private Integer volunteerSlots;
    private Integer filledSlots;
    private String priority;
    private Integer rewardPoints;
    private String status;
    private String startTime;
    private String endTime;
    private String owner;
}
