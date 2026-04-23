package com.example.demo.demos.search.mapper;

import com.example.demo.demos.search.model.ProductSearchQuery;
import org.apache.ibatis.jdbc.SQL;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Map;

public class ProductSearchSnapshotSqlProvider {

    public String buildSearchSql(Map<String, Object> params) {
        ProductSearchQuery query = (ProductSearchQuery) params.get("query");
        return buildBaseSql(query).toString()
                + buildOrderBy(query)
                + " LIMIT " + query.limit()
                + " OFFSET " + query.offset();
    }

    public String buildCountSql(Map<String, Object> params) {
        ProductSearchQuery query = (ProductSearchQuery) params.get("query");
        return buildBaseSql(query, true).toString();
    }

    private SQL buildBaseSql(ProductSearchQuery query) {
        return buildBaseSql(query, false);
    }

    private SQL buildBaseSql(ProductSearchQuery query, boolean countOnly) {
        ProductSearchQuery safeQuery = query == null ? new ProductSearchQuery() : query;
        SQL sql = new SQL();
        sql.SELECT(countOnly ? "COUNT(*)" : "*");
        sql.FROM("product_search_snapshot");
        if (Boolean.TRUE.equals(safeQuery.getSearchableOnly())) {
            sql.WHERE("searchable_status = 'searchable'");
        }
        if (StringUtils.hasText(safeQuery.getKeyword())) {
            sql.WHERE("(title LIKE CONCAT('%', #{query.keyword}, '%') "
                    + "OR summary_text LIKE CONCAT('%', #{query.keyword}, '%') "
                    + "OR category_name LIKE CONCAT('%', #{query.keyword}, '%'))");
        }
        if (safeQuery.getCategoryId() != null) {
            sql.WHERE("category_id = #{query.categoryId}");
        }
        if (safeQuery.getCityId() != null) {
            sql.WHERE("city_id = #{query.cityId}");
        }
        if (safeQuery.getDistrictId() != null) {
            sql.WHERE("district_id = #{query.districtId}");
        }
        if (safeQuery.getMinPrice() != null) {
            sql.WHERE("display_price >= #{query.minPrice}");
        }
        if (safeQuery.getMaxPrice() != null) {
            sql.WHERE("display_price <= #{query.maxPrice}");
        }
        if (!CollectionUtils.isEmpty(safeQuery.getTagNames())) {
            StringBuilder builder = new StringBuilder("(");
            for (int index = 0; index < safeQuery.getTagNames().size(); index++) {
                if (index > 0) {
                    builder.append(" OR ");
                }
                builder.append("tag_names LIKE CONCAT('%', #{query.tagNames[").append(index).append("]}, '%')");
            }
            builder.append(")");
            sql.WHERE(builder.toString());
        }
        return sql;
    }

    private String buildOrderBy(ProductSearchQuery query) {
        if (query == null || !StringUtils.hasText(query.getSortBy())) {
            return " ORDER BY recommend_score DESC, product_id DESC";
        }
        String sortBy = query.getSortBy().trim().toLowerCase();
        if ("price_asc".equals(sortBy)) {
            return " ORDER BY display_price ASC, product_id DESC";
        }
        if ("price_desc".equals(sortBy)) {
            return " ORDER BY display_price DESC, product_id DESC";
        }
        if ("updated_at".equals(sortBy)) {
            return " ORDER BY updated_at DESC, product_id DESC";
        }
        return " ORDER BY recommend_score DESC, product_id DESC";
    }
}
