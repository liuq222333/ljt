package com.example.demo.demosAdmin.Governance.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo.demosAdmin.Governance.Dao.GovernanceAdminStateEntryMapper;
import com.example.demo.demosAdmin.Governance.Dao.GovernanceAdminStateMapper;
import com.example.demo.demosAdmin.Governance.Entity.GovernanceAdminStateEntryEntity;
import com.example.demo.demosAdmin.Governance.Entity.GovernanceAdminStateEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GovernanceAdminStateStore {

    private static final Logger log = LoggerFactory.getLogger(GovernanceAdminStateStore.class);

    static final String DEFAULT_STORE_KEY = "admin_governance";

    private final GovernanceAdminStateMapper stateMapper;
    private final GovernanceAdminStateEntryMapper stateEntryMapper;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private volatile boolean schemaEnsured;

    public GovernanceAdminStateStore(GovernanceAdminStateMapper stateMapper,
                                     GovernanceAdminStateEntryMapper stateEntryMapper,
                                     ObjectMapper objectMapper,
                                     JdbcTemplate jdbcTemplate) {
        this.stateMapper = stateMapper;
        this.stateEntryMapper = stateEntryMapper;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        ensureTables();
    }

    public Map<String, Object> load(String storeKey) {
        ensureTables();
        try {
            GovernanceAdminStateEntity entity = stateMapper.selectOne(new LambdaQueryWrapper<GovernanceAdminStateEntity>()
                    .eq(GovernanceAdminStateEntity::getStoreKey, normalizeStoreKey(storeKey))
                    .last("limit 1"));
            if (entity == null || !StringUtils.hasText(entity.getPayloadJson())) {
                return null;
            }
            return objectMapper.readValue(entity.getPayloadJson(), new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (Exception ex) {
            log.warn("Load governance admin state failed, fallback to in-memory state: {}", ex.getMessage());
            return null;
        }
    }

    public void save(String storeKey, Map<String, Object> payload) {
        ensureTables();
        String normalizedKey = normalizeStoreKey(storeKey);
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload == null ? new LinkedHashMap<String, Object>() : payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize governance admin state", ex);
        }
        try {
            GovernanceAdminStateEntity entity = stateMapper.selectOne(new LambdaQueryWrapper<GovernanceAdminStateEntity>()
                    .eq(GovernanceAdminStateEntity::getStoreKey, normalizedKey)
                    .last("limit 1"));
            LocalDateTime now = LocalDateTime.now();
            if (entity == null) {
                entity = new GovernanceAdminStateEntity();
                entity.setStoreKey(normalizedKey);
                entity.setPayloadJson(payloadJson);
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                stateMapper.insert(entity);
                return;
            }
            entity.setPayloadJson(payloadJson);
            entity.setUpdatedAt(now);
            stateMapper.updateById(entity);
        } catch (Exception ex) {
            log.warn("Persist governance admin state failed, keep in-memory state only: {}", ex.getMessage());
        }
    }

    public Map<String, Map<String, Object>> loadStructured(String storeKey) {
        ensureTables();
        try {
            List<GovernanceAdminStateEntryEntity> entities = stateEntryMapper.selectList(
                    new LambdaQueryWrapper<GovernanceAdminStateEntryEntity>()
                            .eq(GovernanceAdminStateEntryEntity::getStoreKey, normalizeStoreKey(storeKey))
                            .orderByAsc(GovernanceAdminStateEntryEntity::getEntryType, GovernanceAdminStateEntryEntity::getEntryKey)
            );
            if (entities == null || entities.isEmpty()) {
                return new LinkedHashMap<String, Map<String, Object>>();
            }
            Map<String, Map<String, Object>> result = new LinkedHashMap<String, Map<String, Object>>();
            for (GovernanceAdminStateEntryEntity entity : entities) {
                if (!StringUtils.hasText(entity.getPayloadJson())) {
                    continue;
                }
                Map<String, Object> payload = objectMapper.readValue(
                        entity.getPayloadJson(),
                        new TypeReference<LinkedHashMap<String, Object>>() { }
                );
                Map<String, Object> bucket = result.get(entity.getEntryType());
                if (bucket == null) {
                    bucket = new LinkedHashMap<String, Object>();
                    result.put(entity.getEntryType(), bucket);
                }
                bucket.put(entity.getEntryKey(), payload);
            }
            return result;
        } catch (Exception ex) {
            log.warn("Load structured governance admin state failed: {}", ex.getMessage());
            return new LinkedHashMap<String, Map<String, Object>>();
        }
    }

    public void saveStructured(String storeKey, Map<String, Map<String, Object>> payloadByType) {
        ensureTables();
        String normalizedKey = normalizeStoreKey(storeKey);
        try {
            LocalDateTime now = LocalDateTime.now();
            List<String> entryTypes = new ArrayList<String>();
            if (payloadByType != null) {
                entryTypes.addAll(payloadByType.keySet());
            }
            if (!entryTypes.isEmpty()) {
                for (String entryType : entryTypes) {
                    stateEntryMapper.delete(new LambdaUpdateWrapper<GovernanceAdminStateEntryEntity>()
                            .eq(GovernanceAdminStateEntryEntity::getStoreKey, normalizedKey)
                            .eq(GovernanceAdminStateEntryEntity::getEntryType, entryType));
                    Map<String, Object> entries = payloadByType.get(entryType);
                    if (entries == null) {
                        continue;
                    }
                    for (Map.Entry<String, Object> entry : entries.entrySet()) {
                        GovernanceAdminStateEntryEntity entity = new GovernanceAdminStateEntryEntity();
                        entity.setStoreKey(normalizedKey);
                        entity.setEntryType(entryType);
                        entity.setEntryKey(entry.getKey());
                        entity.setPayloadJson(objectMapper.writeValueAsString(entry.getValue() == null
                                ? new LinkedHashMap<String, Object>()
                                : entry.getValue()));
                        entity.setCreatedAt(now);
                        entity.setUpdatedAt(now);
                        stateEntryMapper.insert(entity);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Persist structured governance admin state failed, fallback to snapshot only: {}", ex.getMessage());
        }
    }

    private String normalizeStoreKey(String storeKey) {
        return StringUtils.hasText(storeKey) ? storeKey.trim() : DEFAULT_STORE_KEY;
    }

    private synchronized void ensureTables() {
        if (schemaEnsured) {
            return;
        }
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS governance_admin_states ("
                    + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                    + "store_key VARCHAR(128) NOT NULL,"
                    + "payload_json LONGTEXT NOT NULL,"
                    + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                    + "UNIQUE KEY uk_governance_admin_states_store_key (store_key)"
                    + ")");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS governance_admin_state_entries ("
                    + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                    + "store_key VARCHAR(128) NOT NULL,"
                    + "entry_type VARCHAR(128) NOT NULL,"
                    + "entry_key VARCHAR(128) NOT NULL,"
                    + "payload_json LONGTEXT NOT NULL,"
                    + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                    + "UNIQUE KEY uk_governance_admin_state_entries_store_type_key (store_key, entry_type, entry_key),"
                    + "KEY idx_governance_admin_state_entries_store_type (store_key, entry_type)"
                    + ")");
            schemaEnsured = true;
        } catch (Exception ex) {
            log.warn("Ensure governance admin tables failed: {}", ex.getMessage());
        }
    }
}
