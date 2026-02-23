package com.example.demo.demos.LocalActive.Service;

import com.example.demo.demos.LocalActive.DTO.NearbyActivityDTO;

import java.util.List;

public interface LocalActivitySearchService {
    List<NearbyActivityDTO> searchNearby(double lat, double lon, double radiusKm, String category, String keyword, int size);

    int syncFromDb();
}
