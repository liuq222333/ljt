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

import javax.annotation.Resource;
import java.time.LocalDateTime;
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

    /**
     * 添加知识并自动生成向量
     */
    public Long addKnowledge(KnowledgeDTO dto) {
        // 1. 保存知识到 knowledge_base 表
        KnowledgeBase entity = new KnowledgeBase();
        BeanUtils.copyProperties(dto, entity);
        knowledgeBaseMapper.insert(entity);
        Long knowledgeId = entity.getId();

        // 2. 生成向量并保存到 knowledge_vector 表
        try {
            // 拼接标题和内容作为向量化输入
            String textToEmbed = dto.getTitle() + "\n" + dto.getContent();

            // 调用 Embedding API 生成向量
            float[] vector = embeddingService.embed(textToEmbed);

            // 将向量转为 JSON 字符串
            String vectorJson = floatArrayToJson(vector);

            // 保存到数据库
            KnowledgeVector vectorEntity = new KnowledgeVector();
            vectorEntity.setKnowledgeId(knowledgeId);
            vectorEntity.setVectorData(vectorJson);
            vectorEntity.setCreatedAt(LocalDateTime.now());
            knowledgeVectorMapper.insert(vectorEntity);

            log.info("知识向量生成成功: knowledgeId={}", knowledgeId);

        } catch (Exception e) {
            // 向量生成失败不影响知识保存，记录日志即可
            log.error("生成知识向量失败: knowledgeId={}", knowledgeId, e);
        }

        return knowledgeId;
    }

    /**
     * 更新知识并重新生成向量
     */
    public void updateKnowledge(Long id, KnowledgeDTO dto) {
        // 1. 更新知识内容
        KnowledgeBase entity = new KnowledgeBase();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        knowledgeBaseMapper.updateById(entity);

        // 2. 删除旧向量
        QueryWrapper<KnowledgeVector> wrapper = new QueryWrapper<>();
        wrapper.eq("knowledge_id", id);
        knowledgeVectorMapper.delete(wrapper);

        // 3. 重新生成向量
        try {
            String textToEmbed = dto.getTitle() + "\n" + dto.getContent();
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

    /**
     * 删除知识及其向量
     */
    public void deleteKnowledge(Long id) {
        // 删除知识
        knowledgeBaseMapper.deleteById(id);

        // 删除对应的向量
        QueryWrapper<KnowledgeVector> wrapper = new QueryWrapper<>();
        wrapper.eq("knowledge_id", id);
        knowledgeVectorMapper.delete(wrapper);
    }

    public KnowledgeBase getKnowledge(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    public Page<KnowledgeBase> listKnowledge(int page, int size, String category) {
        Page<KnowledgeBase> pageObj = new Page<>(page, size);
        QueryWrapper<KnowledgeBase> wrapper = new QueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq("category", category);
        }
        wrapper.eq("status", 1).orderByDesc("created_at");
        return knowledgeBaseMapper.selectPage(pageObj, wrapper);
    }

    public List<KnowledgeBase> searchByKeyword(String keyword) {
        QueryWrapper<KnowledgeBase> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
               .and(w -> w.like("title", keyword)
                          .or().like("content", keyword)
                          .or().like("keywords", keyword));
        return knowledgeBaseMapper.selectList(wrapper);
    }

    /**
     * 工具方法：将 float[] 转为 JSON 字符串
     */
    private String floatArrayToJson(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 工具方法：将 JSON 字符串转为 float[]
     */
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
