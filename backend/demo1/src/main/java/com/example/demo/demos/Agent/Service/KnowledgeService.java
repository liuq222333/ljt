package com.example.demo.demos.Agent.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.demos.Agent.Dao.KnowledgeBaseMapper;
import com.example.demo.demos.Agent.Dao.KnowledgeVectorMapper;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Entity.KnowledgeVector;
import com.example.demo.demos.Agent.Pojo.KnowledgeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeVectorMapper knowledgeVectorMapper;

    @Resource
    private EmbeddingService embeddingService;

    public Long addKnowledge(KnowledgeDTO dto) {
        KnowledgeBase entity = new KnowledgeBase();
        BeanUtils.copyProperties(dto, entity);
        fillDefaults(entity);
        knowledgeBaseMapper.insert(entity);
        Long knowledgeId = entity.getId();

        try {
            String textToEmbed = buildEmbeddingText(entity);
            float[] vector = embeddingService.embed(textToEmbed);
            String vectorJson = floatArrayToJson(vector);

            KnowledgeVector vectorEntity = new KnowledgeVector();
            vectorEntity.setKnowledgeId(knowledgeId);
            vectorEntity.setVectorData(vectorJson);
            vectorEntity.setCreatedAt(LocalDateTime.now());
            knowledgeVectorMapper.insert(vectorEntity);

            log.info("知识向量生成成功: knowledgeId={}", knowledgeId);
        } catch (Exception e) {
            log.error("生成知识向量失败: knowledgeId={}", knowledgeId, e);
        }

        return knowledgeId;
    }

    public void updateKnowledge(Long id, KnowledgeDTO dto) {
        KnowledgeBase entity = new KnowledgeBase();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        fillDefaults(entity);
        knowledgeBaseMapper.updateById(entity);

        QueryWrapper<KnowledgeVector> wrapper = new QueryWrapper<KnowledgeVector>();
        wrapper.eq("knowledge_id", id);
        knowledgeVectorMapper.delete(wrapper);

        try {
            String textToEmbed = buildEmbeddingText(entity);
            float[] vector = embeddingService.embed(textToEmbed);
            String vectorJson = floatArrayToJson(vector);

            KnowledgeVector vectorEntity = new KnowledgeVector();
            vectorEntity.setKnowledgeId(id);
            vectorEntity.setVectorData(vectorJson);
            vectorEntity.setCreatedAt(LocalDateTime.now());
            knowledgeVectorMapper.insert(vectorEntity);

            log.info("知识向量更新成功: knowledgeId={}", id);
        } catch (Exception e) {
            log.error("更新知识向量失败: knowledgeId={}", id, e);
        }
    }

    public void deleteKnowledge(Long id) {
        knowledgeBaseMapper.deleteById(id);

        QueryWrapper<KnowledgeVector> wrapper = new QueryWrapper<KnowledgeVector>();
        wrapper.eq("knowledge_id", id);
        knowledgeVectorMapper.delete(wrapper);
    }

    public KnowledgeBase getKnowledge(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    public Page<KnowledgeBase> listKnowledge(int page, int size, String category) {
        return listKnowledge(page, size, category, null);
    }

    public Page<KnowledgeBase> listKnowledge(int page, int size, String category, String docType) {
        Page<KnowledgeBase> pageObj = new Page<KnowledgeBase>(page, size);
        QueryWrapper<KnowledgeBase> wrapper = new QueryWrapper<KnowledgeBase>();
        LocalDateTime now = LocalDateTime.now();
        if (StringUtils.hasText(category)) {
            wrapper.eq("category", category);
        }
        if (StringUtils.hasText(docType)) {
            wrapper.eq("doc_type", docType);
        }
        appendActiveKnowledgeFilter(wrapper, now);
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

    private void appendActiveKnowledgeFilter(QueryWrapper<KnowledgeBase> wrapper, LocalDateTime now) {
        wrapper.eq("status", 1);
        wrapper.and(w -> w.isNull("effective_from").or().le("effective_from", now));
        wrapper.and(w -> w.isNull("effective_to").or().ge("effective_to", now));
    }

    private void fillDefaults(KnowledgeBase entity) {
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
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getPublishedAt() == null && entity.getStatus() == 1) {
            entity.setPublishedAt(LocalDateTime.now());
        }
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

    public float[] jsonToFloatArray(String json) {
        String content = json.substring(1, json.length() - 1);
        String[] parts = content.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
