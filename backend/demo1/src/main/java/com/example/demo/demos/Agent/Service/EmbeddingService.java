package com.example.demo.demos.Agent.Service;

import com.example.demo.demos.Agent.Config.OpenAiEmbeddingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量化服务 - 调用 OpenAI Embedding API 将文本转换为向量
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    @Resource
    private OpenAiEmbeddingProperties properties;

    // RestTemplate 用于发送 HTTP 请求
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 将文本转换为向量
     * @param text 输入文本（标题+内容）
     * @return 向量数组（1536维）
     */
    public float[] embed(String text) {
        try {
            // 1. 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", properties.getModel()); // 使用的模型
            requestBody.put("input", text); // 要向量化的文本

            // 2. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON); // JSON 格式
            headers.setBearerAuth(properties.getKey()); // API Key 认证

            // 3. 调用 OpenAI Embedding API
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                properties.getUrl(), entity, Map.class);

            // 4. 解析响应 - 提取向量数据
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            List<Double> embedding = (List<Double>) data.get(0).get("embedding");

            // 5. 转为 float[] 数组
            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i).floatValue();
            }

            log.info("成功生成向量，维度: {}", vector.length);
            return vector;

        } catch (Exception e) {
            log.error("调用 Embedding API 失败", e);
            throw new RuntimeException("向量化失败: " + e.getMessage(), e);
        }
    }
}
