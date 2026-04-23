package com.example.demo.demos.search.service;

import com.example.demo.demos.search.entity.QueryExpandDict;
import com.example.demo.demos.search.entity.SearchCategory;
import com.example.demo.demos.search.mapper.QueryExpandDictMapper;
import com.example.demo.demos.search.mapper.SearchCategoryMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class ProductQueryExpansionService {

    private static final Map<String, List<String>> BUILTIN_TERM_EXPANSIONS = createBuiltinTermExpansions();

    private final QueryExpandDictMapper queryExpandDictMapper;
    private final SearchCategoryMapper searchCategoryMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductQueryExpansionService(QueryExpandDictMapper queryExpandDictMapper,
                                        SearchCategoryMapper searchCategoryMapper) {
        this.queryExpandDictMapper = queryExpandDictMapper;
        this.searchCategoryMapper = searchCategoryMapper;
    }

    public ExpansionPlan expand(String keyword, String categoryText) {
        ExpansionPlan plan = new ExpansionPlan();
        String normalizedKeyword = normalizeToken(keyword);
        String normalizedCategory = normalizeToken(categoryText);

        if (StringUtils.hasText(normalizedKeyword)) {
            plan.getKeywords().add(normalizedKeyword);
            appendExpandedTerms(plan, normalizedKeyword);
            appendMatchedCategories(plan, normalizedKeyword);
        }
        if (StringUtils.hasText(normalizedCategory) && !normalizedCategory.equals(normalizedKeyword)) {
            plan.getKeywords().add(normalizedCategory);
            appendExpandedTerms(plan, normalizedCategory);
            appendMatchedCategories(plan, normalizedCategory);
        }

        deduplicate(plan.getKeywords());
        deduplicate(plan.getCategoryIds());
        return plan;
    }

    private void appendExpandedTerms(ExpansionPlan plan, String term) {
        if (!StringUtils.hasText(term)) {
            return;
        }

        QueryExpandDict dictionaryEntry = null;
        try {
            if (queryExpandDictMapper != null) {
                dictionaryEntry = queryExpandDictMapper.selectByQueryTerm(term);
            }
        } catch (RuntimeException ignore) {
            // Expansion is best effort and should not break the main path.
        }

        if (dictionaryEntry != null) {
            plan.getKeywords().addAll(readStringList(dictionaryEntry.getExpandTermsJson()));
            plan.getCategoryIds().addAll(readLongList(dictionaryEntry.getExpandCategoriesJson()));
        }

        List<String> builtinTerms = BUILTIN_TERM_EXPANSIONS.get(term);
        if (!CollectionUtils.isEmpty(builtinTerms)) {
            plan.getKeywords().addAll(builtinTerms);
        }
    }

    private void appendMatchedCategories(ExpansionPlan plan, String term) {
        if (!StringUtils.hasText(term) || searchCategoryMapper == null) {
            return;
        }
        try {
            List<SearchCategory> matchedCategories = searchCategoryMapper.selectByName(term);
            if (CollectionUtils.isEmpty(matchedCategories)) {
                return;
            }
            for (SearchCategory matchedCategory : matchedCategories) {
                if (matchedCategory != null && matchedCategory.getCategoryId() != null) {
                    plan.getCategoryIds().add(matchedCategory.getCategoryId());
                }
            }
        } catch (RuntimeException ignore) {
            // Category lookup is also best effort.
        }
    }

    private List<String> readStringList(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return new ArrayList<String>();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {
            });
        } catch (Exception ignore) {
            return new ArrayList<String>();
        }
    }

    private List<Long> readLongList(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return new ArrayList<Long>();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<Long>>() {
            });
        } catch (Exception ignore) {
            return new ArrayList<Long>();
        }
    }

    private String normalizeToken(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim()
                .replace("在售卖", "")
                .replace("在卖", "")
                .replace("附近", "")
                .replace("附件", "")
                .replaceAll("\\s+", "");
    }

    private static <T> void deduplicate(List<T> values) {
        if (CollectionUtils.isEmpty(values)) {
            return;
        }
        LinkedHashSet<T> deduplicated = new LinkedHashSet<T>(values);
        values.clear();
        values.addAll(deduplicated);
    }

    private static Map<String, List<String>> createBuiltinTermExpansions() {
        Map<String, List<String>> expansions = new LinkedHashMap<String, List<String>>();
        expansions.put("水果", Arrays.asList(
                "草莓", "樱桃", "苹果", "香蕉",
                "橙子", "蓝莓", "葡萄", "西瓜"
        ));
        expansions.put("果蔬", Arrays.asList(
                "水果", "蔬菜", "草莓", "苹果",
                "番茄", "黄瓜"
        ));
        return expansions;
    }

    public static final class ExpansionPlan {
        private final List<String> keywords = new ArrayList<String>();
        private final List<Long> categoryIds = new ArrayList<Long>();

        public List<String> getKeywords() {
            return keywords;
        }

        public List<Long> getCategoryIds() {
            return categoryIds;
        }
    }
}
