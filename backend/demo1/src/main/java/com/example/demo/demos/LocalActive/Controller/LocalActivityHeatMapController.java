package com.example.demo.demos.LocalActive.Controller;

import com.example.demo.demos.LocalActive.DTO.LocalActivityHeatPoint;
import com.example.demo.demos.LocalActive.Service.LocalActivityHeatMapService;
import com.example.demo.demos.generic.Resp;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/local-act/heatmap")
@RequiredArgsConstructor
public class LocalActivityHeatMapController {

    private final LocalActivityHeatMapService heatMapService;

    @Operation(summary = "获取附近活动热力点")
    @GetMapping
    public Resp<List<LocalActivityHeatPoint>> getHeatmapPoints(
            @RequestParam("lat") double lat,
            @RequestParam("lon") double lon,
            @RequestParam(value = "radiusKm", defaultValue = "5") double radiusKm,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "size", defaultValue = "500") int size
    ) {
        return Resp.success(heatMapService.getHeatPoints(lat, lon, radiusKm, category, keyword, size));
    }
}
