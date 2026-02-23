package com.example.demo.demos.LocalActive.Pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LocalActivityStory {
    private Long id;
    private Long activityId;
    private Integer authorUserId;
    private String title;
    private String coverUrl;
    private String summary;
    private String content;
    private String visibility;
    private Integer likes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
