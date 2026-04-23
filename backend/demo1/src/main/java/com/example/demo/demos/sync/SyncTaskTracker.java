package com.example.demo.demos.sync;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SyncTaskTracker {

    private final AtomicLong sequence = new AtomicLong(1000L);
    private final Map<Long, Map<String, Object>> tasks = new ConcurrentHashMap<Long, Map<String, Object>>();
    private final List<Map<String, Object>> deadLetters = new CopyOnWriteArrayList<Map<String, Object>>();

    public long start(String taskType, Map<String, Object> payload) {
        long taskId = sequence.incrementAndGet();
        Map<String, Object> task = new LinkedHashMap<String, Object>();
        task.put("taskId", taskId);
        task.put("taskType", taskType);
        task.put("status", "RUNNING");
        task.put("payload", payload == null ? Collections.emptyMap() : payload);
        task.put("startedAt", LocalDateTime.now());
        tasks.put(taskId, task);
        return taskId;
    }

    public Map<String, Object> complete(long taskId, Map<String, Object> result) {
        Map<String, Object> task = tasks.get(taskId);
        if (task == null) {
            return Collections.emptyMap();
        }
        task.put("status", "SUCCESS");
        task.put("finishedAt", LocalDateTime.now());
        task.put("result", result == null ? Collections.emptyMap() : result);
        return task;
    }

    public Map<String, Object> fail(long taskId, String errorMessage) {
        Map<String, Object> task = tasks.get(taskId);
        if (task == null) {
            return Collections.emptyMap();
        }
        task.put("status", "FAILED");
        task.put("finishedAt", LocalDateTime.now());
        task.put("errorMessage", errorMessage);
        deadLetters.add(new LinkedHashMap<String, Object>(task));
        return task;
    }

    public List<Map<String, Object>> listTasks() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(tasks.values());
        result.sort((left, right) -> Long.compare(asLong(right.get("taskId")), asLong(left.get("taskId"))));
        return result;
    }

    public Map<String, Object> getTask(long taskId) {
        Map<String, Object> task = tasks.get(taskId);
        return task == null ? Collections.emptyMap() : new LinkedHashMap<String, Object>(task);
    }

    public List<Map<String, Object>> listDeadLetters() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(deadLetters);
        result.sort((left, right) -> Long.compare(asLong(right.get("taskId")), asLong(left.get("taskId"))));
        return result;
    }

    public Map<String, Object> summary() {
        int running = 0;
        int failed = 0;
        int success = 0;
        for (Map<String, Object> task : tasks.values()) {
            String status = String.valueOf(task.get("status"));
            if ("RUNNING".equalsIgnoreCase(status)) {
                running++;
            } else if ("FAILED".equalsIgnoreCase(status)) {
                failed++;
            } else if ("SUCCESS".equalsIgnoreCase(status)) {
                success++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", tasks.size());
        result.put("running", running);
        result.put("failed", failed);
        result.put("success", success);
        result.put("deadLetterTotal", deadLetters.size());
        return result;
    }

    private long asLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
