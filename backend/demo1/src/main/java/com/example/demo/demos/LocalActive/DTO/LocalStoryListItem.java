package com.example.demo.demos.LocalActive.DTO;

import lombok.Data;

@Data
public class LocalStoryListItem {
    private Long id;
    private String title;
    private String summary;
    private String coverUrl;
    private String author;
    private String visibility;
    private Integer likes;
    private String createdAt;
}
