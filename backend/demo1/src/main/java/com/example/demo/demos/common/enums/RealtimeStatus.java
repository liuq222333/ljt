package com.example.demo.demos.common.enums;

/**
 * 实时查询状态 — 实时层返回的查询结果状态。
 * 与施工单 W10 实时层协议对齐。
 */
public enum RealtimeStatus {

    /** 查询成功，数据完整 */
    SUCCESS("success", "查询成功"),

    /** 部分成功（批量查询中部分实体成功） */
    PARTIAL_SUCCESS("partial_success", "部分成功"),

    /** 查询超时 */
    TIMEOUT("timeout", "查询超时"),

    /** 查询失败 */
    FAILED("failed", "查询失败"),

    /** 已降级（使用快照数据兜底） */
    DEGRADED("degraded", "已降级");

    private final String code;
    private final String description;

    RealtimeStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RealtimeStatus fromCode(String code) {
        if (code == null) {
            return FAILED;
        }
        for (RealtimeStatus status : values()) {
            if (status.code.equalsIgnoreCase(code.trim())) {
                return status;
            }
        }
        return FAILED;
    }
}
