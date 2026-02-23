package com.example.demo.demos.LocalActive.Pojo;

import lombok.Data;

import java.time.LocalTime;

@Data
public class LocalActivityScheduleTemplate {
    private Long id;
    private Integer ownerUserId;
    private String title;
    private String categoryCode;
    private Integer weekday;
    private LocalTime startTime;
    private LocalTime endTime;
    private String locationText;
    private String recurrenceRule;
    private Integer defaultCapacity;
    private String defaultFeeType;
    private Integer reminderMinutes;
    private String status;
}
