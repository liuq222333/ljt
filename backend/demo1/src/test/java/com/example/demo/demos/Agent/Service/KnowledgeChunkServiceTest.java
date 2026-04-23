package com.example.demo.demos.Agent.Service;

import com.example.demo.demos.Agent.Dao.KnowledgeBaseMapper;
import com.example.demo.demos.Agent.Dao.KnowledgeChunkMapper;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Entity.KnowledgeChunk;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeChunkServiceTest {

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;
    @Mock
    private KnowledgeChunkMapper knowledgeChunkMapper;
    @Mock
    private KnowledgeService knowledgeService;

    @InjectMocks
    private KnowledgeChunkService knowledgeChunkService;

    @Test
    void rebuildChunksShouldInsertChunkRowsForActiveKnowledge() {
        KnowledgeBase knowledgeBase = knowledge(1L);
        when(knowledgeBaseMapper.selectById(1L)).thenReturn(knowledgeBase);
        when(knowledgeService.isKnowledgeActiveForIndex(eq(knowledgeBase), any(LocalDateTime.class))).thenReturn(true);

        int count = knowledgeChunkService.rebuildChunks(1L);

        assertTrue(count >= 4);
        verify(knowledgeChunkMapper, atLeast(count)).insert(any(KnowledgeChunk.class));
    }

    @Test
    void collectChunkScoresShouldReturnChunkMatchForFilteredKnowledge() {
        KnowledgeBase knowledgeBase = knowledge(2L);
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setKnowledgeId(2L);
        chunk.setChunkNo(1);
        chunk.setChunkType("rule_clause");
        chunk.setChunkText("退款规则：活动开始前24小时可退");
        chunk.setStatus(1);
        when(knowledgeChunkMapper.selectList(any())).thenReturn(Collections.singletonList(chunk));
        when(knowledgeBaseMapper.selectById(2L)).thenReturn(knowledgeBase);
        when(knowledgeService.matchesKnowledgeFilters(eq(knowledgeBase), any(KnowledgeRetrievalRequest.class), anyList(), any(LocalDateTime.class)))
                .thenReturn(true);

        Map<Long, Double> scores = knowledgeChunkService.collectChunkScores(
                "退款规则",
                new KnowledgeRetrievalRequest(),
                Collections.singletonList("refund_rule"),
                20,
                LocalDateTime.now()
        );

        assertFalse(scores.isEmpty());
        assertTrue(scores.get(2L) > 0.0D);
    }

    private KnowledgeBase knowledge(Long id) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setDocType("refund_rule");
        knowledgeBase.setTitle("退款规则");
        knowledgeBase.setSummary("支持退改");
        knowledgeBase.setRelatedQuestions("可以退款吗\n退款多久到账");
        knowledgeBase.setContent("活动开始前24小时可退。\n\n逾期不支持退款。");
        knowledgeBase.setStatus(1);
        return knowledgeBase;
    }
}
