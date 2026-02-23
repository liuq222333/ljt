package com.example.demo.demos.Agent.Dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.demos.Agent.Entity.ApiRoute;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus 的 Mapper，用于操作 api_routes 表。
 */
@Mapper
public interface ApiRouteMapper extends BaseMapper<ApiRoute> {
}
