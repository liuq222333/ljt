package com.example.demo.demos.Agent.Runtime;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class AgentCheckpointStore {

    private final Map<String, List<Checkpoint>> checkpointsBySession =
            new ConcurrentHashMap<String, List<Checkpoint>>();

    public void record(String sessionId, SessionState state) {
        if (!StringUtils.hasText(sessionId) || state == null) {
            return;
        }
        checkpointsBySession
                .computeIfAbsent(sessionId, key -> new CopyOnWriteArrayList<Checkpoint>())
                .add(Checkpoint.fromState(state));
    }

    public SessionState.SessionContext latestSessionContext(String sessionId) {
        Checkpoint checkpoint = latestCheckpoint(sessionId);
        return checkpoint == null ? null : checkpoint.copySessionContext();
    }

    public Map<String, Object> latestIntermediateData(String sessionId) {
        Checkpoint checkpoint = latestCheckpoint(sessionId);
        return checkpoint == null ? new LinkedHashMap<String, Object>() : checkpoint.copyIntermediateData();
    }

    public Integer count(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return 0;
        }
        List<Checkpoint> checkpoints = checkpointsBySession.get(sessionId);
        return checkpoints == null ? 0 : checkpoints.size();
    }

    public void clear(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        checkpointsBySession.remove(sessionId);
    }

    private Checkpoint latestCheckpoint(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        List<Checkpoint> checkpoints = checkpointsBySession.get(sessionId);
        if (checkpoints == null || checkpoints.isEmpty()) {
            return null;
        }
        return checkpoints.get(checkpoints.size() - 1);
    }

    private static final class Checkpoint {
        private final SessionState.SessionContext sessionContext;
        private final Map<String, Object> intermediateData;

        private Checkpoint(SessionState.SessionContext sessionContext, Map<String, Object> intermediateData) {
            this.sessionContext = sessionContext;
            this.intermediateData = intermediateData;
        }

        private static Checkpoint fromState(SessionState state) {
            SessionState.SessionContext copiedContext = copySessionContext(state.getSessionContext());
            Map<String, Object> copiedIntermediateData = state.getIntermediateData() == null
                    ? new LinkedHashMap<String, Object>()
                    : new LinkedHashMap<String, Object>(state.getIntermediateData());
            return new Checkpoint(copiedContext, copiedIntermediateData);
        }

        private SessionState.SessionContext copySessionContext() {
            return copySessionContext(sessionContext);
        }

        private Map<String, Object> copyIntermediateData() {
            return intermediateData == null
                    ? new LinkedHashMap<String, Object>()
                    : new LinkedHashMap<String, Object>(intermediateData);
        }

        private static SessionState.SessionContext copySessionContext(SessionState.SessionContext source) {
            SessionState.SessionContext target = new SessionState.SessionContext();
            if (source == null) {
                return target;
            }
            target.setPendingSlots(source.getPendingSlots() == null
                    ? new LinkedHashMap<String, Object>()
                    : new LinkedHashMap<String, Object>(source.getPendingSlots()));
            target.setConfirmedConstraints(source.getConfirmedConstraints() == null
                    ? new LinkedHashMap<String, Object>()
                    : new LinkedHashMap<String, Object>(source.getConfirmedConstraints()));
            target.setCandidateEntities(source.getCandidateEntities() == null
                    ? new ArrayList<String>()
                    : new ArrayList<String>(source.getCandidateEntities()));
            target.setFocusedEntityId(source.getFocusedEntityId());
            target.setLastSelectedEntityIds(source.getLastSelectedEntityIds() == null
                    ? new ArrayList<String>()
                    : new ArrayList<String>(source.getLastSelectedEntityIds()));
            target.setDialogueTurnIndex(source.getDialogueTurnIndex());
            target.setFollowUpContext(source.getFollowUpContext() == null
                    ? new LinkedHashMap<String, Object>()
                    : new LinkedHashMap<String, Object>(source.getFollowUpContext()));
            return target;
        }
    }
}
