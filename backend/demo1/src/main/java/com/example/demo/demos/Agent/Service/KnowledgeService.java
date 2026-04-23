package com.example.demo.demos.Agent.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.demos.Agent.Dao.KnowledgeBaseMapper;
import com.example.demo.demos.Agent.Dao.KnowledgeVectorMapper;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Entity.KnowledgeVector;
import com.example.demo.demos.Agent.Pojo.KnowledgeDTO;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeVectorMapper knowledgeVectorMapper;

    @Resource
    private EmbeddingService embeddingService;

    @Autowired
    @Lazy
    private KnowledgeChunkService knowledgeChunkService;

    public Long addKnowledge(KnowledgeDTO dto) {
        KnowledgeBase entity = new KnowledgeBase();
        BeanUtils.copyProperties(dto, entity);
        fillDefaults(entity);
        knowledgeBaseMapper.insert(entity);
        rebuildArtifacts(entity.getId());
        return entity.getId();
    }

    public void updateKnowledge(Long id, KnowledgeDTO dto) {
        KnowledgeBase existing = requireKnowledge(id);
        mergeNonNullProperties(dto, existing);
        fillNonLifecycleDefaults(existing);
        existing.setId(id);
        knowledgeBaseMapper.updateById(existing);
        rebuildArtifacts(id);
    }

    public void deleteKnowledge(Long id) {
        knowledgeBaseMapper.deleteById(id);
        QueryWrapper<KnowledgeVector> wrapper = new QueryWrapper<KnowledgeVector>();
        wrapper.eq("knowledge_id", id);
        knowledgeVectorMapper.delete(wrapper);
        if (knowledgeChunkService != null) {
            knowledgeChunkService.deleteChunksByKnowledgeId(id);
        }
    }

    public KnowledgeBase getKnowledge(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    public void publishKnowledge(Long id, LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {
        KnowledgeBase knowledgeBase = requireKnowledge(id);
        knowledgeBase.setStatus(1);
        if (effectiveFrom != null) {
            knowledgeBase.setEffectiveFrom(effectiveFrom);
        }
        knowledgeBase.setEffectiveTo(effectiveTo);
        if (knowledgeBase.getPublishedAt() == null) {
            knowledgeBase.setPublishedAt(LocalDateTime.now());
        }
        knowledgeBaseMapper.updateById(knowledgeBase);
        rebuildArtifacts(id);
    }

    public void disableKnowledge(Long id) {
        KnowledgeBase knowledgeBase = requireKnowledge(id);
        knowledgeBase.setStatus(0);
        knowledgeBaseMapper.updateById(knowledgeBase);
        rebuildArtifacts(id);
    }

    public void archiveKnowledge(Long id) {
        KnowledgeBase knowledgeBase = requireKnowledge(id);
        knowledgeBase.setStatus(0);
        if (knowledgeBase.getEffectiveTo() == null || knowledgeBase.getEffectiveTo().isAfter(LocalDateTime.now())) {
            knowledgeBase.setEffectiveTo(LocalDateTime.now());
        }
        knowledgeBaseMapper.updateById(knowledgeBase);
        rebuildArtifacts(id);
    }

    public java.util.Map<String, Object> rebuildKnowledgeArtifacts(boolean rebuildVectors, boolean rebuildChunks) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
        if (rebuildVectors) {
            int vectorGenerated = 0;
            for (KnowledgeBase knowledgeBase : knowledgeBaseMapper.selectList(null)) {
                if (knowledgeBase == null || knowledgeBase.getId() == null) {
                    continue;
                }
                if (rebuildVector(knowledgeBase)) {
                    vectorGenerated++;
                }
            }
            result.put("vectorGenerated", vectorGenerated);
        }
        if (rebuildChunks && knowledgeChunkService != null) {
            result.put("chunkGenerated", knowledgeChunkService.generateChunks(true));
            result.put("chunkStats", knowledgeChunkService.collectChunkStats());
        }
        result.put("success", true);
        return result;
    }

    public Page<KnowledgeBase> listKnowledge(int page, int size, String category) {
        return listKnowledge(page, size, category, null, null);
    }

    public Page<KnowledgeBase> listKnowledge(int page, int size, String category, String docType) {
        return listKnowledge(page, size, category, docType, null);
    }

    public Page<KnowledgeBase> listKnowledge(int page,
                                             int size,
                                             String category,
                                             String docType,
                                             String lifecycleStatus) {
        Page<KnowledgeBase> pageObj = new Page<KnowledgeBase>(page, size);
        QueryWrapper<KnowledgeBase> wrapper = new QueryWrapper<KnowledgeBase>();
        LocalDateTime now = LocalDateTime.now();
        if (StringUtils.hasText(category)) {
            wrapper.eq("category", category);
        }
        if (StringUtils.hasText(docType)) {
            wrapper.eq("doc_type", docType);
        }
        applyLifecycleFilter(wrapper, lifecycleStatus, now);
        wrapper.orderByDesc("priority")
                .orderByDesc("published_at")
                .orderByDesc("updated_at");
        return knowledgeBaseMapper.selectPage(pageObj, wrapper);
    }

    public List<KnowledgeBase> searchByKeyword(String keyword) {
        QueryWrapper<KnowledgeBase> wrapper = new QueryWrapper<KnowledgeBase>();
        appendActiveKnowledgeFilter(wrapper, LocalDateTime.now());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("title", keyword)
                    .or().like("content", keyword)
                    .or().like("keywords", keyword)
                    .or().like("related_questions", keyword));
        }
        wrapper.orderByDesc("priority")
                .orderByDesc("helpful_count")
                .orderByDesc("view_count")
                .orderByDesc("updated_at");
        return knowledgeBaseMapper.selectList(wrapper);
    }

    public List<Long> listKnowledgeIdsByUpdatedRange(LocalDateTime start,
                                                     LocalDateTime end,
                                                     Integer limit,
                                                     String docType) {
        return listKnowledgeIdsByFilters(start, end, limit, docType, null, null, null);
    }

    public List<Long> listKnowledgeIdsByStatus(Integer status,
                                               Integer limit,
                                               String docType) {
        return listKnowledgeIdsByFilters(null, null, limit, docType, status, null, null);
    }

    public List<Long> listKnowledgeIdsByEntity(String entityType,
                                               String entityId,
                                               Integer limit,
                                               String docType,
                                               Integer status) {
        return listKnowledgeIdsByFilters(null, null, limit, docType, status, entityType, entityId);
    }

    public boolean isKnowledgeActiveForIndex(KnowledgeBase knowledgeBase, LocalDateTime now) {
        if (knowledgeBase == null || knowledgeBase.getId() == null) {
            return false;
        }
        if (knowledgeBase.getStatus() == null || knowledgeBase.getStatus().intValue() != 1) {
            return false;
        }
        if (knowledgeBase.getEffectiveFrom() != null && knowledgeBase.getEffectiveFrom().isAfter(now)) {
            return false;
        }
        if (knowledgeBase.getEffectiveTo() != null && knowledgeBase.getEffectiveTo().isBefore(now)) {
            return false;
        }
        return true;
    }

    public boolean matchesKnowledgeFilters(KnowledgeBase knowledgeBase,
                                           KnowledgeRetrievalRequest request,
                                           List<String> docTypes,
                                           LocalDateTime timeContext) {
        if (!isKnowledgeActiveForIndex(knowledgeBase, timeContext)) {
            return false;
        }
        if (!CollectionUtils.isEmpty(docTypes) && !docTypes.contains(resolveDocType(knowledgeBase))) {
            return false;
        }
        if (request == null) {
            return true;
        }
        if (StringUtils.hasText(request.getEntityType())
                && StringUtils.hasText(knowledgeBase.getEntityType())
                && !request.getEntityType().equalsIgnoreCase(knowledgeBase.getEntityType())) {
            return false;
        }
        if (!CollectionUtils.isEmpty(request.getEntityIds())
                && StringUtils.hasText(knowledgeBase.getEntityId())
                && !request.getEntityIds().contains(knowledgeBase.getEntityId())) {
            return false;
        }
        if (!matchesAnyLongToken(request.getCityIds(), knowledgeBase.getCityIds())) {
            return false;
        }
        return matchesAnyLongToken(request.getCategoryIds(), knowledgeBase.getCategoryIds());
    }

    public String buildEmbeddingText(KnowledgeBase entity) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(entity.getTitle())) {
            builder.append(entity.getTitle()).append('\n');
        }
        if (StringUtils.hasText(entity.getSummary())) {
            builder.append(entity.getSummary()).append('\n');
        }
        if (StringUtils.hasText(entity.getKeywords())) {
            builder.append(entity.getKeywords()).append('\n');
        }
        if (StringUtils.hasText(entity.getContent())) {
            builder.append(entity.getContent());
        }
        return builder.toString();
    }

    public float[] jsonToFloatArray(String json) {
        String content = json.substring(1, json.length() - 1);
        String[] parts = content.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    private void rebuildArtifacts(Long knowledgeId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeId);
        if (knowledgeBase == null) {
            return;
        }
        rebuildVector(knowledgeBase);
        if (knowledgeChunkService != null) {
            knowledgeChunkService.rebuildChunks(knowledgeId);
        }
    }

    private boolean rebuildVector(KnowledgeBase entity) {
        QueryWrapper<KnowledgeVector> wrapper = new QueryWrapper<KnowledgeVector>();
        wrapper.eq("knowledge_id", entity.getId());
        knowledgeVectorMapper.delete(wrapper);

        if (!isKnowledgeActiveForIndex(entity, LocalDateTime.now())) {
            return false;
        }
        try {
            String textToEmbed = buildEmbeddingText(entity);
            if (!StringUtils.hasText(textToEmbed)) {
                return false;
            }
            float[] vector = embeddingService.embed(textToEmbed);
            KnowledgeVector vectorEntity = new KnowledgeVector();
            vectorEntity.setKnowledgeId(entity.getId());
            vectorEntity.setVectorData(floatArrayToJson(vector));
            vectorEntity.setCreatedAt(LocalDateTime.now());
            knowledgeVectorMapper.insert(vectorEntity);
            return true;
        } catch (Exception e) {
            log.error("Failed to rebuild knowledge vector: knowledgeId={}", entity.getId(), e);
            return false;
        }
    }

    private List<Long> listKnowledgeIdsByFilters(LocalDateTime start,
                                                 LocalDateTime end,
                                                 Integer limit,
                                                 String docType,
                                                 Integer status,
                                                 String entityType,
                                                 String entityId) {
        QueryWrapper<KnowledgeBase> wrapper = new QueryWrapper<KnowledgeBase>();
        if (start != null) {
            wrapper.ge("updated_at", start);
        }
        if (end != null) {
            wrapper.le("updated_at", end);
        }
        if (StringUtils.hasText(docType)) {
            wrapper.eq("doc_type", docType);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(entityType)) {
            wrapper.eq("entity_type", entityType);
        }
        if (StringUtils.hasText(entityId)) {
            wrapper.eq("entity_id", entityId);
        }
        wrapper.orderByAsc("updated_at").orderByAsc("id");
        if (limit != null && limit.intValue() > 0) {
            wrapper.last("LIMIT " + Math.min(limit.intValue(), 200));
        }
        List<KnowledgeBase> rows = knowledgeBaseMapper.selectList(wrapper);
        List<Long> ids = new ArrayList<Long>();
        for (KnowledgeBase row : rows) {
            if (row != null && row.getId() != null) {
                ids.add(row.getId());
            }
        }
        return ids;
    }

    private void applyLifecycleFilter(QueryWrapper<KnowledgeBase> wrapper, String lifecycleStatus, LocalDateTime now) {
        if (!StringUtils.hasText(lifecycleStatus)) {
            appendActiveKnowledgeFilter(wrapper, now);
            return;
        }
        if ("all".equalsIgnoreCase(lifecycleStatus)) {
            return;
        }
        if ("published".equalsIgnoreCase(lifecycleStatus)) {
            appendActiveKnowledgeFilter(wrapper, now);
            return;
        }
        if ("draft".equalsIgnoreCase(lifecycleStatus)) {
            wrapper.eq("status", 0).isNull("published_at");
            return;
        }
        if ("disabled".equalsIgnoreCase(lifecycleStatus)) {
            wrapper.eq("status", 0).isNotNull("published_at")
                    .and(w -> w.isNull("effective_to").or().gt("effective_to", now));
            return;
        }
        if ("archived".equalsIgnoreCase(lifecycleStatus)) {
            wrapper.and(w -> w.eq("status", 0)
                    .or()
                    .and(nested -> nested.isNotNull("effective_to").lt("effective_to", now)));
        }
    }

    private void appendActiveKnowledgeFilter(QueryWrapper<KnowledgeBase> wrapper, LocalDateTime now) {
        wrapper.eq("status", 1);
        wrapper.and(w -> w.isNull("effective_from").or().le("effective_from", now));
        wrapper.and(w -> w.isNull("effective_to").or().ge("effective_to", now));
    }

    private void fillDefaults(KnowledgeBase entity) {
        fillNonLifecycleDefaults(entity);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getPublishedAt() == null && entity.getStatus() == 1) {
            entity.setPublishedAt(LocalDateTime.now());
        }
    }

    private void fillNonLifecycleDefaults(KnowledgeBase entity) {
        if (!StringUtils.hasText(entity.getDocType())) {
            entity.setDocType(StringUtils.hasText(entity.getCategory()) ? entity.getCategory() : "faq");
        }
        if (!StringUtils.hasText(entity.getCategory())) {
            entity.setCategory(entity.getDocType());
        }
        if (!StringUtils.hasText(entity.getSourceSystem())) {
            entity.setSourceSystem("manual");
        }
        if (!StringUtils.hasText(entity.getVersion())) {
            entity.setVersion("v1");
        }
        if (entity.getPriority() == null) {
            entity.setPriority(0);
        }
        if (!StringUtils.hasText(entity.getLanguage())) {
            entity.setLanguage("zh-CN");
        }
    }

    private void mergeNonNullProperties(KnowledgeDTO source, KnowledgeBase target) {
        if (source == null || target == null) {
            return;
        }
        if (source.getCategory() != null) {
            target.setCategory(source.getCategory());
        }
        if (source.getDocType() != null) {
            target.setDocType(source.getDocType());
        }
        if (source.getTitle() != null) {
            target.setTitle(source.getTitle());
        }
        if (source.getContent() != null) {
            target.setContent(source.getContent());
        }
        if (source.getSummary() != null) {
            target.setSummary(source.getSummary());
        }
        if (source.getKeywords() != null) {
            target.setKeywords(source.getKeywords());
        }
        if (source.getRelatedQuestions() != null) {
            target.setRelatedQuestions(source.getRelatedQuestions());
        }
        if (source.getSourceSystem() != null) {
            target.setSourceSystem(source.getSourceSystem());
        }
        if (source.getEntityType() != null) {
            target.setEntityType(source.getEntityType());
        }
        if (source.getEntityId() != null) {
            target.setEntityId(source.getEntityId());
        }
        if (source.getCityIds() != null) {
            target.setCityIds(source.getCityIds());
        }
        if (source.getCategoryIds() != null) {
            target.setCategoryIds(source.getCategoryIds());
        }
        if (source.getTagIds() != null) {
            target.setTagIds(source.getTagIds());
        }
        if (source.getVersion() != null) {
            target.setVersion(source.getVersion());
        }
        if (source.getPriority() != null) {
            target.setPriority(source.getPriority());
        }
        if (source.getOwner() != null) {
            target.setOwner(source.getOwner());
        }
        if (source.getLanguage() != null) {
            target.setLanguage(source.getLanguage());
        }
        if (source.getEffectiveFrom() != null) {
            target.setEffectiveFrom(source.getEffectiveFrom());
        }
        if (source.getEffectiveTo() != null) {
            target.setEffectiveTo(source.getEffectiveTo());
        }
        if (source.getPublishedAt() != null) {
            target.setPublishedAt(source.getPublishedAt());
        }
        if (source.getStatus() != null) {
            target.setStatus(source.getStatus());
        }
    }

    private String resolveDocType(KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null) {
            return "";
        }
        if (StringUtils.hasText(knowledgeBase.getDocType())) {
            return knowledgeBase.getDocType().trim().toLowerCase(Locale.ROOT);
        }
        return StringUtils.hasText(knowledgeBase.getCategory())
                ? knowledgeBase.getCategory().trim().toLowerCase(Locale.ROOT)
                : "";
    }

    private boolean matchesAnyLongToken(List<Long> expectedIds, String csvValue) {
        if (CollectionUtils.isEmpty(expectedIds)) {
            return true;
        }
        if (!StringUtils.hasText(csvValue)) {
            return false;
        }
        Set<String> tokens = new LinkedHashSet<String>();
        for (String part : csvValue.split(",")) {
            if (StringUtils.hasText(part)) {
                tokens.add(part.trim());
            }
        }
        for (Long expectedId : expectedIds) {
            if (expectedId != null && tokens.contains(String.valueOf(expectedId))) {
                return true;
            }
        }
        return false;
    }

    private KnowledgeBase requireKnowledge(Long id) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(id);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("knowledge not found: " + id);
        }
        return knowledgeBase;
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
}
