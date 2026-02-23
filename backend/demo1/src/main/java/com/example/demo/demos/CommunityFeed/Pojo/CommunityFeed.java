package com.example.demo.demos.CommunityFeed.Pojo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CommunityFeed {
    private Long id;
    private Long userId;
    private String content;
    // JSON array stored as string (e.g. ["key1","key2"])
    private String images;
    // JSON array stored as string for topics (e.g. ["topicA","topicB"])
    private String visibility;
    private String locationText;
    private Integer likesCount;
    private Integer commentsCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // denormalized fields from join
    private String username;
    private String userAvatar;
}
