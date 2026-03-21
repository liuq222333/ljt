package com.example.demo.demos.common.trace;

import java.util.UUID;

/**
 * 请求追踪上下文 — 基于 ThreadLocal 传递 request_id / trace_id。
 * 所有接口、任务、索引写入、LLM 调用都应携带这两个 ID。
 * 与施工单 W01 通用要求对齐。
 */
public class TraceContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private TraceContext() {
    }

    /**
     * 初始化追踪上下文（通常在请求入口处调用）。
     * 如果外部未传入，则自动生成 UUID。
     */
    public static void init(String requestId, String traceId) {
        REQUEST_ID.set(requestId != null ? requestId : generateId());
        TRACE_ID.set(traceId != null ? traceId : generateId());
    }

    /**
     * 使用自动生成的 ID 初始化。
     */
    public static void init() {
        init(generateId(), generateId());
    }

    public static String getRequestId() {
        String id = REQUEST_ID.get();
        return id != null ? id : "unknown";
    }

    public static String getTraceId() {
        String id = TRACE_ID.get();
        return id != null ? id : "unknown";
    }

    /**
     * 请求结束后必须调用，防止 ThreadLocal 内存泄漏。
     */
    public static void clear() {
        REQUEST_ID.remove();
        TRACE_ID.remove();
    }

    private static String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
