package com.example.demo.demos.Agent.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.demos.Agent.Dao.KnowledgeBaseMapper;
import com.example.demo.demos.Agent.Dao.KnowledgeVectorMapper;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Entity.KnowledgeVector;
import com.example.demo.demos.Agent.Pojo.KnowledgeDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;
    @Mock
    private KnowledgeVectorMapper knowledgeVectorMapper;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private KnowledgeChunkService knowledgeChunkService;

    @InjectMocks
    private KnowledgeService knowledgeService;

    @Test
    void publishKnowledgeShouldRefreshVectorAndChunks() {
        KnowledgeBase knowledgeBase = knowledge(10L, 0);
        when(knowledgeBaseMapper.selectById(10L)).thenReturn(knowledgeBase);
        when(embeddingService.embed(any(String.class))).thenReturn(new float[]{1.0F, 2.0F});

        knowledgeService.publishKnowledge(10L, null, null);

        assertEquals(Integer.valueOf(1), knowledgeBase.getStatus());
        assertNotNull(knowledgeBase.getPublishedAt());
        verify(knowledgeBaseMapper).updateById(knowledgeBase);
        verify(knowledgeVectorMapper).delete(any());
        verify(knowledgeVectorMapper).insert(any(KnowledgeVector.class));
        verify(knowledgeChunkService).rebuildChunks(10L);
    }

    @Test
    void archiveKnowledgeShouldStopVectorInsertAndRebuildChunks() {
        KnowledgeBase knowledgeBase = knowledge(11L, 1);
        when(knowledgeBaseMapper.selectById(11L)).thenReturn(knowledgeBase);

        knowledgeService.archiveKnowledge(11L);

        assertEquals(Integer.valueOf(0), knowledgeBase.getStatus());
        verify(knowledgeVectorMapper).delete(any());
        verify(knowledgeVectorMapper, never()).insert(any(KnowledgeVector.class));
        verify(knowledgeChunkService).rebuildChunks(11L);
    }

    @Test
    void updateKnowledgeShouldMergeNonNullFieldsWithoutPublishingDraft() {
        KnowledgeBase existing = knowledge(12L, 0);
        existing.setPublishedAt(null);
        existing.setContent("old-content");
        existing.setSummary("old-summary");
        when(knowledgeBaseMapper.selectById(12L)).thenReturn(existing);

        KnowledgeDTO dto = new KnowledgeDTO();
        dto.setTitle("new-title");

        knowledgeService.updateKnowledge(12L, dto);

        ArgumentCaptor<KnowledgeBase> captor = ArgumentCaptor.forClass(KnowledgeBase.class);
        verify(knowledgeBaseMapper).updateById(captor.capture());
        KnowledgeBase updated = captor.getValue();
        assertEquals("new-title", updated.getTitle());
        assertEquals("old-content", updated.getContent());
        assertEquals("old-summary", updated.getSummary());
        assertEquals(Integer.valueOf(0), updated.getStatus());
        assertNull(updated.getPublishedAt());
        verify(knowledgeChunkService).rebuildChunks(12L);
    }

    @Test
    void listKnowledgeShouldDefaultToPublishedLifecycleWhenLifecycleStatusMissing() {
        Page<KnowledgeBase> page = new Page<KnowledgeBase>(1, 10);
        when(knowledgeBaseMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

        Page<KnowledgeBase> result = knowledgeService.listKnowledge(1, 10, null, null, null);

        assertSame(page, result);
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(knowledgeBaseMapper).selectPage(any(Page.class), captor.capture());
        QueryWrapper<?> wrapper = captor.getValue();
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("status"));
        assertTrue(sqlSegment.contains("effective_from"));
        assertTrue(sqlSegment.contains("effective_to"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(1));
    }

    @Test
    void rebuildKnowledgeArtifactsShouldCountOnlySuccessfulVectorWrites() {
        KnowledgeBase success = knowledge(20L, 1);
        KnowledgeBase failure = knowledge(21L, 1);
        when(knowledgeBaseMapper.selectList(null)).thenReturn(Arrays.asList(success, failure));
        when(embeddingService.embed(any(String.class)))
                .thenReturn(new float[]{1.0F, 2.0F})
                .thenThrow(new RuntimeException("embed failed"));

        Map<String, Object> result = knowledgeService.rebuildKnowledgeArtifacts(true, false);

        assertEquals(Boolean.TRUE, result.get("success"));
        assertEquals(1, result.get("vectorGenerated"));
        verify(knowledgeVectorMapper).insert(any(KnowledgeVector.class));
    }

    private KnowledgeBase knowledge(Long id, Integer status) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setDocType("faq");
        knowledgeBase.setCategory("faq");
        knowledgeBase.setTitle("title");
        knowledgeBase.setContent("content");
        knowledgeBase.setStatus(status);
        knowledgeBase.setEffectiveTo(LocalDateTime.now().plusDays(1));
        return knowledgeBase;
    }
}
