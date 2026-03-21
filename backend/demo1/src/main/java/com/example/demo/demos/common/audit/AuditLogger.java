package com.example.demo.demos.common.audit;

import com.example.demo.demos.common.trace.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计日志写入组件 — 异步写入审计日志，不阻塞主链路。
 * 与施工单 W01 审计日志对齐。
 */
@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    private final AuditLogMapper auditLogMapper;

    public AuditLogger(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 异步记录审计日志。
     * 写入失败只记录告警，不影响主链路。
     */
    @Async
    public void log(String userId, String rawInput, String parsedIntent,
                    String toolCalls, String finalAnswer, boolean isDegraded,
                    String riskLevel, long durationMs, int tokensUsed) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setRequestId(TraceContext.getRequestId());
            auditLog.setTraceId(TraceContext.getTraceId());
            auditLog.setUserId(userId);
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setRawInput(rawInput);
            auditLog.setParsedIntent(parsedIntent);
            auditLog.setToolCalls(toolCalls);
            auditLog.setFinalAnswer(truncate(finalAnswer, 2000));
            auditLog.setIsDegraded(isDegraded);
            auditLog.setRiskLevel(riskLevel);
            auditLog.setDurationMs(durationMs);
            auditLog.setTokensUsed(tokensUsed);
            auditLog.setCreatedAt(LocalDateTime.now());

            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("审计日志写入失败: requestId={}, error={}",
                    TraceContext.getRequestId(), e.getMessage());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
