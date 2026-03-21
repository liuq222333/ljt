package com.example.demo.demos.common.schema;

import com.example.demo.demos.common.enums.ParamSource;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 标准化参数 — normalize_params 节点的输出。
 * 所有字段均为可执行级参数（ID 级），可直接传给结构化搜索接口。
 * 与施工单 W00 NormalizedParams schema 对齐。
 */
@Data
public class NormalizedParams {

    /** 搜索关键词（清洗后） */
    private String keywords;

    /** 类目 ID（name→id 收敛后） */
    private Long categoryId;

    /** 标签 ID 列表（name→id 收敛后） */
    private List<Long> tagIds = new ArrayList<>();

    /** 地理位置 */
    private Location location;

    /** 价格区间 */
    private PriceRange priceRange;

    /** 时间区间 */
    private TimeRange timeRange;

    /** 排序字段（如 price / distance / score / created_at） */
    private String sortBy;

    /** 排序方向（asc / desc） */
    private String sortOrder;

    /** 当前页码（从 1 开始） */
    private Integer page = 1;

    /** 每页数量 */
    private Integer pageSize = 10;

    /** 目标实体类型（product / event / store） */
    private String entityType;

    /** 每个参数的来源标记，key=参数名，value=来源 */
    private Map<String, ParamSource> paramsWithSource = new HashMap<>();

    /** 参数校验警告信息（如"价格区间可能过窄"） */
    private List<String> warnings = new ArrayList<>();

    /** 被丢弃的参数及原因 */
    private List<String> droppedParams = new ArrayList<>();

    /** 参数是否通过校验，可直接执行 */
    private boolean executionReady = true;

    // ========== 内嵌结构 ==========

    @Data
    public static class Location {
        /** 纬度 */
        private Double lat;
        /** 经度 */
        private Double lng;
        /** 搜索半径（公里） */
        private Double radiusKm;
    }

    @Data
    public static class PriceRange {
        /** 最低价 */
        private BigDecimal min;
        /** 最高价 */
        private BigDecimal max;
    }

    @Data
    public static class TimeRange {
        /** 开始时间 */
        private LocalDateTime start;
        /** 结束时间 */
        private LocalDateTime end;
    }
}
