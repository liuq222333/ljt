package com.example.demo.demos.search.mapper;

import com.example.demo.demos.search.entity.QueryExpandDict;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 查询扩展词典 Mapper — 对应 query_expand_dict 表。
 */
@Mapper
public interface QueryExpandDictMapper {

    @Insert("INSERT INTO query_expand_dict (query_term, expand_terms_json, expand_categories_json, " +
            "expand_tags_json, status) " +
            "VALUES (#{queryTerm}, #{expandTermsJson}, #{expandCategoriesJson}, " +
            "#{expandTagsJson}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QueryExpandDict dict);

    @Select("SELECT id, query_term, expand_terms_json, expand_categories_json, " +
            "expand_tags_json, status, updated_at, created_at " +
            "FROM query_expand_dict WHERE query_term = #{queryTerm} AND status = 'enabled'")
    QueryExpandDict selectByQueryTerm(@Param("queryTerm") String queryTerm);

    @Select("SELECT id, query_term, expand_terms_json, expand_categories_json, " +
            "expand_tags_json, status, updated_at, created_at " +
            "FROM query_expand_dict WHERE status = 'enabled' ORDER BY updated_at DESC")
    List<QueryExpandDict> selectAllEnabled();

    @Update("UPDATE query_expand_dict SET expand_terms_json = #{expandTermsJson}, " +
            "expand_categories_json = #{expandCategoriesJson}, expand_tags_json = #{expandTagsJson}, " +
            "status = #{status} WHERE id = #{id}")
    int update(QueryExpandDict dict);
}
