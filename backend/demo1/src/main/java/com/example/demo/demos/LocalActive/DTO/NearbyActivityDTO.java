package com.example.demo.demos.LocalActive.DTO;

import lombok.Data;

@Data
public class NearbyActivityDTO {
    private Long id;
    private String title;
    private String category;
    private String location;
    private String status;
    private String startAt;
    private String endAt;
    private String coverUrl;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private Integer capacity;
}
