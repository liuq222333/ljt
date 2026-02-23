package com.example.demo.demos.LocalActive.Dao;

import com.example.demo.demos.LocalActive.DTO.LocalStoryDetail;
import com.example.demo.demos.LocalActive.DTO.LocalStoryListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LocalActivityStoryMapper {

    List<LocalStoryListItem> listStories(@Param("keyword") String keyword,
                                         @Param("visibility") String visibility,
                                         @Param("limit") int limit,
                                         @Param("offset") int offset);

    LocalStoryDetail findById(@Param("id") Long id);

    int insertStory(com.example.demo.demos.LocalActive.Pojo.LocalActivityStory story);
}
