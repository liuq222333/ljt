package com.example.demo.demos.common.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * 通用重试组件 — 支持配置重试次数、间隔、异常类型。
 * 与施工单 W01 公共组件对齐。
 */
public class RetryHelper {

    private static final Logger log = LoggerFactory.getLogger(RetryHelper.class);

    private RetryHelper() {
    }

    /**
     * 带重试执行任务。
     *
     * @param task          要执行的任务
     * @param maxRetries    最大重试次数（不含首次执行）
     * @param intervalMs    重试间隔（毫秒）
     * @param taskName      任务名称（用于日志）
     * @param <T>           返回类型
     * @return 任务结果
     * @throws RuntimeException 超过最大重试次数后抛出最后一次异常
     */
    public static <T> T execute(Supplier<T> task, int maxRetries, long intervalMs, String taskName) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return task.get();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    log.warn("[{}] 执行失败 (attempt {}/{}): {}，{}ms 后重试",
                            taskName, attempt + 1, maxRetries + 1, e.getMessage(), intervalMs);
                    if (intervalMs > 0) {
                        try {
                            Thread.sleep(intervalMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(taskName + " 重试被中断", ie);
                        }
                    }
                } else {
                    log.error("[{}] 执行失败，已耗尽重试次数 ({}/{}): {}",
                            taskName, attempt + 1, maxRetries + 1, e.getMessage());
                }
            }
        }

        throw new RuntimeException(taskName + " 超过最大重试次数", lastException);
    }

    /**
     * 带重试执行无返回值任务。
     */
    public static void executeVoid(Runnable task, int maxRetries, long intervalMs, String taskName) {
        execute(() -> {
            task.run();
            return null;
        }, maxRetries, intervalMs, taskName);
    }
}
