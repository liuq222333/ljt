package com.example.demo.demos.LocalActive.DTO;

import lombok.Data;

@Data
public class LocalStoryCreateRequest {
    private String username;
    private Long activityId;
    private String title;
    private String coverUrl;
    private String summary;
    private String content;
    private String visibility;
}
