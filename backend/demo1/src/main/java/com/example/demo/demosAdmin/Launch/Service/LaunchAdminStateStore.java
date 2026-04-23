package com.example.demo.demosAdmin.Launch.Service;

import com.example.demo.demosAdmin.Governance.Service.GovernanceAdminStateStore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LaunchAdminStateStore {

    private static final String STORE_KEY = "admin_launch";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GovernanceAdminStateStore governanceAdminStateStore;

    public LaunchAdminStateStore(GovernanceAdminStateStore governanceAdminStateStore) {
        this.governanceAdminStateStore = governanceAdminStateStore;
    }

    public Map<String, Object> saveRecord(String entryType, String idPrefix, Map<String, Object> payload) {
        Map<String, Map<String, Object>> structured = governanceAdminStateStore.loadStructured(STORE_KEY);
        Map<String, Object> bucket = bucket(structured, entryType);
        String now = now();
        String recordId = normalizeRecordId(payload == null ? null : payload.get("id"), idPrefix);
        Map<String, Object> previous = asMap(bucket.get(recordId));
        Map<String, Object> merged = new LinkedHashMap<String, Object>(previous);
        if (payload != null) {
            merged.putAll(payload);
        }
        merged.put("id", recordId);
        merged.put("recordType", entryType);
        merged.put("createdAt", defaultIfBlank(str(previous.get("createdAt")), now));
        merged.put("updatedAt", now);
        bucket.put(recordId, merged);
        governanceAdminStateStore.saveStructured(STORE_KEY, structured);
        return merged;
    }

    public List<Map<String, Object>> listRecords(String entryType, int limit) {
        Map<String, Map<String, Object>> structured = governanceAdminStateStore.loadStructured(STORE_KEY);
        Map<String, Object> bucket = bucket(structured, entryType);
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (Object value : bucket.values()) {
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> record = new LinkedHashMap<String, Object>((Map<String, Object>) value);
                records.add(record);
            }
        }
        Collections.sort(records, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                return sortValue(right).compareTo(sortValue(left));
            }
        });
        if (limit > 0 && records.size() > limit) {
            return new ArrayList<Map<String, Object>>(records.subList(0, limit));
        }
        return records;
    }

    public Map<String, Object> latestRecord(String entryType) {
        List<Map<String, Object>> records = listRecords(entryType, 1);
        return records.isEmpty() ? Collections.<String, Object>emptyMap() : records.get(0);
    }

    public void deleteRecord(String entryType, String id) {
        if (!StringUtils.hasText(id)) {
            return;
        }
        Map<String, Map<String, Object>> structured = governanceAdminStateStore.loadStructured(STORE_KEY);
        Map<String, Object> bucket = bucket(structured, entryType);
        bucket.remove(id.trim());
        governanceAdminStateStore.saveStructured(STORE_KEY, structured);
    }

    private Map<String, Object> bucket(Map<String, Map<String, Object>> structured, String entryType) {
        Map<String, Object> bucket = structured.get(entryType);
        if (bucket == null) {
            bucket = new LinkedHashMap<String, Object>();
            structured.put(entryType, bucket);
        }
        return bucket;
    }

    private String normalizeRecordId(Object value, String prefix) {
        if (value != null) {
            String text = String.valueOf(value).trim();
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return prefix + "-" + System.currentTimeMillis();
    }

    private String sortValue(Map<String, Object> record) {
        String updatedAt = str(record.get("updatedAt"));
        return StringUtils.hasText(updatedAt) ? updatedAt : defaultIfBlank(str(record.get("createdAt")), "");
    }

    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.<String, Object>emptyMap();
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
