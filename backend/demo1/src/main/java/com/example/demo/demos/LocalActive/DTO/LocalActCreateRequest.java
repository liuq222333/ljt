package com.example.demo.demos.LocalActive.DTO;

import lombok.Data;

import java.util.List;

@Data
public class LocalActCreateRequest {
    private String username;
    private String title;
    private String subtitle;
    private String category;
    private String date;
    private String timeStart;
    private String timeEnd;
    private String location;
    private Integer capacity;
    private String fee;
    private String description;
    private List<String> tags;
    private String registration;
    private String reviewNote;
    private String reminder;
    private String checkin;
    private String waiting;
    private String status;
}
