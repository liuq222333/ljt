package com.example.demo.demos.Agent.Service;

import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalRequest;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Component
public class KnowledgeEnhancer {

    @Resource
    private KnowledgeSearchService knowledgeSearchService;

    private static final String KNOWLEDGE_TEMPLATE =
            "\n\n【相关知识库】\n%s\n\n请优先依据以上知识库内容回答用户问题；如果知识不足，再结合工具结果补充。";

    public String enhancePrompt(String originalPrompt, String userQuestion) {
        if (!StringUtils.hasText(userQuestion)) {
            return originalPrompt;
        }

        KnowledgeRetrievalRequest request = new KnowledgeRetrievalRequest();
        request.setQueryText(userQuestion);
        request.setPurpose("answer_enhancement");
        request.setTaskType("faq_query");
        request.setDocTypes(Arrays.asList("faq", "refund_rule", "reservation_rule", "usage_notice", "guide", "rule"));
        request.setNeedRerank(true);
        request.setTopK(3);

        KnowledgeRetrievalResponse response = knowledgeSearchService.retrieve(request);
        List<KnowledgeBase> knowledge = response.getItems();
        if (knowledge.isEmpty()) {
            return originalPrompt;
        }

        StringBuilder knowledgeText = new StringBuilder();
        for (int index = 0; index < knowledge.size(); index++) {
            KnowledgeBase kb = knowledge.get(index);
            knowledgeText.append(index + 1)
                    .append(". ")
                    .append(defaultText(kb.getTitle(), "未命名知识"))
                    .append('\n');
            if (StringUtils.hasText(kb.getSummary())) {
                knowledgeText.append(kb.getSummary()).append('\n');
            }
            knowledgeText.append(defaultText(kb.getContent(), "无正文")).append('\n');
        }

        return originalPrompt + String.format(KNOWLEDGE_TEMPLATE, knowledgeText.toString());
    }

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

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
