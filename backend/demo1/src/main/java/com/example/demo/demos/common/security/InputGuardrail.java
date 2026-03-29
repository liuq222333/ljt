package com.example.demo.demos.common.security;

import com.example.demo.demos.common.error.BizException;
import com.example.demo.demos.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 输入安全守卫 — 用户输入送入 LLM 前的 Prompt Injection 检测。
 * 检测到注入模式时抛出 BizException(AGENT_PROMPT_INJECTION)。
 * 与施工单 W01 安全组件 + 总设计文档 §15.1 对齐。
 */
@Component
public class InputGuardrail {

    private static final Logger log = LoggerFactory.getLogger(InputGuardrail.class);

    /** 单次输入最大长度（字符数），超长截断 */
    private static final int MAX_INPUT_LENGTH = 2000;

    /**
     * 检查用户输入安全性。
     * 不安全时抛出异常；安全时返回清洗后的输入。
     *
     * @param rawInput 用户原始输入
     * @return 清洗后的安全输入
     */
    public String check(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            return "";
        }

        // 1. 长度截断
        String input = rawInput.length() > MAX_INPUT_LENGTH
                ? rawInput.substring(0, MAX_INPUT_LENGTH)
                : rawInput;

        String lowerInput = input.toLowerCase();

        // 2. 关键词黑名单检测
        for (String keyword : InjectionPatterns.KEYWORD_BLACKLIST) {
            if (lowerInput.contains(keyword.toLowerCase())) {
                log.warn("InputGuardrail 检测到注入关键词: keyword={}, input={}",
                        keyword, truncateForLog(input));
                throw new BizException(ErrorCode.AGENT_PROMPT_INJECTION,
                        "检测到不安全的输入，请重新输入您的问题");
            }
        }

        // 3. 正则模式检测
        for (Pattern pattern : InjectionPatterns.REGEX_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("InputGuardrail 检测到注入模式: pattern={}, input={}",
                        pattern.pattern(), truncateForLog(input));
                throw new BizException(ErrorCode.AGENT_PROMPT_INJECTION,
                        "检测到不安全的输入，请重新输入您的问题");
            }
        }

        return input;
    }

    private String truncateForLog(String text) {
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}
