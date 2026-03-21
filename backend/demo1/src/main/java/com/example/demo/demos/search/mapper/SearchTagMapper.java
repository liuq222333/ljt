package com.example.demo.demos.search.mapper;

import com.example.demo.demos.search.entity.SearchTag;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 搜索标签 Mapper — 对应 tag 表。
 */
@Mapper
public interface SearchTagMapper {

    @Insert("INSERT INTO tag (tag_id, tag_name, tag_type, status) " +
            "VALUES (#{tagId}, #{tagName}, #{tagType}, #{status})")
    int insert(SearchTag tag);

    @Select("SELECT tag_id, tag_name, tag_type, status, created_at, updated_at " +
            "FROM tag WHERE tag_id = #{tagId}")
    SearchTag selectById(@Param("tagId") Long tagId);

    @Select("SELECT tag_id, tag_name, tag_type, status, created_at, updated_at " +
            "FROM tag WHERE tag_name = #{tagName} AND status = 'enabled'")
    List<SearchTag> selectByName(@Param("tagName") String tagName);

    @Select("SELECT tag_id, tag_name, tag_type, status, created_at, updated_at " +
            "FROM tag WHERE tag_type = #{tagType} AND status = 'enabled' ORDER BY tag_name")
    List<SearchTag> selectByType(@Param("tagType") String tagType);

    @Select("SELECT tag_id, tag_name, tag_type, status, created_at, updated_at " +
            "FROM tag WHERE status = 'enabled' ORDER BY tag_type, tag_name")
    List<SearchTag> selectAllEnabled();

    @Update("UPDATE tag SET tag_name = #{tagName}, tag_type = #{tagType}, status = #{status} " +
            "WHERE tag_id = #{tagId}")
    int update(SearchTag tag);
}
