package com.example.demo.demos.Agent.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Pojo.KnowledgeDTO;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalRequest;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalResponse;
import com.example.demo.demos.Agent.Service.KnowledgeChunkService;
import com.example.demo.demos.Agent.Service.KnowledgeSearchService;
import com.example.demo.demos.Agent.Service.KnowledgeService;
import com.example.demo.demos.Agent.Service.KnowledgeVectorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private KnowledgeVectorService knowledgeVectorService;

    @Resource
    private KnowledgeChunkService knowledgeChunkService;

    @Resource
    private KnowledgeSearchService knowledgeSearchService;

    @PostMapping("/add")
    public Map<String, Object> addKnowledge(@RequestBody KnowledgeDTO dto) {
        Long id = knowledgeService.addKnowledge(dto);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("id", id);
        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> listKnowledge(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String docType,
            @RequestParam(defaultValue = "published") String lifecycleStatus) {
        Page<KnowledgeBase> pageData = knowledgeService.listKnowledge(page, size, category, docType, lifecycleStatus);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("data", pageData);
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getKnowledge(@PathVariable Long id) {
        KnowledgeBase knowledge = knowledgeService.getKnowledge(id);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("data", knowledge);
        return result;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateKnowledge(@PathVariable Long id, @RequestBody KnowledgeDTO dto) {
        knowledgeService.updateKnowledge(id, dto);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        return result;
    }

    @PostMapping("/{id}/publish")
    public Map<String, Object> publishKnowledge(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime effectiveFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime effectiveTo) {
        knowledgeService.publishKnowledge(id, effectiveFrom, effectiveTo);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("action", "published");
        return result;
    }

    @PostMapping("/{id}/disable")
    public Map<String, Object> disableKnowledge(@PathVariable Long id) {
        knowledgeService.disableKnowledge(id);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("action", "disabled");
        return result;
    }

    @PostMapping("/{id}/archive")
    public Map<String, Object> archiveKnowledge(@PathVariable Long id) {
        knowledgeService.archiveKnowledge(id);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("action", "archived");
        return result;
    }

    @PostMapping("/generate-vectors")
    public Map<String, Object> generateVectors(@RequestParam(defaultValue = "false") boolean rebuildExisting) {
        int count = knowledgeVectorService.generateVectors(rebuildExisting);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("count", count);
        result.put("message", (rebuildExisting ? "成功重建 " : "成功生成 ") + count + " 条知识向量");
        result.put("stats", knowledgeVectorService.collectVectorStats());
        return result;
    }

    @PostMapping("/chunks/rebuild")
    public Map<String, Object> rebuildChunks(@RequestParam(defaultValue = "false") boolean rebuildExisting) {
        int count = knowledgeChunkService.generateChunks(rebuildExisting);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("count", count);
        result.put("stats", knowledgeChunkService.collectChunkStats());
        return result;
    }

    @GetMapping("/chunks/stats")
    public Map<String, Object> chunkStats(
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer limit) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("data", knowledgeChunkService.collectChunkStats(docType, entityType, entityId, status, limit));
        return result;
    }

    @PostMapping("/sync/rebuild")
    public Map<String, Object> rebuildKnowledgeArtifacts(
            @RequestParam(defaultValue = "true") boolean rebuildVectors,
            @RequestParam(defaultValue = "true") boolean rebuildChunks) {
        return knowledgeService.rebuildKnowledgeArtifacts(rebuildVectors, rebuildChunks);
    }

    @PostMapping("/retrieve")
    public Map<String, Object> retrieveKnowledge(@RequestBody(required = false) KnowledgeRetrievalRequest request) {
        KnowledgeRetrievalResponse response = knowledgeSearchService.retrieve(request);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("data", response);
        return result;
    }
}
