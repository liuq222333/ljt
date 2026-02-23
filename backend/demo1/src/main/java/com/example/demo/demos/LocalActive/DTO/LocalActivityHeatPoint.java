package com.example.demo.demos.LocalActive.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LocalActivityHeatPoint {
    private Double lat;
    private Double lon;
    private Double weight;
}
