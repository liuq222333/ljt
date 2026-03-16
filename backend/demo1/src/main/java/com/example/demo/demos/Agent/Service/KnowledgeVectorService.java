package com.example.demo.demos.Agent.Service;

import com.example.demo.demos.Agent.Dao.KnowledgeBaseMapper;
import com.example.demo.demos.Agent.Dao.KnowledgeVectorMapper;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Entity.KnowledgeVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识向量批量生成服务
 */
@Service
public class KnowledgeVectorService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeVectorService.class);

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private KnowledgeVectorMapper knowledgeVectorMapper;

    @Resource
    private EmbeddingService embeddingService;

    /**
     * 为所有没有向量的知识生成向量
     * @return 成功生成的向量数量
     */
    public int generateMissingVectors() {
        log.info("开始批量生成知识向量...");

        // 1. 查询所有知识
        List<KnowledgeBase> allKnowledge = knowledgeBaseMapper.selectList(null);
        log.info("知识库总数: {}", allKnowledge.size());

        // 2. 查询已有向量的知识ID
        List<KnowledgeVector> existingVectors = knowledgeVectorMapper.selectList(null);
        Set<Long> vectorizedIds = existingVectors.stream()
            .map(KnowledgeVector::getKnowledgeId)
            .collect(Collectors.toSet());
        log.info("已有向量数: {}", vectorizedIds.size());

        // 3. 筛选出没有向量的知识
        List<KnowledgeBase> missingVectorKnowledge = allKnowledge.stream()
            .filter(kb -> !vectorizedIds.contains(kb.getId()))
            .collect(Collectors.toList());
        log.info("需要生成向量的知识数: {}", missingVectorKnowledge.size());

        // 4. 批量生成向量
        int successCount = 0;
        for (KnowledgeBase kb : missingVectorKnowledge) {
            try {
                // 拼接标题和内容
                String textToEmbed = kb.getTitle() + "\n" + kb.getContent();

                // 调用 Embedding API
                float[] vector = embeddingService.embed(textToEmbed);

                // 转为 JSON 字符串
                String vectorJson = floatArrayToJson(vector);

                // 保存到数据库
                KnowledgeVector vectorEntity = new KnowledgeVector();
                vectorEntity.setKnowledgeId(kb.getId());
                vectorEntity.setVectorData(vectorJson);
                vectorEntity.setCreatedAt(LocalDateTime.now());
                knowledgeVectorMapper.insert(vectorEntity);

                successCount++;
                log.info("生成向量成功 [{}/{}]: knowledgeId={}",
                    successCount, missingVectorKnowledge.size(), kb.getId());

                // 避免 API 限流，每次请求后休眠 100ms
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("生成向量失败: knowledgeId={}", kb.getId(), e);
            }
        }

        log.info("批量生成向量完成，成功: {}, 失败: {}",
            successCount, missingVectorKnowledge.size() - successCount);

        return successCount;
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
}
