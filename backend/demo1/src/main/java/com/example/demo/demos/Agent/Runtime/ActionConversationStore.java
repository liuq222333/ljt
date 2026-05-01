package com.example.demo.demos.Agent.Runtime;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActionConversationStore {

    private static final Duration PENDING_TTL = Duration.ofMinutes(30);

    private final Map<String, PendingAction> pendingActions = new ConcurrentHashMap<String, PendingAction>();

    public PendingAction get(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        PendingAction pendingAction = pendingActions.get(sessionId);
        if (pendingAction != null && isExpired(pendingAction)) {
            pendingActions.remove(sessionId);
            return null;
        }
        return pendingAction;
    }

    public void put(String sessionId, PendingAction pendingAction) {
        if (!StringUtils.hasText(sessionId) || pendingAction == null) {
            return;
        }
        pendingAction.setUpdatedAt(LocalDateTime.now());
        pendingActions.put(sessionId, pendingAction);
    }

    public void clear(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        pendingActions.remove(sessionId);
    }

    private boolean isExpired(PendingAction pendingAction) {
        LocalDateTime updatedAt = pendingAction.getUpdatedAt();
        if (updatedAt == null) {
            updatedAt = pendingAction.getCreatedAt();
        }
        return updatedAt != null && updatedAt.plus(PENDING_TTL).isBefore(LocalDateTime.now());
    }

    @Data
    public static class PendingAction {
        private String sessionId;
        private String resource;
        private String action;
        private String operationType;
        private String displayName;
        private String routeDescription;
        private Long routeId;
        private Map<String, Object> params = new LinkedHashMap<String, Object>();
        private Map<String, Object> payload = new LinkedHashMap<String, Object>();
        private List<String> missingFields = new ArrayList<String>();
        private List<String> routeKeywords = new ArrayList<String>();
        private boolean awaitingConfirmation;
        private String originalText;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();

        public PendingAction copy() {
            PendingAction copied = new PendingAction();
            copied.setSessionId(sessionId);
            copied.setResource(resource);
            copied.setAction(action);
            copied.setOperationType(operationType);
            copied.setDisplayName(displayName);
            copied.setRouteDescription(routeDescription);
            copied.setRouteId(routeId);
            copied.setParams(new LinkedHashMap<String, Object>(params));
            copied.setPayload(new LinkedHashMap<String, Object>(payload));
            copied.setMissingFields(new ArrayList<String>(missingFields));
            copied.setRouteKeywords(new ArrayList<String>(routeKeywords));
            copied.setAwaitingConfirmation(awaitingConfirmation);
            copied.setOriginalText(originalText);
            copied.setCreatedAt(createdAt);
            copied.setUpdatedAt(updatedAt);
            return copied;
        }
    }
}
