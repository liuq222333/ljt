package com.example.demo.demos.common.audit;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 MyBatis Mapper。
 */
@Mapper
public interface AuditLogMapper {

    @Insert("INSERT INTO audit_log (request_id, trace_id, user_id, timestamp, raw_input, " +
            "parsed_intent, tool_calls, final_answer, is_degraded, risk_level, " +
            "duration_ms, tokens_used, created_at) " +
            "VALUES (#{requestId}, #{traceId}, #{userId}, #{timestamp}, #{rawInput}, " +
            "#{parsedIntent}, #{toolCalls}, #{finalAnswer}, #{isDegraded}, #{riskLevel}, " +
            "#{durationMs}, #{tokensUsed}, #{createdAt})")
    int insert(AuditLog log);
}
