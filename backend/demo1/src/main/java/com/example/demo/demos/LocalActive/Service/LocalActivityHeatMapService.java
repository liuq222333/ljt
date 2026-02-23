package com.example.demo.demos.LocalActive.Service;

import com.example.demo.demos.LocalActive.DTO.LocalActivityHeatPoint;

import java.util.List;

public interface LocalActivityHeatMapService {
    List<LocalActivityHeatPoint> getHeatPoints(double lat, double lon, double radiusKm, String category, String keyword, int size);
}
