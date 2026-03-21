package com.example.demo.demos.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 输出安全守卫 — LLM 回答输出前的敏感信息过滤。
 * 检测并脱敏内部表名、字段名、接口地址、其他用户信息、成本数据等。
 * 与施工单 W01 安全组件 + 总设计文档 §15.2 对齐。
 */
@Component
public class OutputGuardrail {

    private static final Logger log = LoggerFactory.getLogger(OutputGuardrail.class);

    /** 内部表名/字段名关键词（出现在回答中说明泄露了实现细节） */
    private static final List<String> INTERNAL_KEYWORDS = Arrays.asList(
            "product_search_snapshot",
            "event_search_snapshot",
            "store_search_snapshot",
            "knowledge_base",
            "knowledge_vector",
            "knowledge_chunk",
            "api_route",
            "chat_feedback",
            "audit_log",
            "searchable_status",
            "internal_id",
            "cost_price",
            "profit_margin",
            "supplier_id"
    );

    /** 匹配内部接口地址的模式 */
    private static final List<Pattern> SENSITIVE_PATTERNS = Arrays.asList(
            // 内部 API 地址
            Pattern.compile("https?://localhost[:/]\\S*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("https?://127\\.0\\.0\\.1[:/]\\S*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("https?://10\\.\\d+\\.\\d+\\.\\d+[:/]\\S*"),
            Pattern.compile("https?://172\\.(1[6-9]|2\\d|3[01])\\.\\d+\\.\\d+[:/]\\S*"),
            Pattern.compile("https?://192\\.168\\.\\d+\\.\\d+[:/]\\S*"),
            // SQL 语句片段
            Pattern.compile("(?i)(SELECT|INSERT|UPDATE|DELETE|DROP|ALTER)\\s+.*(FROM|INTO|TABLE)\\s+"),
            // 手机号（简单匹配，脱敏为 138****1234）
            Pattern.compile("1[3-9]\\d{9}")
    );

    /**
     * 过滤 LLM 输出中的敏感信息。
     * 发现敏感内容时脱敏处理，不抛异常（保证用户能收到回答）。
     *
     * @param output LLM 原始输出
     * @return 脱敏后的输出
     */
    public String filter(String output) {
        if (output == null || output.isBlank()) {
            return output;
        }

        String result = output;

        // 1. 内部关键词替换
        for (String keyword : INTERNAL_KEYWORDS) {
            if (result.contains(keyword)) {
                log.warn("OutputGuardrail 检测到内部关键词泄露: {}", keyword);
                result = result.replace(keyword, "[内部信息已隐藏]");
            }
        }

        // 2. 敏感模式替换
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            var matcher = pattern.matcher(result);
            if (matcher.find()) {
                log.warn("OutputGuardrail 检测到敏感模式: {}", pattern.pattern());
                result = matcher.replaceAll("[敏感信息已隐藏]");
            }
        }

        return result;
    }
}
