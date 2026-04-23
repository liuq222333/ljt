package com.example.demo.demos.governance.replay;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Runtime.SessionState;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GovernanceReplayStore {

    private static final Logger log = LoggerFactory.getLogger(GovernanceReplayStore.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private GovernanceReplayRecordMapper replayRecordMapper;

    @Autowired(required = false)
    private GovernanceReplayCheckpointMapper replayCheckpointMapper;

    @Autowired(required = false)
    private GovernanceReplayToolIoMapper replayToolIoMapper;

    private final Map<String, GovernanceReplayRecordEntity> memoryByRequestId =
            new ConcurrentHashMap<String, GovernanceReplayRecordEntity>();

    public void record(AgentChatRequest request, SessionState state) {
        if (state == null || state.getExecutionMeta() == null || !StringUtils.hasText(state.getExecutionMeta().getRequestId())) {
            return;
        }
        GovernanceReplayRecordEntity entity = new GovernanceReplayRecordEntity();
        entity.setRequestId(state.getExecutionMeta().getRequestId());
        entity.setTraceId(state.getExecutionMeta().getTraceId());
        entity.setSessionId(state.getExecutionMeta().getSessionId());
        entity.setUserId(request == null ? null : request.getUserId());
        entity.setLastUserMessage(extractLastUserMessage(request));
        entity.setAnswerType(state.getFinalAnswer() == null || state.getFinalAnswer().getAnswerType() == null
                ? null : state.getFinalAnswer().getAnswerType().name());
        entity.setErrorCode(state.getExecutionMeta().getErrorCode());
        entity.setFailedNode(state.getExecutionMeta().getFailedNode());
        entity.setDegraded(state.getExecutionMeta().isDegraded() ? 1 : 0);
        entity.setDurationMs(state.getExecutionMeta().getDurationMs());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setRequestJson(writeJson(request));
        entity.setStateJson(writeJson(state));

        try {
            if (replayRecordMapper != null) {
                replayRecordMapper.insert(entity);
            }
        } catch (Exception ex) {
            log.warn("Persist replay record failed, fallback to memory store: {}", ex.getMessage());
        }
        memoryByRequestId.put(entity.getRequestId(), entity);
    }

    public List<GovernanceReplayRecordEntity> listRecent(String keyword, int limit) {
        int safeLimit = limit <= 0 ? 20 : limit;
        try {
            if (replayRecordMapper != null) {
                LambdaQueryWrapper<GovernanceReplayRecordEntity> wrapper =
                        new LambdaQueryWrapper<GovernanceReplayRecordEntity>()
                                .orderByDesc(GovernanceReplayRecordEntity::getCreatedAt)
                                .last("limit " + safeLimit);
                if (StringUtils.hasText(keyword)) {
                    wrapper.and(w -> w.like(GovernanceReplayRecordEntity::getRequestId, keyword)
                            .or().like(GovernanceReplayRecordEntity::getTraceId, keyword)
                            .or().like(GovernanceReplayRecordEntity::getSessionId, keyword)
                            .or().like(GovernanceReplayRecordEntity::getLastUserMessage, keyword));
                }
                return replayRecordMapper.selectList(wrapper);
            }
        } catch (Exception ex) {
            log.warn("Load replay list from database failed: {}", ex.getMessage());
        }
        List<GovernanceReplayRecordEntity> items = new ArrayList<GovernanceReplayRecordEntity>(memoryByRequestId.values());
        items.sort((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()));
        return items.subList(0, Math.min(safeLimit, items.size()));
    }

    public GovernanceReplayRecordEntity findByRequestId(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return null;
        }
        try {
            if (replayRecordMapper != null) {
                return replayRecordMapper.selectOne(new LambdaQueryWrapper<GovernanceReplayRecordEntity>()
                        .eq(GovernanceReplayRecordEntity::getRequestId, requestId)
                        .last("limit 1"));
            }
        } catch (Exception ex) {
            log.warn("Load replay by requestId failed: {}", ex.getMessage());
        }
        return memoryByRequestId.get(requestId);
    }

    public GovernanceReplayRecordEntity findByTraceId(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return null;
        }
        try {
            if (replayRecordMapper != null) {
                return replayRecordMapper.selectOne(new LambdaQueryWrapper<GovernanceReplayRecordEntity>()
                        .eq(GovernanceReplayRecordEntity::getTraceId, traceId)
                        .orderByDesc(GovernanceReplayRecordEntity::getCreatedAt)
                        .last("limit 1"));
            }
        } catch (Exception ex) {
            log.warn("Load replay by traceId failed: {}", ex.getMessage());
        }
        for (GovernanceReplayRecordEntity entity : memoryByRequestId.values()) {
            if (traceId.equals(entity.getTraceId())) {
                return entity;
            }
        }
        return null;
    }

    public GovernanceReplayRecordEntity findLatestBySessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        try {
            if (replayRecordMapper != null) {
                return replayRecordMapper.selectOne(new LambdaQueryWrapper<GovernanceReplayRecordEntity>()
                        .eq(GovernanceReplayRecordEntity::getSessionId, sessionId)
                        .orderByDesc(GovernanceReplayRecordEntity::getCreatedAt)
                        .last("limit 1"));
            }
        } catch (Exception ex) {
            log.warn("Load replay by sessionId failed: {}", ex.getMessage());
        }
        GovernanceReplayRecordEntity latest = null;
        for (GovernanceReplayRecordEntity entity : memoryByRequestId.values()) {
            if (!sessionId.equals(entity.getSessionId())) {
                continue;
            }
            if (latest == null || entity.getCreatedAt().isAfter(latest.getCreatedAt())) {
                latest = entity;
            }
        }
        return latest;
    }

    public List<GovernanceReplayRecordEntity> listSessionHistory(String sessionId, int limit) {
        if (!StringUtils.hasText(sessionId)) {
            return Collections.emptyList();
        }
        try {
            if (replayRecordMapper != null) {
                return replayRecordMapper.selectList(new LambdaQueryWrapper<GovernanceReplayRecordEntity>()
                        .eq(GovernanceReplayRecordEntity::getSessionId, sessionId)
                        .orderByDesc(GovernanceReplayRecordEntity::getCreatedAt)
                        .last("limit " + Math.max(limit, 20)));
            }
        } catch (Exception ex) {
            log.warn("Load replay session history failed: {}", ex.getMessage());
        }
        List<GovernanceReplayRecordEntity> items = new ArrayList<GovernanceReplayRecordEntity>();
        for (GovernanceReplayRecordEntity entity : memoryByRequestId.values()) {
            if (sessionId.equals(entity.getSessionId())) {
                items.add(entity);
            }
        }
        items.sort((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()));
        return items;
    }

    public List<GovernanceReplayCheckpointEntity> loadCheckpoints(Long replayRecordId) {
        if (replayRecordId == null || replayCheckpointMapper == null) {
            return Collections.emptyList();
        }
        try {
            return replayCheckpointMapper.selectList(new LambdaQueryWrapper<GovernanceReplayCheckpointEntity>()
                    .eq(GovernanceReplayCheckpointEntity::getReplayRecordId, replayRecordId)
                    .orderByAsc(GovernanceReplayCheckpointEntity::getCheckpointOrder));
        } catch (Exception ex) {
            log.warn("Load replay checkpoints failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    public List<GovernanceReplayToolIoEntity> loadToolIos(Long replayRecordId) {
        if (replayRecordId == null || replayToolIoMapper == null) {
            return Collections.emptyList();
        }
        try {
            return replayToolIoMapper.selectList(new LambdaQueryWrapper<GovernanceReplayToolIoEntity>()
                    .eq(GovernanceReplayToolIoEntity::getReplayRecordId, replayRecordId)
                    .orderByAsc(GovernanceReplayToolIoEntity::getStepOrder));
        } catch (Exception ex) {
            log.warn("Load replay tool I/O failed: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> readJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<String, Object>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (Exception ex) {
            Map<String, Object> fallback = new LinkedHashMap<String, Object>();
            fallback.put("raw", json);
            return fallback;
        }
    }

    private String extractLastUserMessage(AgentChatRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.getMessages())) {
            return null;
        }
        for (int index = request.getMessages().size() - 1; index >= 0; index--) {
            AgentChatMessage message = request.getMessages().get(index);
            if (message != null && StringUtils.hasText(message.getContent())) {
                return message.getContent();
            }
        }
        return null;
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }
}
