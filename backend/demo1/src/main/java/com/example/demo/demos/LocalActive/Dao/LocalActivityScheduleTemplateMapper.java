package com.example.demo.demos.LocalActive.Dao;

import com.example.demo.demos.LocalActive.Pojo.LocalActivityScheduleTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LocalActivityScheduleTemplateMapper {
    int insertTemplate(LocalActivityScheduleTemplate template);
}
