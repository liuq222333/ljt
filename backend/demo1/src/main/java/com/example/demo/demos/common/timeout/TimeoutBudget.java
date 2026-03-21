package com.example.demo.demos.common.timeout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 超时预算管理器 — 管理全链路超时和各节点的剩余时间预算。
 * 端到端最大超时 8s，每个节点执行前检查剩余预算，不足时跳过走降级。
 * 与施工单 W04 性能预算框架对齐，在 W01 先落基础实现。
 */
public class TimeoutBudget {

    private static final Logger log = LoggerFactory.getLogger(TimeoutBudget.class);

    /** 全链路默认最大超时（毫秒） */
    public static final long DEFAULT_TOTAL_BUDGET_MS = 8000;

    /** 请求开始时间 */
    private final long startTimeMs;

    /** 总预算（毫秒） */
    private final long totalBudgetMs;

    public TimeoutBudget() {
        this(DEFAULT_TOTAL_BUDGET_MS);
    }

    public TimeoutBudget(long totalBudgetMs) {
        this.startTimeMs = System.currentTimeMillis();
        this.totalBudgetMs = totalBudgetMs;
    }

    /**
     * 获取已消耗的时间（毫秒）。
     */
    public long elapsedMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    /**
     * 获取剩余预算（毫秒），最小为 0。
     */
    public long remainingMs() {
        return Math.max(0, totalBudgetMs - elapsedMs());
    }

    /**
     * 检查全链路是否已超时。
     */
    public boolean isExpired() {
        return remainingMs() <= 0;
    }

    /**
     * 检查剩余预算是否够执行指定节点。
     * 如果不够，日志告警并返回 false。
     *
     * @param nodeName  节点名称
     * @param requiredMs 节点所需预算（毫秒）
     * @return 是否有足够预算
     */
    public boolean hasEnoughBudget(String nodeName, long requiredMs) {
        long remaining = remainingMs();
        if (remaining < requiredMs) {
            log.warn("超时预算不足: 节点={}, 需要={}ms, 剩余={}ms, 已耗时={}ms，将跳过该节点走降级",
                    nodeName, requiredMs, remaining, elapsedMs());
            return false;
        }
        return true;
    }

    /**
     * 获取节点的实际可用超时时间。
     * 取节点配置超时和剩余预算中的较小值。
     *
     * @param nodeConfigTimeoutMs 节点配置的超时时间
     * @return 节点实际可用的超时时间
     */
    public long effectiveTimeoutMs(long nodeConfigTimeoutMs) {
        return Math.min(nodeConfigTimeoutMs, remainingMs());
    }

    public long getTotalBudgetMs() {
        return totalBudgetMs;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }
}
