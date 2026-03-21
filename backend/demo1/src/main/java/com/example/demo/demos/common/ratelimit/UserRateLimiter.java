package com.example.demo.demos.common.ratelimit;

import com.example.demo.demos.common.error.BizException;
import com.example.demo.demos.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户级限流器。
 * - 单用户 QPS 限制（默认 2 QPS，突发允许 5）
 * - 单用户日调用量上限（默认 200 次/天）
 * 与施工单 W01 限流组件 + 总设计文档 §15.3 对齐。
 */
@Component
public class UserRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(UserRateLimiter.class);

    /** 单用户每秒请求上限（突发） */
    private static final int MAX_QPS = 5;

    /** 单用户日调用量上限 */
    private static final int MAX_DAILY_CALLS = 200;

    /** 用户 -> 当前秒计数器 {userId -> [secondTimestamp, count]} */
    private final Map<String, long[]> qpsCounters = new ConcurrentHashMap<>();

    /** 用户 -> 当日计数器 {userId -> [dayTimestamp, count]} */
    private final Map<String, long[]> dailyCounters = new ConcurrentHashMap<>();

    /**
     * 检查用户是否被限流。
     * 被限流时抛出 BizException(AGENT_RATE_LIMITED)。
     *
     * @param userId 用户 ID
     */
    public void checkLimit(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        checkQps(userId);
        checkDailyLimit(userId);
    }

    private void checkQps(String userId) {
        long currentSecond = System.currentTimeMillis() / 1000;
        long[] counter = qpsCounters.compute(userId, (k, v) -> {
            if (v == null || v[0] != currentSecond) {
                return new long[]{currentSecond, 1};
            }
            v[1]++;
            return v;
        });

        if (counter[1] > MAX_QPS) {
            log.warn("用户 QPS 限流触发: userId={}, count={}", userId, counter[1]);
            throw new BizException(ErrorCode.AGENT_RATE_LIMITED,
                    "请求过于频繁，请稍后重试");
        }
    }

    private void checkDailyLimit(String userId) {
        long currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24);
        long[] counter = dailyCounters.compute(userId, (k, v) -> {
            if (v == null || v[0] != currentDay) {
                return new long[]{currentDay, 1};
            }
            v[1]++;
            return v;
        });

        if (counter[1] > MAX_DAILY_CALLS) {
            log.warn("用户日调用量限流触发: userId={}, count={}", userId, counter[1]);
            throw new BizException(ErrorCode.AGENT_RATE_LIMITED,
                    "今日调用次数已达上限，请明天再试");
        }
    }

    /**
     * 获取用户今日剩余调用次数（供前端展示）。
     */
    public int remainingDailyCalls(String userId) {
        long currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24);
        long[] counter = dailyCounters.get(userId);
        if (counter == null || counter[0] != currentDay) {
            return MAX_DAILY_CALLS;
        }
        return Math.max(0, MAX_DAILY_CALLS - (int) counter[1]);
    }
}
