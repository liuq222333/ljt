package com.example.demo.demos.Agent.Service;

import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class KnowledgeEnhancer {

    @Resource
    private KnowledgeSearchService knowledgeSearchService;

    private static final String KNOWLEDGE_TEMPLATE =
        "\n\n【相关知识库】\n%s\n\n请根据上述知识库内容回答用户问题。如果知识库中没有相关内容，可以使用工具查询数据。";

    /**
     * 增强提示词 - 使用向量检索注入相关知识
     */
    public String enhancePrompt(String originalPrompt, String userQuestion) {
        // 使用向量检索（Phase 2）替代关键词搜索
        List<KnowledgeBase> knowledge = knowledgeSearchService.vectorSearch(userQuestion, 3);

        if (knowledge.isEmpty()) {
            return originalPrompt;
        }

        // 构建知识文本
        StringBuilder knowledgeText = new StringBuilder();
        int count = 0;
        for (KnowledgeBase kb : knowledge) {
            knowledgeText.append(String.format("\n%d. %s\n%s\n",
                ++count, kb.getTitle(), kb.getContent()));
        }

        // 将知识注入到系统提示词中
        return originalPrompt + String.format(KNOWLEDGE_TEMPLATE, knowledgeText.toString());
    }

    /**
     * 从消息列表中提取用户问题
     */
    public String extractUserQuestion(List<?> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        Object lastMsg = messages.get(messages.size() - 1);
        if (lastMsg instanceof com.example.demo.demos.Agent.Pojo.AgentChatMessage) {
            return ((com.example.demo.demos.Agent.Pojo.AgentChatMessage) lastMsg).getContent();
        }
        return "";
    }
}
