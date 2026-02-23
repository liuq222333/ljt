package com.example.demo.demos.LocalActive.Service.Impl;

import com.example.demo.demos.LocalActive.DTO.LocalActivityHeatPoint;
import com.example.demo.demos.LocalActive.DTO.NearbyActivityDTO;
import com.example.demo.demos.LocalActive.Dao.LocalActivityHeatMapMapper;
import com.example.demo.demos.LocalActive.Pojo.LocalActivity;
import com.example.demo.demos.LocalActive.Service.LocalActivityHeatMapService;
import com.example.demo.demos.LocalActive.Service.LocalActivitySearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocalActivityHeatMapServiceImpl implements LocalActivityHeatMapService {

    private final LocalActivitySearchService searchService;
    private final LocalActivityHeatMapMapper heatMapMapper;

    @Override
    public List<LocalActivityHeatPoint> getHeatPoints(double lat, double lon, double radiusKm, String category, String keyword, int size) {
        try {
            List<NearbyActivityDTO> nearby = searchService.searchNearby(lat, lon, radiusKm, category, keyword, size);
            if (!CollectionUtils.isEmpty(nearby)) {
                return toHeatPoints(nearby);
            }
        } catch (Exception ignore) {
        }
        // fallback to DB if redisearch fails
        List<LocalActivity> list = heatMapMapper.listPublished();
        return fallback(list, lat, lon, radiusKm, size);
    }

    private List<LocalActivityHeatPoint> toHeatPoints(List<NearbyActivityDTO> nearby) {
        List<LocalActivityHeatPoint> result = new ArrayList<>();
        for (NearbyActivityDTO n : nearby) {
            if (n.getLatitude() == null || n.getLongitude() == null) continue;
            result.add(new LocalActivityHeatPoint(
                    n.getLatitude(),
                    n.getLongitude(),
                    resolveWeight(n.getCapacity())
            ));
        }
        return result;
    }

    private List<LocalActivityHeatPoint> fallback(List<LocalActivity> list, double lat, double lon, double radiusKm, int size) {
        List<LocalActivityHeatPoint> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(list)) return result;
        for (LocalActivity a : list) {
            if (a.getLatitude() == null || a.getLongitude() == null) continue;
            Double dist = distanceKm(lat, lon, a.getLatitude(), a.getLongitude());
            if (dist != null && dist <= radiusKm) {
                result.add(new LocalActivityHeatPoint(
                        a.getLatitude(),
                        a.getLongitude(),
                        resolveWeight(a.getCapacity())
                ));
                if (result.size() >= size) break;
            }
        }
        return result;
    }

    private double resolveWeight(Integer capacity) {
        if (capacity != null && capacity > 0) {
            return Math.log(capacity + 1);
        }
        return 1.0;
    }

    private Double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 10.0) / 10.0;
    }
}
