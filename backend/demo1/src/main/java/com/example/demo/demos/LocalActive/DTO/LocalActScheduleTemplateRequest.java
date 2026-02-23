package com.example.demo.demos.LocalActive.DTO;

import lombok.Data;

/**
 * 创建固定日程模板请求
 */
@Data
public class LocalActScheduleTemplateRequest {
    private String username;
    private String title;
    private String category;
    private Integer weekday; // 0=周日,6=周六
    private String startTime; // HH:mm
    private String endTime;   // HH:mm
    private String location;
    private String recurrenceRule;
    private Integer capacity;
    private String feeType;
    private Integer reminderMinutes;
    private String status;
}
