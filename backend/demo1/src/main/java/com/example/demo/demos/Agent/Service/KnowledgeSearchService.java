package com.example.demo.demos.Agent.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.demos.Agent.Dao.KnowledgeBaseMapper;
import com.example.demo.demos.Agent.Dao.KnowledgeVectorMapper;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Entity.KnowledgeVector;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalRequest;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);

    @Resource
    private KnowledgeService knowledgeService;

    @Autowired(required = false)
    private EmbeddingService embeddingService;

    @Resource
    private KnowledgeVectorMapper knowledgeVectorMapper;

    @Resource
    private KnowledgeBaseMapper knowledgeBaseMapper;

    public List<KnowledgeBase> search(String query) {
        KnowledgeRetrievalRequest request = new KnowledgeRetrievalRequest();
        request.setQueryText(query);
        request.setTopK(5);
        request.setNeedRerank(true);
        return retrieve(request).getItems();
    }

    public List<KnowledgeBase> vectorSearch(String query, int topK) {
        KnowledgeRetrievalRequest request = new KnowledgeRetrievalRequest();
        request.setQueryText(query);
        request.setTopK(topK);
        request.setNeedRerank(true);
        return retrieve(request).getItems();
    }

    public KnowledgeRetrievalResponse retrieve(KnowledgeRetrievalRequest request) {
        KnowledgeRetrievalRequest safeRequest = request == null ? new KnowledgeRetrievalRequest() : request;
        String queryText = safeText(safeRequest.getQueryText());
        List<String> docTypes = resolveDocTypes(safeRequest);
        int topK = safeRequest.getTopK() <= 0 ? 5 : safeRequest.getTopK();
        LocalDateTime timeContext = safeRequest.getTimeContext() == null
                ? LocalDateTime.now() : safeRequest.getTimeContext();

        List<KnowledgeBase> lexicalCandidates = searchLexicalCandidates(queryText, docTypes, topK * 4, timeContext);
        Map<Long, CandidateScore> candidateScores = new LinkedHashMap<Long, CandidateScore>();
        for (KnowledgeBase knowledgeBase : lexicalCandidates) {
            if (!matchesKnowledgeFilters(knowledgeBase, safeRequest, docTypes, timeContext)) {
                continue;
            }
            CandidateScore candidate = getOrCreateCandidate(candidateScores, knowledgeBase);
            candidate.lexicalScore = computeLexicalScore(knowledgeBase, queryText);
        }

        boolean usedVector = false;
        if (embeddingService != null && StringUtils.hasText(queryText)) {
            try {
                List<CandidateScore> vectorCandidates = searchVectorCandidates(queryText, safeRequest, docTypes, topK * 6, timeContext);
                usedVector = !vectorCandidates.isEmpty();
                for (CandidateScore candidate : vectorCandidates) {
                    CandidateScore existing = getOrCreateCandidate(candidateScores, candidate.knowledgeBase);
                    existing.vectorScore = Math.max(existing.vectorScore, candidate.vectorScore);
                }
            } catch (Exception ex) {
                log.warn("Knowledge vector retrieval failed, fallback to lexical only: {}", ex.getMessage());
            }
        }

        List<CandidateScore> rankedCandidates = new ArrayList<CandidateScore>(candidateScores.values());
        for (CandidateScore candidate : rankedCandidates) {
            candidate.finalScore = computeFinalScore(candidate, queryText, safeRequest, docTypes);
        }
        rankedCandidates.sort((left, right) -> {
            int scoreCompare = Double.compare(right.finalScore, left.finalScore);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            int helpfulCompare = compareNullable(right.knowledgeBase.getHelpfulCount(), left.knowledgeBase.getHelpfulCount());
            if (helpfulCompare != 0) {
                return helpfulCompare;
            }
            return compareNullable(right.knowledgeBase.getViewCount(), left.knowledgeBase.getViewCount());
        });

        KnowledgeRetrievalResponse response = new KnowledgeRetrievalResponse();
        response.setFilterApplied(hasMetadataFilters(safeRequest) || !CollectionUtils.isEmpty(docTypes));
        response.setRerankApplied(safeRequest.isNeedRerank() && rankedCandidates.size() > 1);
        response.setCandidateCount(rankedCandidates.size());
        response.setQueryVersion(usedVector ? "kb_v1_hybrid" : "kb_v1_keyword");
        for (int index = 0; index < Math.min(topK, rankedCandidates.size()); index++) {
            response.getItems().add(rankedCandidates.get(index).knowledgeBase);
        }
        response.setHitCount(response.getItems().size());
        return response;
    }

    private List<KnowledgeBase> searchLexicalCandidates(String queryText, List<String> docTypes, int limit, LocalDateTime timeContext) {
        QueryWrapper<KnowledgeBase> wrapper = new QueryWrapper<KnowledgeBase>();
        wrapper.eq("status", 1);
        if (!CollectionUtils.isEmpty(docTypes)) {
            wrapper.in("doc_type", docTypes);
        }
        wrapper.and(w -> w.isNull("effective_from").or().le("effective_from", timeContext));
        wrapper.and(w -> w.isNull("effective_to").or().ge("effective_to", timeContext));
        if (StringUtils.hasText(queryText)) {
            wrapper.and(w -> w.like("title", queryText)
                    .or().like("content", queryText)
                    .or().like("keywords", queryText)
                    .or().like("related_questions", queryText));
        }
        wrapper.orderByDesc("helpful_count")
                .orderByDesc("view_count")
                .orderByDesc("updated_at")
                .last("limit " + Math.max(limit, 10));
        return knowledgeBaseMapper.selectList(wrapper);
    }

    private List<CandidateScore> searchVectorCandidates(String queryText,
                                                        KnowledgeRetrievalRequest request,
                                                        List<String> docTypes,
                                                        int limit,
                                                        LocalDateTime timeContext) {
        float[] queryVector = embeddingService.embed(queryText);
        List<KnowledgeVector> allVectors = knowledgeVectorMapper.selectList(null);
        List<CandidateScore> result = new ArrayList<CandidateScore>();
        for (KnowledgeVector vector : allVectors) {
            if (vector == null || vector.getKnowledgeId() == null || !StringUtils.hasText(vector.getVectorData())) {
                continue;
            }
            KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(vector.getKnowledgeId());
            if (!matchesKnowledgeFilters(knowledgeBase, request, docTypes, timeContext)) {
                continue;
            }
            CandidateScore candidate = new CandidateScore();
            candidate.knowledgeBase = knowledgeBase;
            candidate.vectorScore = cosineSimilarity(queryVector, knowledgeService.jsonToFloatArray(vector.getVectorData()));
            result.add(candidate);
        }
        result.sort((left, right) -> Double.compare(right.vectorScore, left.vectorScore));
        if (result.size() <= limit) {
            return result;
        }
        return new ArrayList<CandidateScore>(result.subList(0, limit));
    }

    private boolean matchesKnowledgeFilters(KnowledgeBase knowledgeBase,
                                            KnowledgeRetrievalRequest request,
                                            List<String> docTypes,
                                            LocalDateTime timeContext) {
        if (knowledgeBase == null || knowledgeBase.getStatus() == null || knowledgeBase.getStatus() != 1) {
            return false;
        }
        if (knowledgeBase.getEffectiveFrom() != null && knowledgeBase.getEffectiveFrom().isAfter(timeContext)) {
            return false;
        }
        if (knowledgeBase.getEffectiveTo() != null && knowledgeBase.getEffectiveTo().isBefore(timeContext)) {
            return false;
        }
        if (!CollectionUtils.isEmpty(docTypes) && !docTypes.contains(resolveDocType(knowledgeBase))) {
            return false;
        }
        if (StringUtils.hasText(request.getEntityType())
                && StringUtils.hasText(knowledgeBase.getEntityType())
                && !request.getEntityType().equalsIgnoreCase(knowledgeBase.getEntityType())) {
            return false;
        }
        if (!CollectionUtils.isEmpty(request.getEntityIds()) && StringUtils.hasText(knowledgeBase.getEntityId())
                && !request.getEntityIds().contains(knowledgeBase.getEntityId())) {
            return false;
        }
        if (!matchesAnyLongToken(request.getCityIds(), knowledgeBase.getCityIds())) {
            return false;
        }
        if (!matchesAnyLongToken(request.getCategoryIds(), knowledgeBase.getCategoryIds())) {
            return false;
        }
        return true;
    }

    private CandidateScore getOrCreateCandidate(Map<Long, CandidateScore> candidates, KnowledgeBase knowledgeBase) {
        Long key = knowledgeBase.getId();
        CandidateScore candidate = candidates.get(key);
        if (candidate == null) {
            candidate = new CandidateScore();
            candidate.knowledgeBase = knowledgeBase;
            candidates.put(key, candidate);
        }
        return candidate;
    }

    private double computeLexicalScore(KnowledgeBase knowledgeBase, String queryText) {
        if (!StringUtils.hasText(queryText) || knowledgeBase == null) {
            return 0.0D;
        }
        String loweredQuery = queryText.toLowerCase(Locale.ROOT);
        double score = 0.0D;
        if (containsIgnoreCase(knowledgeBase.getTitle(), loweredQuery)) {
            score += 0.45D;
        }
        if (containsIgnoreCase(knowledgeBase.getKeywords(), loweredQuery)) {
            score += 0.25D;
        }
        if (containsIgnoreCase(knowledgeBase.getRelatedQuestions(), loweredQuery)) {
            score += 0.20D;
        }
        if (containsIgnoreCase(knowledgeBase.getContent(), loweredQuery)) {
            score += 0.10D;
        }
        for (String token : splitQueryTokens(queryText)) {
            if (containsIgnoreCase(knowledgeBase.getTitle(), token)) {
                score += 0.08D;
            }
            if (containsIgnoreCase(knowledgeBase.getKeywords(), token)) {
                score += 0.05D;
            }
        }
        return Math.min(score, 1.0D);
    }

    private double computeFinalScore(CandidateScore candidate,
                                     String queryText,
                                     KnowledgeRetrievalRequest request,
                                     List<String> docTypes) {
        double score = candidate.lexicalScore * 0.60D + Math.max(candidate.vectorScore, 0.0D) * 0.30D;
        if (!CollectionUtils.isEmpty(docTypes)
                && docTypes.contains(resolveDocType(candidate.knowledgeBase))) {
            score += 0.08D;
        }
        if (request.isNeedRerank()) {
            score += rerankBoost(candidate.knowledgeBase, queryText, request.getPurpose());
        }
        return score;
    }

    private double rerankBoost(KnowledgeBase knowledgeBase, String queryText, String purpose) {
        double score = 0.0D;
        if (StringUtils.hasText(queryText)) {
            if (equalsIgnoreCase(knowledgeBase.getTitle(), queryText)) {
                score += 0.12D;
            }
            if (containsIgnoreCase(knowledgeBase.getRelatedQuestions(), queryText.toLowerCase(Locale.ROOT))) {
                score += 0.08D;
            }
        }
        String loweredDocType = resolveDocType(knowledgeBase);
        if ("faq_direct".equals(purpose) && "faq".equals(loweredDocType)) {
            score += 0.12D;
        }
        if ("knowledge_enrichment".equals(purpose)
                && ("usage_notice".equals(loweredDocType)
                || "play_guide".equals(loweredDocType)
                || "refund_rule".equals(loweredDocType)
                || "reservation_rule".equals(loweredDocType)
                || "guide".equals(loweredDocType)
                || "rule".equals(loweredDocType))) {
            score += 0.10D;
        }
        return score;
    }

    private List<String> resolveDocTypes(KnowledgeRetrievalRequest request) {
        Set<String> docTypes = new LinkedHashSet<String>();
        if (request != null && !CollectionUtils.isEmpty(request.getDocTypes())) {
            for (String docType : request.getDocTypes()) {
                if (StringUtils.hasText(docType)) {
                    docTypes.add(docType.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        if (!docTypes.isEmpty()) {
            return new ArrayList<String>(docTypes);
        }

        String purpose = request == null ? null : request.getPurpose();
        String taskType = request == null ? null : request.getTaskType();
        if ("faq_direct".equals(purpose) || "faq_query".equals(taskType)) {
            docTypes.addAll(Arrays.asList("faq", "refund_rule", "reservation_rule", "usage_notice", "rule", "guide"));
        } else if ("knowledge_enrichment".equals(purpose) || "mixed_search_knowledge".equals(taskType)) {
            docTypes.addAll(Arrays.asList("usage_notice", "play_guide", "refund_rule", "reservation_rule", "guide", "rule", "faq"));
        }
        return new ArrayList<String>(docTypes);
    }

    private boolean hasMetadataFilters(KnowledgeRetrievalRequest request) {
        return request != null
                && (StringUtils.hasText(request.getEntityType())
                || !CollectionUtils.isEmpty(request.getEntityIds())
                || !CollectionUtils.isEmpty(request.getCityIds())
                || !CollectionUtils.isEmpty(request.getCategoryIds()));
    }

    private List<String> splitQueryTokens(String queryText) {
        if (!StringUtils.hasText(queryText)) {
            return new ArrayList<String>();
        }
        String normalized = queryText.replace('，', ' ')
                .replace('。', ' ')
                .replace('？', ' ')
                .replace('、', ' ')
                .replace(',', ' ')
                .replace('.', ' ');
        String[] parts = normalized.split("\\s+");
        List<String> result = new ArrayList<String>();
        for (String part : parts) {
            if (StringUtils.hasText(part) && part.length() >= 2) {
                result.add(part.toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }

    private boolean containsIgnoreCase(String source, String loweredQuery) {
        if (!StringUtils.hasText(source) || !StringUtils.hasText(loweredQuery)) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(loweredQuery);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String resolveDocType(KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null) {
            return "";
        }
        if (StringUtils.hasText(knowledgeBase.getDocType())) {
            return knowledgeBase.getDocType().trim().toLowerCase(Locale.ROOT);
        }
        return safeText(knowledgeBase.getCategory()).toLowerCase(Locale.ROOT);
    }

    private boolean matchesAnyLongToken(List<Long> expectedIds, String csvValue) {
        if (CollectionUtils.isEmpty(expectedIds)) {
            return true;
        }
        if (!StringUtils.hasText(csvValue)) {
            return false;
        }
        Set<String> tokens = parseCsvTokens(csvValue);
        for (Long expectedId : expectedIds) {
            if (expectedId != null && tokens.contains(String.valueOf(expectedId))) {
                return true;
            }
        }
        return false;
    }

    private Set<String> parseCsvTokens(String csvValue) {
        Set<String> tokens = new LinkedHashSet<String>();
        if (!StringUtils.hasText(csvValue)) {
            return tokens;
        }
        String[] parts = csvValue.split(",");
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                tokens.add(part.trim());
            }
        }
        return tokens;
    }

    private int compareNullable(Integer left, Integer right) {
        int leftValue = left == null ? 0 : left;
        int rightValue = right == null ? 0 : right;
        return Integer.compare(leftValue, rightValue);
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0.0D;
        }
        double dotProduct = 0.0D;
        double normA = 0.0D;
        double normB = 0.0D;
        for (int index = 0; index < a.length; index++) {
            dotProduct += a[index] * b[index];
            normA += a[index] * a[index];
            normB += b[index] * b[index];
        }
        if (normA == 0.0D || normB == 0.0D) {
            return 0.0D;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static class CandidateScore {
        private KnowledgeBase knowledgeBase;
        private double lexicalScore;
        private double vectorScore;
        private double finalScore;
    }
}
