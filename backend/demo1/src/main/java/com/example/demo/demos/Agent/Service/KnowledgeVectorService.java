package com.example.demo.demos.Agent.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.demos.Agent.Dao.KnowledgeBaseMapper;
import com.example.demo.demos.Agent.Dao.KnowledgeVectorMapper;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Entity.KnowledgeVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KnowledgeVectorService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeVectorService.class);

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeVectorMapper knowledgeVectorMapper;

    @Resource
    private EmbeddingService embeddingService;

    @Resource
    private KnowledgeService knowledgeService;

    public int generateMissingVectors() {
        return generateVectors(false);
    }

    public int generateVectors(boolean rebuildExisting) {
        log.info("Start knowledge vector generation, rebuildExisting={}", rebuildExisting);

        List<KnowledgeBase> allKnowledge = knowledgeBaseMapper.selectList(null);
        log.info("Knowledge rows loaded: {}", allKnowledge.size());

        List<KnowledgeVector> existingVectors = knowledgeVectorMapper.selectList(null);
        Set<Long> vectorizedIds = existingVectors.stream()
                .map(KnowledgeVector::getKnowledgeId)
                .collect(Collectors.toSet());
        log.info("Existing vector rows: {}", vectorizedIds.size());

        Set<Long> allKnowledgeIds = allKnowledge.stream()
                .map(KnowledgeBase::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> activeKnowledgeIds = allKnowledge.stream()
                .filter(this::shouldIndexKnowledge)
                .map(KnowledgeBase::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (rebuildExisting) {
            cleanupStaleVectors(vectorizedIds, activeKnowledgeIds, allKnowledgeIds);
        }

        List<KnowledgeBase> targetKnowledge = allKnowledge.stream()
                .filter(this::shouldIndexKnowledge)
                .filter(kb -> rebuildExisting || !vectorizedIds.contains(kb.getId()))
                .collect(Collectors.toList());
        log.info("Knowledge rows to vectorize: {}", targetKnowledge.size());

        int successCount = 0;
        for (KnowledgeBase knowledgeBase : targetKnowledge) {
            try {
                if (upsertVector(knowledgeBase, rebuildExisting)) {
                    successCount++;
                    log.info("Vector generated [{}/{}], knowledgeId={}",
                            successCount, targetKnowledge.size(), knowledgeBase.getId());
                }

                Thread.sleep(100L);
            } catch (Exception e) {
                log.error("Failed to generate vector, knowledgeId={}", knowledgeBase.getId(), e);
            }
        }

        log.info("Knowledge vector generation finished, success={}, failed={}",
                successCount, targetKnowledge.size() - successCount);
        return successCount;
    }

    public int rebuildVector(Long knowledgeId) {
        if (knowledgeId == null) {
            return 0;
        }
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeId);
        if (!shouldIndexKnowledge(knowledgeBase)) {
            deleteVectorByKnowledgeId(knowledgeId);
            return 0;
        }
        return upsertVector(knowledgeBase, true) ? 1 : 0;
    }

    public int deleteVectorByKnowledgeId(Long knowledgeId) {
        if (knowledgeId == null) {
            return 0;
        }
        QueryWrapper<KnowledgeVector> deleteWrapper = new QueryWrapper<KnowledgeVector>();
        deleteWrapper.eq("knowledge_id", knowledgeId);
        return knowledgeVectorMapper.delete(deleteWrapper);
    }

    public boolean shouldIndexKnowledge(KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null || knowledgeBase.getId() == null) {
            return false;
        }
        if (knowledgeBase.getStatus() == null || knowledgeBase.getStatus().intValue() != 1) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (knowledgeBase.getEffectiveFrom() != null && knowledgeBase.getEffectiveFrom().isAfter(now)) {
            return false;
        }
        if (knowledgeBase.getEffectiveTo() != null && knowledgeBase.getEffectiveTo().isBefore(now)) {
            return false;
        }
        return true;
    }

    public Map<String, Object> collectVectorStats() {
        List<KnowledgeBase> allKnowledge = knowledgeBaseMapper.selectList(null);
        List<KnowledgeVector> vectors = knowledgeVectorMapper.selectList(null);
        Set<Long> vectorIds = vectors.stream()
                .map(KnowledgeVector::getKnowledgeId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> activeIds = allKnowledge.stream()
                .filter(this::shouldIndexKnowledge)
                .map(KnowledgeBase::getId)
                .collect(Collectors.toSet());
        Set<Long> allIds = allKnowledge.stream()
                .map(KnowledgeBase::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Set<Long> missingActive = new HashSet<Long>(activeIds);
        missingActive.removeAll(vectorIds);
        Set<Long> staleVectors = new HashSet<Long>(vectorIds);
        staleVectors.removeAll(activeIds);

        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        stats.put("knowledge_total", allIds.size());
        stats.put("active_knowledge_total", activeIds.size());
        stats.put("vector_total", vectorIds.size());
        stats.put("missing_active_vector_total", missingActive.size());
        stats.put("stale_vector_total", staleVectors.size());
        stats.put("missing_active_vector_ids", missingActive.stream().sorted().limit(20).collect(Collectors.toList()));
        stats.put("stale_vector_ids", staleVectors.stream().sorted().limit(20).collect(Collectors.toList()));
        stats.put("consistent", missingActive.isEmpty() && staleVectors.isEmpty());
        return stats;
    }

    public Map<String, Object> collectVectorStats(String docType,
                                                  String entityType,
                                                  String entityId,
                                                  Integer status,
                                                  Integer limit) {
        QueryWrapper<KnowledgeBase> wrapper = new QueryWrapper<KnowledgeBase>();
        if (StringUtils.hasText(docType)) {
            wrapper.eq("doc_type", docType);
        }
        if (StringUtils.hasText(entityType)) {
            wrapper.eq("entity_type", entityType);
        }
        if (StringUtils.hasText(entityId)) {
            wrapper.eq("entity_id", entityId);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("updated_at").orderByAsc("id");
        int effectiveLimit = normalizeLimit(limit);
        if (effectiveLimit > 0) {
            wrapper.last("LIMIT " + effectiveLimit);
        }

        List<KnowledgeBase> scopedKnowledge = knowledgeBaseMapper.selectList(wrapper);
        List<KnowledgeVector> vectors = knowledgeVectorMapper.selectList(null);
        Set<Long> vectorIds = vectors.stream()
                .map(KnowledgeVector::getKnowledgeId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> scopedIds = scopedKnowledge.stream()
                .map(KnowledgeBase::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> activeIds = scopedKnowledge.stream()
                .filter(this::shouldIndexKnowledge)
                .map(KnowledgeBase::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> vectorIdsInScope = new HashSet<Long>(vectorIds);
        vectorIdsInScope.retainAll(scopedIds);

        Set<Long> missingActive = new HashSet<Long>(activeIds);
        missingActive.removeAll(vectorIdsInScope);
        Set<Long> staleVectors = new HashSet<Long>(vectorIdsInScope);
        staleVectors.removeAll(activeIds);

        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        stats.put("scope", "filtered_knowledge");
        stats.put("doc_type", docType);
        stats.put("entity_type", entityType);
        stats.put("entity_id", entityId);
        stats.put("status", status);
        stats.put("limit", effectiveLimit);
        stats.put("knowledge_total", scopedIds.size());
        stats.put("active_knowledge_total", activeIds.size());
        stats.put("vector_total", vectorIdsInScope.size());
        stats.put("missing_active_vector_total", missingActive.size());
        stats.put("stale_vector_total", staleVectors.size());
        stats.put("missing_active_vector_ids", sampleIds(missingActive));
        stats.put("stale_vector_ids", sampleIds(staleVectors));
        stats.put("detail_records", buildKnowledgeDetailRecords(scopedKnowledge, vectorIdsInScope));
        stats.put("consistent", missingActive.isEmpty() && staleVectors.isEmpty());
        return stats;
    }

    private void cleanupStaleVectors(Set<Long> vectorizedIds, Set<Long> activeKnowledgeIds, Set<Long> allKnowledgeIds) {
        Set<Long> staleIds = new HashSet<Long>(vectorizedIds);
        staleIds.removeAll(activeKnowledgeIds);
        if (staleIds.isEmpty()) {
            return;
        }
        for (Long staleId : staleIds) {
            int deleted = deleteVectorByKnowledgeId(staleId);
            if (deleted > 0) {
                String reason = allKnowledgeIds.contains(staleId) ? "inactive_or_expired" : "orphan";
                log.info("Deleted stale knowledge vector, knowledgeId={}, reason={}", staleId, reason);
            }
        }
    }

    private boolean upsertVector(KnowledgeBase knowledgeBase, boolean replaceExisting) {
        try {
            String textToEmbed = knowledgeService.buildEmbeddingText(knowledgeBase);
            if (!StringUtils.hasText(textToEmbed)) {
                log.warn("Skip vector generation because embedding text is empty, knowledgeId={}",
                        knowledgeBase.getId());
                return false;
            }

            float[] vector = embeddingService.embed(textToEmbed);
            String vectorJson = floatArrayToJson(vector);

            if (replaceExisting) {
                deleteVectorByKnowledgeId(knowledgeBase.getId());
            }

            KnowledgeVector vectorEntity = new KnowledgeVector();
            vectorEntity.setKnowledgeId(knowledgeBase.getId());
            vectorEntity.setVectorData(vectorJson);
            vectorEntity.setCreatedAt(LocalDateTime.now());
            knowledgeVectorMapper.insert(vectorEntity);
            return true;
        } catch (Exception e) {
            log.error("Failed to generate vector, knowledgeId={}", knowledgeBase.getId(), e);
            return false;
        }
    }

    private String floatArrayToJson(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private List<Long> sampleIds(Set<Long> values) {
        List<Long> ids = new ArrayList<Long>(values);
        Collections.sort(ids);
        if (ids.size() > 20) {
            return new ArrayList<Long>(ids.subList(0, 20));
        }
        return ids;
    }

    private List<Map<String, Object>> buildKnowledgeDetailRecords(List<KnowledgeBase> scopedKnowledge,
                                                                  Set<Long> vectorIdsInScope) {
        List<Map<String, Object>> details = new ArrayList<Map<String, Object>>();
        for (KnowledgeBase row : scopedKnowledge) {
            if (row == null || row.getId() == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("knowledge_id", row.getId());
            item.put("doc_type", row.getDocType());
            item.put("entity_type", row.getEntityType());
            item.put("entity_id", row.getEntityId());
            item.put("status", row.getStatus());
            item.put("should_index", shouldIndexKnowledge(row));
            item.put("vector_present", vectorIdsInScope.contains(row.getId()));
            item.put("updated_at", row.getUpdatedAt());
            details.add(item);
            if (details.size() >= 20) {
                break;
            }
        }
        return details;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit.intValue() <= 0) {
            return 100;
        }
        return Math.min(limit.intValue(), 200);
    }
}
