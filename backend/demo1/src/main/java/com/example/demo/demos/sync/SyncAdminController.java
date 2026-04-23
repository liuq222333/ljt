package com.example.demo.demos.sync;

import com.example.demo.demos.generic.Resp;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/sync/admin")
public class SyncAdminController {

    private final SyncAdminService service;

    public SyncAdminController(SyncAdminService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public Resp<Map<String, Object>> status() {
        return ok(service::status);
    }

    @GetMapping("/report/baseline")
    public Resp<Map<String, Object>> baselineReport() {
        return ok(service::baselineReport);
    }

    @GetMapping("/tasks")
    public Resp<List<Map<String, Object>>> tasks() {
        return okList(service::listTasks);
    }

    @GetMapping("/tasks/{taskId}")
    public Resp<Map<String, Object>> task(@PathVariable("taskId") long taskId) {
        return ok(() -> service.getTask(taskId));
    }

    @GetMapping("/dead-letters")
    public Resp<List<Map<String, Object>>> deadLetters() {
        return okList(service::deadLetters);
    }

    @PostMapping("/tasks/{taskId}/retry")
    public Resp<Map<String, Object>> retryTask(@PathVariable("taskId") long taskId) {
        return ok(() -> service.retryTask(taskId));
    }

    @GetMapping("/reconcile/products")
    public Resp<Map<String, Object>> reconcileProducts() {
        return ok(service::reconcileProducts);
    }

    @GetMapping("/reconcile/products/detail")
    public Resp<Map<String, Object>> reconcileProductsDetail(@RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return ok(() -> service.reconcileProductsDetail(limit));
    }

    @GetMapping("/products/index/validate")
    public Resp<Map<String, Object>> validateProductIndex() {
        return ok(service::validateProductIndex);
    }

    @GetMapping("/repair/strategy")
    public Resp<Map<String, Object>> repairStrategy() {
        return ok(service::repairStrategyReport);
    }

    @PostMapping("/rebuild/products")
    public Resp<Map<String, Object>> rebuildProducts(@RequestParam(value = "targetIndex", required = false) String targetIndex) {
        return ok(() -> service.rebuildProducts(targetIndex));
    }

    @PostMapping("/rebuild/products/versioned")
    public Resp<Map<String, Object>> rebuildProductsVersioned(@RequestParam(value = "targetIndex", required = false) String targetIndex,
                                                              @RequestParam(value = "switchAlias", defaultValue = "false") boolean switchAlias) {
        return ok(() -> service.rebuildProductsVersioned(targetIndex, switchAlias));
    }

    @PostMapping("/incremental/products")
    public Resp<Map<String, Object>> incrementalProducts(@RequestParam(value = "start", required = false) String start,
                                                         @RequestParam(value = "end", required = false) String end,
                                                         @RequestParam(value = "limit", required = false) Integer limit,
                                                         @RequestParam(value = "targetIndex", required = false) String targetIndex) {
        return ok(() -> service.incrementalProducts(parseTime(start), parseTime(end), limit, targetIndex));
    }

    @PostMapping("/repair/product/{productId}")
    public Resp<Map<String, Object>> repairProduct(@PathVariable("productId") Long productId) {
        return ok(() -> service.repairProduct(productId));
    }

    @PostMapping("/repair/products/by-ids")
    public Resp<Map<String, Object>> repairProductsByIds(@RequestBody(required = false) Map<String, Object> body,
                                                         @RequestParam(value = "targetIndex", required = false) String targetIndex) {
        return ok(() -> service.repairProductsByIds(longList(body == null ? null : body.get("ids")), targetIndex));
    }

    @PostMapping("/repair/products/by-updated-range")
    public Resp<Map<String, Object>> repairProductsByUpdatedRange(@RequestParam(value = "start", required = false) String start,
                                                                  @RequestParam(value = "end", required = false) String end,
                                                                  @RequestParam(value = "limit", required = false) Integer limit,
                                                                  @RequestParam(value = "targetIndex", required = false) String targetIndex) {
        return ok(() -> service.repairProductsByUpdatedRange(parseTime(start), parseTime(end), limit, targetIndex));
    }

    @PostMapping("/repair/products/by-status")
    public Resp<Map<String, Object>> repairProductsByStatus(@RequestParam(value = "status", required = false) String status,
                                                            @RequestParam(value = "limit", required = false) Integer limit,
                                                            @RequestParam(value = "targetIndex", required = false) String targetIndex) {
        return ok(() -> service.repairProductsByStatus(status, limit, targetIndex));
    }

    @PostMapping("/repair/products/by-category")
    public Resp<Map<String, Object>> repairProductsByCategory(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                                              @RequestParam(value = "status", required = false) String status,
                                                              @RequestParam(value = "limit", required = false) Integer limit,
                                                              @RequestParam(value = "targetIndex", required = false) String targetIndex) {
        return ok(() -> service.repairProductsByCategory(categoryId, status, limit, targetIndex));
    }

    @PostMapping("/products/index/switch-alias")
    public Resp<Map<String, Object>> switchProductAliases(@RequestParam("targetIndex") String targetIndex) {
        return ok(() -> service.switchProductAliases(targetIndex));
    }

    @GetMapping("/products/index/rollback/candidates")
    public Resp<List<Map<String, Object>>> rollbackCandidates() {
        return okList(service::listProductRollbackCandidates);
    }

    @PostMapping("/products/index/rollback/auto")
    public Resp<Map<String, Object>> autoRollbackProducts() {
        return ok(service::autoRollbackProducts);
    }

    @GetMapping("/reconcile/knowledge")
    public Resp<Map<String, Object>> reconcileKnowledge(@RequestParam(value = "docType", required = false) String docType,
                                                        @RequestParam(value = "entityType", required = false) String entityType,
                                                        @RequestParam(value = "entityId", required = false) String entityId,
                                                        @RequestParam(value = "status", required = false) Integer status,
                                                        @RequestParam(value = "limit", required = false) Integer limit) {
        return ok(() -> service.reconcileKnowledge(docType, entityType, entityId, status, limit));
    }

    @PostMapping("/rebuild/knowledge")
    public Resp<Map<String, Object>> rebuildKnowledge(@RequestParam(value = "rebuildExisting", defaultValue = "true") boolean rebuildExisting) {
        return ok(() -> service.rebuildKnowledge(rebuildExisting));
    }

    @PostMapping("/incremental/knowledge")
    public Resp<Map<String, Object>> incrementalKnowledge(@RequestParam(value = "start", required = false) String start,
                                                          @RequestParam(value = "end", required = false) String end,
                                                          @RequestParam(value = "docType", required = false) String docType,
                                                          @RequestParam(value = "entityType", required = false) String entityType,
                                                          @RequestParam(value = "entityId", required = false) String entityId,
                                                          @RequestParam(value = "status", required = false) Integer status,
                                                          @RequestParam(value = "limit", required = false) Integer limit) {
        return ok(() -> service.incrementalKnowledge(parseTime(start), parseTime(end), docType, entityType, entityId, status, limit));
    }

    @PostMapping("/repair/knowledge/{knowledgeId}")
    public Resp<Map<String, Object>> repairKnowledge(@PathVariable("knowledgeId") Long knowledgeId) {
        return ok(() -> service.repairKnowledge(knowledgeId));
    }

    @PostMapping("/repair/knowledge/by-ids")
    public Resp<Map<String, Object>> repairKnowledgeByIds(@RequestBody(required = false) Map<String, Object> body) {
        return ok(() -> service.repairKnowledgeByIds(longList(body == null ? null : body.get("ids"))));
    }

    @PostMapping("/repair/knowledge/by-filter")
    public Resp<Map<String, Object>> repairKnowledgeByFilter(@RequestParam(value = "docType", required = false) String docType,
                                                             @RequestParam(value = "entityType", required = false) String entityType,
                                                             @RequestParam(value = "entityId", required = false) String entityId,
                                                             @RequestParam(value = "status", required = false) Integer status,
                                                             @RequestParam(value = "limit", required = false) Integer limit) {
        return ok(() -> service.repairKnowledgeByFilters(docType, entityType, entityId, status, limit));
    }

    private Resp<Map<String, Object>> ok(Supplier<Map<String, Object>> supplier) {
        return Resp.success(supplier.get());
    }

    private Resp<List<Map<String, Object>>> okList(Supplier<List<Map<String, Object>>> supplier) {
        return Resp.success(supplier.get());
    }

    private List<Long> longList(Object raw) {
        if (!(raw instanceof List)) {
            return Collections.emptyList();
        }
        List<?> values = (List<?>) raw;
        java.util.ArrayList<Long> result = new java.util.ArrayList<Long>();
        for (Object value : values) {
            if (value instanceof Number) {
                result.add(((Number) value).longValue());
            } else if (value instanceof String) {
                result.add(Long.valueOf((String) value));
            }
        }
        return result;
    }

    private LocalDateTime parseTime(String value) {
        return StringUtils.hasText(value) ? LocalDateTime.parse(value) : null;
    }
}
