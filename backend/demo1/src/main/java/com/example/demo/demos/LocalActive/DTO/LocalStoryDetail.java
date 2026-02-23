package com.example.demo.demos.LocalActive.DTO;

import lombok.Data;

@Data
public class LocalStoryDetail {
    private Long id;
    private Long activityId;
    private String title;
    private String coverUrl;
    private String summary;
    private String content;
    private String author;
    private String visibility;
    private Integer likes;
    private String createdAt;
}
