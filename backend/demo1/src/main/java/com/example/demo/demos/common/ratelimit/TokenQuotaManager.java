package com.example.demo.demos.common.ratelimit;

import com.example.demo.demos.common.error.BizException;
import com.example.demo.demos.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token 配额控制器。
 * - 单次请求 Input 上限 2000 tokens、Output 上限 1000 tokens
 * - 日消耗达预算 80% 时告警、100% 时降级为缓存回答
 * 与施工单 W01 限流组件 + 总设计文档 §15.3 对齐。
 */
@Component
public class TokenQuotaManager {

    private static final Logger log = LoggerFactory.getLogger(TokenQuotaManager.class);

    /** 单次请求 Input 上限 tokens */
    public static final int MAX_INPUT_TOKENS = 2000;

    /** 单次请求 Output 上限 tokens */
    public static final int MAX_OUTPUT_TOKENS = 1000;

    /** 日 Token 预算（默认 500K） */
    private static final int DAILY_BUDGET = 500_000;

    /** 当日已消耗 token 数 */
    private final AtomicInteger dailyUsed = new AtomicInteger(0);

    /** 当天标记（用于自动重置） */
    private volatile long currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24);

    /**
     * 检查单次请求 token 是否超限。
     *
     * @param inputTokens  输入 token 数
     * @param outputTokens 输出 token 数
     */
    public void checkSingleRequest(int inputTokens, int outputTokens) {
        if (inputTokens > MAX_INPUT_TOKENS) {
            log.warn("单次请求 Input Token 超限: actual={}, max={}", inputTokens, MAX_INPUT_TOKENS);
            throw new BizException(ErrorCode.AGENT_TOKEN_QUOTA_EXCEEDED,
                    "输入内容过长，请精简后重试");
        }
        if (outputTokens > MAX_OUTPUT_TOKENS) {
            log.warn("单次请求 Output Token 超限: actual={}, max={}", outputTokens, MAX_OUTPUT_TOKENS);
        }
    }

    /**
     * 记录本次请求消耗的 token 并检查日预算。
     *
     * @param tokensUsed 本次消耗的总 token 数
     * @return 是否应降级（日预算已耗尽）
     */
    public boolean recordUsageAndCheckBudget(int tokensUsed) {
        resetIfNewDay();

        int total = dailyUsed.addAndGet(tokensUsed);

        if (total >= DAILY_BUDGET) {
            log.error("日 Token 预算已耗尽: used={}, budget={}", total, DAILY_BUDGET);
            return true; // 需要降级
        }

        if (total >= DAILY_BUDGET * 0.8) {
            log.warn("日 Token 预算使用已达 80%: used={}, budget={}", total, DAILY_BUDGET);
        }

        return false;
    }

    /**
     * 获取日预算使用百分比。
     */
    public double usagePercent() {
        resetIfNewDay();
        return (double) dailyUsed.get() / DAILY_BUDGET * 100;
    }

    /**
     * 是否需要降级（日预算已耗尽）。
     */
    public boolean shouldDegrade() {
        resetIfNewDay();
        return dailyUsed.get() >= DAILY_BUDGET;
    }

    private void resetIfNewDay() {
        long today = System.currentTimeMillis() / (1000 * 60 * 60 * 24);
        if (today != currentDay) {
            synchronized (this) {
                if (today != currentDay) {
                    dailyUsed.set(0);
                    currentDay = today;
                    log.info("Token 日预算已重置");
                }
            }
        }
    }
}
