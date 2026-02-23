package com.example.demo.demos.CommunityFeed.Pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommunityFeedComment {
    private Long id;
    private Long feedId;
    private Long userId;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
