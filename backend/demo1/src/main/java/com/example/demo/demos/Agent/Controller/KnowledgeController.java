package com.example.demo.demos.Agent.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Pojo.KnowledgeDTO;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalRequest;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalResponse;
import com.example.demo.demos.Agent.Service.KnowledgeSearchService;
import com.example.demo.demos.Agent.Service.KnowledgeService;
import com.example.demo.demos.Agent.Service.KnowledgeVectorService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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
            @RequestParam(required = false) String docType) {
        Page<KnowledgeBase> pageData = knowledgeService.listKnowledge(page, size, category, docType);
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

    @PostMapping("/generate-vectors")
    public Map<String, Object> generateVectors(
            @RequestParam(defaultValue = "false") boolean rebuildExisting) {
        int count = knowledgeVectorService.generateVectors(rebuildExisting);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("count", count);
        result.put("message", (rebuildExisting ? "成功重建 " : "成功生成 ") + count + " 条知识向量");
        return result;
    }

    @PostMapping("/retrieve")
    public Map<String, Object> retrieveKnowledge(
            @RequestBody(required = false) KnowledgeRetrievalRequest request) {
        KnowledgeRetrievalResponse response = knowledgeSearchService.retrieve(request);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("success", true);
        result.put("data", response);
        return result;
    }
}
