package com.example.demo.demos.Agent.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.demos.Agent.Dao.KnowledgeBaseMapper;
import com.example.demo.demos.Agent.Dao.KnowledgeChunkMapper;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Entity.KnowledgeChunk;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgeChunkService {

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeChunkMapper knowledgeChunkMapper;

    @Resource
    private KnowledgeService knowledgeService;

    public int generateChunks(boolean rebuildExisting) {
        List<KnowledgeBase> allKnowledge = knowledgeBaseMapper.selectList(null);
        int successCount = 0;
        for (KnowledgeBase knowledgeBase : allKnowledge) {
            if (!knowledgeService.isKnowledgeActiveForIndex(knowledgeBase, LocalDateTime.now())) {
                deleteChunksByKnowledgeId(knowledgeBase == null ? null : knowledgeBase.getId());
                continue;
            }
            if (rebuildExisting || countChunksByKnowledgeId(knowledgeBase.getId()) == 0) {
                successCount += rebuildChunks(knowledgeBase.getId());
            }
        }
        return successCount;
    }

    public int rebuildChunks(Long knowledgeId) {
        if (knowledgeId == null) {
            return 0;
        }
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeId);
        if (!knowledgeService.isKnowledgeActiveForIndex(knowledgeBase, LocalDateTime.now())) {
            deleteChunksByKnowledgeId(knowledgeId);
            return 0;
        }
        deleteChunksByKnowledgeId(knowledgeId);
        List<KnowledgeChunk> chunks = buildChunks(knowledgeBase);
        for (KnowledgeChunk chunk : chunks) {
            knowledgeChunkMapper.insert(chunk);
        }
        return chunks.size();
    }

    public int deleteChunksByKnowledgeId(Long knowledgeId) {
        if (knowledgeId == null) {
            return 0;
        }
        QueryWrapper<KnowledgeChunk> wrapper = new QueryWrapper<KnowledgeChunk>();
        wrapper.eq("knowledge_id", knowledgeId);
        return knowledgeChunkMapper.delete(wrapper);
    }

    public int countChunksByKnowledgeId(Long knowledgeId) {
        if (knowledgeId == null) {
            return 0;
        }
        QueryWrapper<KnowledgeChunk> wrapper = new QueryWrapper<KnowledgeChunk>();
        wrapper.eq("knowledge_id", knowledgeId);
        return knowledgeChunkMapper.selectCount(wrapper).intValue();
    }

    public Map<Long, Double> collectChunkScores(String queryText,
                                                KnowledgeRetrievalRequest request,
                                                List<String> docTypes,
                                                int limit,
                                                LocalDateTime timeContext) {
        if (!StringUtils.hasText(queryText)) {
            return Collections.emptyMap();
        }
        QueryWrapper<KnowledgeChunk> wrapper = new QueryWrapper<KnowledgeChunk>();
        wrapper.eq("status", 1)
                .like("chunk_text", queryText)
                .orderByAsc("knowledge_id")
                .orderByAsc("chunk_no")
                .last("limit " + Math.max(limit, 20));
        List<KnowledgeChunk> chunks = knowledgeChunkMapper.selectList(wrapper);
        Map<Long, Double> scores = new LinkedHashMap<Long, Double>();
        for (KnowledgeChunk chunk : chunks) {
            if (chunk == null || chunk.getKnowledgeId() == null || !StringUtils.hasText(chunk.getChunkText())) {
                continue;
            }
            KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(chunk.getKnowledgeId());
            if (!knowledgeService.matchesKnowledgeFilters(knowledgeBase, request, docTypes, timeContext)) {
                continue;
            }
            double score = computeChunkScore(chunk, queryText);
            Double existing = scores.get(chunk.getKnowledgeId());
            if (existing == null || score > existing.doubleValue()) {
                scores.put(chunk.getKnowledgeId(), score);
            }
        }
        return scores;
    }

    public Map<String, Object> collectChunkStats() {
        return collectChunkStats(null, null, null, null, null);
    }

    public Map<String, Object> collectChunkStats(String docType,
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
        if (limit != null && limit.intValue() > 0) {
            wrapper.last("limit " + Math.min(limit.intValue(), 200));
        }
        List<KnowledgeBase> scopedKnowledge = knowledgeBaseMapper.selectList(wrapper);
        Set<Long> scopedIds = new LinkedHashSet<Long>();
        Set<Long> activeIds = new LinkedHashSet<Long>();
        for (KnowledgeBase knowledgeBase : scopedKnowledge) {
            if (knowledgeBase != null && knowledgeBase.getId() != null) {
                scopedIds.add(knowledgeBase.getId());
                if (knowledgeService.isKnowledgeActiveForIndex(knowledgeBase, LocalDateTime.now())) {
                    activeIds.add(knowledgeBase.getId());
                }
            }
        }

        QueryWrapper<KnowledgeChunk> chunkWrapper = new QueryWrapper<KnowledgeChunk>();
        chunkWrapper.eq("status", 1);
        List<KnowledgeChunk> allChunks = knowledgeChunkMapper.selectList(chunkWrapper);
        Set<Long> chunkKnowledgeIds = new LinkedHashSet<Long>();
        int chunkTotal = 0;
        for (KnowledgeChunk chunk : allChunks) {
            if (chunk != null && chunk.getKnowledgeId() != null && scopedIds.contains(chunk.getKnowledgeId())) {
                chunkKnowledgeIds.add(chunk.getKnowledgeId());
                chunkTotal++;
            }
        }

        Set<Long> missingChunkIds = new LinkedHashSet<Long>(activeIds);
        missingChunkIds.removeAll(chunkKnowledgeIds);
        Set<Long> staleChunkIds = new LinkedHashSet<Long>(chunkKnowledgeIds);
        staleChunkIds.removeAll(activeIds);

        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        stats.put("knowledge_total", scopedIds.size());
        stats.put("active_knowledge_total", activeIds.size());
        stats.put("chunk_total", chunkTotal);
        stats.put("chunk_knowledge_total", chunkKnowledgeIds.size());
        stats.put("missing_chunk_knowledge_ids", sampleIds(missingChunkIds));
        stats.put("stale_chunk_knowledge_ids", sampleIds(staleChunkIds));
        stats.put("detail_records", buildChunkDetailRecords(scopedKnowledge, chunkKnowledgeIds, chunkTotal));
        stats.put("consistent", missingChunkIds.isEmpty() && staleChunkIds.isEmpty());
        return stats;
    }

    private List<KnowledgeChunk> buildChunks(KnowledgeBase knowledgeBase) {
        List<KnowledgeChunk> chunks = new ArrayList<KnowledgeChunk>();
        List<String> segments = new ArrayList<String>();
        if (StringUtils.hasText(knowledgeBase.getTitle())) {
            segments.add(knowledgeBase.getTitle());
        }
        if (StringUtils.hasText(knowledgeBase.getSummary())) {
            segments.add(knowledgeBase.getSummary());
        }
        if (StringUtils.hasText(knowledgeBase.getRelatedQuestions())) {
            segments.addAll(splitRelatedQuestions(knowledgeBase.getRelatedQuestions()));
        }
        if (StringUtils.hasText(knowledgeBase.getContent())) {
            segments.addAll(splitContent(knowledgeBase.getContent()));
        }

        int chunkNo = 1;
        for (String segment : segments) {
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setKnowledgeId(knowledgeBase.getId());
            chunk.setChunkNo(chunkNo++);
            chunk.setChunkType(resolveChunkType(knowledgeBase.getDocType(), segment));
            chunk.setChunkText(segment.trim());
            chunk.setMetadataJson(buildChunkMetadata(knowledgeBase));
            chunk.setStatus(1);
            chunk.setCreatedAt(LocalDateTime.now());
            chunk.setUpdatedAt(LocalDateTime.now());
            chunks.add(chunk);
        }
        return chunks;
    }

    private List<String> splitRelatedQuestions(String relatedQuestions) {
        List<String> segments = new ArrayList<String>();
        String normalized = relatedQuestions.replace("[", "")
                .replace("]", "")
                .replace("\"", "");
        for (String part : normalized.split("[,\\r\\n]+")) {
            if (StringUtils.hasText(part)) {
                segments.add(part.trim());
            }
        }
        return segments;
    }

    private List<String> splitContent(String content) {
        List<String> segments = new ArrayList<String>();
        String normalized = content.replace("\r\n", "\n");
        for (String paragraph : normalized.split("\\n\\s*\\n")) {
            if (!StringUtils.hasText(paragraph)) {
                continue;
            }
            String trimmed = paragraph.trim();
            if (trimmed.length() <= 220) {
                segments.add(trimmed);
                continue;
            }
            int start = 0;
            while (start < trimmed.length()) {
                int end = Math.min(trimmed.length(), start + 220);
                segments.add(trimmed.substring(start, end));
                start = end;
            }
        }
        return segments;
    }

    private String resolveChunkType(String docType, String segment) {
        String loweredDocType = StringUtils.hasText(docType) ? docType.toLowerCase(Locale.ROOT) : "";
        if ("faq".equals(loweredDocType)) {
            return "faq_pair";
        }
        if (segment.contains("退款") || segment.contains("预约") || segment.contains("规则")) {
            return "rule_clause";
        }
        return "paragraph";
    }

    private String buildChunkMetadata(KnowledgeBase knowledgeBase) {
        return "{\"docType\":\"" + safeJson(knowledgeBase.getDocType())
                + "\",\"entityType\":\"" + safeJson(knowledgeBase.getEntityType())
                + "\",\"entityId\":\"" + safeJson(knowledgeBase.getEntityId()) + "\"}";
    }

    private String safeJson(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private double computeChunkScore(KnowledgeChunk chunk, String queryText) {
        String loweredChunk = chunk.getChunkText().toLowerCase(Locale.ROOT);
        String loweredQuery = queryText.toLowerCase(Locale.ROOT);
        double score = loweredChunk.contains(loweredQuery) ? 0.70D : 0.0D;
        for (String token : queryText.split("\\s+")) {
            if (StringUtils.hasText(token) && token.length() >= 2 && loweredChunk.contains(token.toLowerCase(Locale.ROOT))) {
                score += 0.10D;
            }
        }
        return Math.min(score, 1.0D);
    }

    private List<Long> sampleIds(Set<Long> ids) {
        List<Long> result = new ArrayList<Long>(ids);
        Collections.sort(result);
        if (result.size() > 20) {
            return new ArrayList<Long>(result.subList(0, 20));
        }
        return result;
    }

    private List<Map<String, Object>> buildChunkDetailRecords(List<KnowledgeBase> scopedKnowledge,
                                                              Set<Long> chunkKnowledgeIds,
                                                              int chunkTotal) {
        List<Map<String, Object>> details = new ArrayList<Map<String, Object>>();
        int accumulatedChunks = 0;
        for (KnowledgeBase row : scopedKnowledge) {
            if (row == null || row.getId() == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("knowledge_id", row.getId());
            item.put("doc_type", row.getDocType());
            item.put("entity_type", row.getEntityType());
            item.put("entity_id", row.getEntityId());
            item.put("chunk_present", chunkKnowledgeIds.contains(row.getId()));
            item.put("should_index", knowledgeService.isKnowledgeActiveForIndex(row, LocalDateTime.now()));
            details.add(item);
            accumulatedChunks++;
            if (details.size() >= 20 || accumulatedChunks >= chunkTotal) {
                break;
            }
        }
        return details;
    }
}
