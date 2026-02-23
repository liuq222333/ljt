package com.example.demo.demos.LocalActive.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LocalActEnrollmentRecord {
    private Long enrollmentId;
    private Long activityId;
    private String title;
    private LocalDateTime startAt;
    private String location;
    private String organizer;
    private String status;
    private Integer waitlistRank;
    private String tagsCsv;
}
