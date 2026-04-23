package com.example.demo.demos.Agent.Service;

import com.example.demo.demos.Agent.Dao.KnowledgeBaseMapper;
import com.example.demo.demos.Agent.Dao.KnowledgeVectorMapper;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalRequest;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeSearchServiceTest {

    @Mock
    private KnowledgeService knowledgeService;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private KnowledgeVectorMapper knowledgeVectorMapper;
    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;
    @Mock
    private KnowledgeChunkService knowledgeChunkService;

    @InjectMocks
    private KnowledgeSearchService knowledgeSearchService;

    @Test
    void retrieveShouldUseChunkVersionWhenChunkScoresPresent() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(21L);
        knowledgeBase.setDocType("refund_rule");
        knowledgeBase.setTitle("退款规则");
        knowledgeBase.setContent("活动开始前24小时可退");
        knowledgeBase.setStatus(1);

        KnowledgeRetrievalRequest request = new KnowledgeRetrievalRequest();
        request.setQueryText("退款规则");
        request.setNeedRerank(false);

        when(knowledgeBaseMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(knowledgeVectorMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(embeddingService.embed(any(String.class))).thenReturn(new float[]{1.0F, 0.0F});
        when(knowledgeChunkService.collectChunkScores(any(String.class), any(KnowledgeRetrievalRequest.class), anyList(), eq(30), any()))
                .thenReturn(new LinkedHashMap<Long, Double>(Collections.singletonMap(21L, 0.85D)));
        when(knowledgeBaseMapper.selectById(21L)).thenReturn(knowledgeBase);
        when(knowledgeService.matchesKnowledgeFilters(eq(knowledgeBase), any(KnowledgeRetrievalRequest.class), anyList(), any()))
                .thenReturn(true);

        KnowledgeRetrievalResponse response = knowledgeSearchService.retrieve(request);

        assertEquals("kb_v2_chunk_keyword", response.getQueryVersion());
        assertEquals(1, response.getHitCount());
        assertEquals(knowledgeBase.getId(), response.getItems().get(0).getId());
    }
}
