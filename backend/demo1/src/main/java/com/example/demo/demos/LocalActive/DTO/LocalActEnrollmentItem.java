package com.example.demo.demos.LocalActive.DTO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LocalActEnrollmentItem {
    private Long id;
    private Long activityId;
    private String title;
    private String organizer;
    private String location;
    private String status;
    private String reminder;
    private LocalDateTime startAt;
    private List<String> tags;
}
