package com.example.demo.demos.LocalActive.Dao;

import com.example.demo.demos.LocalActive.Pojo.LocalActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LocalActivityHeatMapMapper {

    @Select("SELECT id, latitude, longitude, capacity FROM local_activity WHERE status = 'PUBLISHED'")
    List<LocalActivity> listPublished();
}
