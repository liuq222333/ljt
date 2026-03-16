package com.example.demo.demos.Agent.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Pojo.KnowledgeDTO;
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

    @PostMapping("/add")
    public Map<String, Object> addKnowledge(@RequestBody KnowledgeDTO dto) {
        Long id = knowledgeService.addKnowledge(dto);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", id);
        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> listKnowledge(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category) {
        Page<KnowledgeBase> pageData = knowledgeService.listKnowledge(page, size, category);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", pageData);
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getKnowledge(@PathVariable Long id) {
        KnowledgeBase knowledge = knowledgeService.getKnowledge(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", knowledge);
        return result;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateKnowledge(@PathVariable Long id, @RequestBody KnowledgeDTO dto) {
        knowledgeService.updateKnowledge(id, dto);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * 批量生成向量（管理员工具）
     * 为所有没有向量的知识生成向量
     */
    @PostMapping("/generate-vectors")
    public Map<String, Object> generateVectors() {
        int count = knowledgeVectorService.generateMissingVectors();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", count);
        result.put("message", "成功生成 " + count + " 条知识向量");
        return result;
    }
}
