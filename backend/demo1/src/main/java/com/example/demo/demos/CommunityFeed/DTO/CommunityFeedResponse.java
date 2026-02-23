package com.example.demo.demos.CommunityFeed.DTO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommunityFeedResponse {
    private Long id;
    private Long userId;
    private String content;
    private List<String> images;
    private String visibility;
    private String locationText;
    private Integer likesCount;
    private Integer commentsCount;
    private String status;
    private LocalDateTime createdAt;
    private String userAvatar;
    private String username;
    private LocalDateTime updatedAt;
    private List<String> topics;
}
