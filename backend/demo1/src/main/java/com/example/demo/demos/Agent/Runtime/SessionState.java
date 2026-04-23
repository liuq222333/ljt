package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.common.schema.FinalAnswer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class SessionState {

    private FinalAnswer finalAnswer = new FinalAnswer();
    private ExecutionMeta executionMeta = new ExecutionMeta();
    private SessionContext sessionContext = new SessionContext();
    private Map<String, Object> intermediateData = new LinkedHashMap<String, Object>();

    @Data
    public static class ExecutionMeta {
        private String requestId;
        private String traceId;
        private String sessionId;
        private List<String> completedNodes = new ArrayList<String>();
        private boolean degraded;
        private boolean restoredFromCheckpoint;
        private String errorCode;
        private String failedNode;
        private long durationMs;
        private LocalDateTime createdAt = LocalDateTime.now();
    }

    @Data
    public static class SessionContext {
        private Map<String, Object> pendingSlots = new LinkedHashMap<String, Object>();
        private Map<String, Object> confirmedConstraints = new LinkedHashMap<String, Object>();
        private List<String> candidateEntities = new ArrayList<String>();
        private String focusedEntityId;
        private List<String> lastSelectedEntityIds = new ArrayList<String>();
        private Integer dialogueTurnIndex = 0;
        private Map<String, Object> followUpContext = new LinkedHashMap<String, Object>();
    }
}
