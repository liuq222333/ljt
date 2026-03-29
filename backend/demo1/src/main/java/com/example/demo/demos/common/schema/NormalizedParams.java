package com.example.demo.demos.common.schema;

import com.example.demo.demos.common.enums.ParamSource;
import com.example.demo.demos.common.enums.TaskType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class NormalizedParams {

    private TaskType taskType;
    private String keywords;
    private Long categoryId;
    private List<Long> categoryPathIds = new ArrayList<Long>();
    private List<Long> tagIds = new ArrayList<Long>();
    private List<String> tagNames = new ArrayList<String>();
    private Long cityId;
    private Long districtId;
    private Long businessAreaId;
    private Integer distanceKm;
    private Location location;
    private PriceRange priceRange;
    private TimeRange timeRange;
    private String sortBy;
    private String sortOrder;
    private Integer page = 1;
    private Integer pageSize = 10;
    private String entityType;

    private Map<String, Object> filters = new HashMap<String, Object>();
    private Map<String, Object> sort = new HashMap<String, Object>();
    private Map<String, Object> explicitConstraints = new HashMap<String, Object>();
    private Map<String, Object> inferredConstraints = new HashMap<String, Object>();
    private Map<String, Object> defaultConstraints = new HashMap<String, Object>();
    private Map<String, ParamSource> paramsWithSource = new HashMap<String, ParamSource>();
    private List<String> warnings = new ArrayList<String>();
    private List<String> missingRequiredSlots = new ArrayList<String>();
    private List<String> validationErrors = new ArrayList<String>();
    private List<String> droppedParams = new ArrayList<String>();
    private boolean executionReady = true;

    @Data
    public static class Location {
        private Double lat;
        private Double lng;
        private Double radiusKm;
    }

    @Data
    public static class PriceRange {
        private BigDecimal min;
        private BigDecimal max;
    }

    @Data
    public static class TimeRange {
        private LocalDateTime start;
        private LocalDateTime end;
    }
}
