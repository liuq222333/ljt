package com.example.demo.demos.Agent.Service;

import com.example.demo.demos.Agent.Dao.KnowledgeBaseMapper;
import com.example.demo.demos.Agent.Dao.KnowledgeVectorMapper;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Entity.KnowledgeVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);

    @Resource
    private KnowledgeService knowledgeService;

    @Resource(required = false)
    private EmbeddingService embeddingService;

    @Resource
    private KnowledgeVectorMapper knowledgeVectorMapper;

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 简单关键词搜索（Phase 1）
     */
    public List<KnowledgeBase> search(String query) {
        return knowledgeService.searchByKeyword(query);
    }

    /**
     * 向量相似度检索（Phase 2）
     * @param query 用户查询
     * @param topK 返回前 K 个最相似的结果
     * @return 知识列表（按相似度降序）
     */
    public List<KnowledgeBase> vectorSearch(String query, int topK) {
        try {
            // 1. 将查询转为向量
            float[] queryVector = embeddingService.embed(query);

            // 2. 加载所有知识向量
            List<KnowledgeVector> allVectors = knowledgeVectorMapper.selectList(null);

            if (allVectors.isEmpty()) {
                // 如果没有向量数据，降级到关键词搜索
                log.warn("知识库中没有向量数据，降级到关键词搜索");
                return knowledgeService.searchByKeyword(query);
            }

            // 3. 计算相似度并排序
            List<SimilarityResult> results = new ArrayList<>();
            for (KnowledgeVector kv : allVectors) {
                // 将 JSON 字符串转为 float[] 数组
                float[] knowledgeVector = knowledgeService.jsonToFloatArray(kv.getVectorData());

                // 计算余弦相似度
                double similarity = cosineSimilarity(queryVector, knowledgeVector);

                // 保存结果
                results.add(new SimilarityResult(kv.getKnowledgeId(), similarity));
            }

            // 按相似度降序排序
            results.sort((a, b) -> Double.compare(b.similarity, a.similarity));

            // 4. 取 Top-K 并加载完整知识
            List<KnowledgeBase> topKnowledge = new ArrayList<>();
            for (int i = 0; i < Math.min(topK, results.size()); i++) {
                Long knowledgeId = results.get(i).knowledgeId;
                KnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeId);

                // 只返回启用的知识
                if (kb != null && kb.getStatus() == 1) {
                    topKnowledge.add(kb);
                    log.info("检索结果 {}: knowledgeId={}, 相似度={}",
                        i + 1, knowledgeId, results.get(i).similarity);
                }
            }

            return topKnowledge;

        } catch (Exception e) {
            // 向量检索失败，降级到关键词搜索
            log.error("向量检索失败，降级到关键词搜索", e);
            return knowledgeService.searchByKeyword(query);
        }
    }

    /**
     * 计算余弦相似度
     * 公式: cosine_similarity = dot(A, B) / (norm(A) * norm(B))
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("向量维度不匹配: " + a.length + " vs " + b.length);
        }

        double dotProduct = 0.0;  // 点积
        double normA = 0.0;       // 向量 A 的模
        double normB = 0.0;       // 向量 B 的模

        // 计算点积和模
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        // 返回余弦相似度
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 内部类：相似度结果
     */
    private static class SimilarityResult {
        Long knowledgeId;
        double similarity;

        SimilarityResult(Long knowledgeId, double similarity) {
            this.knowledgeId = knowledgeId;
            this.similarity = similarity;
        }
    }
}
