package com.example.demo.demosAdmin.AdminCommunityMarket.Dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminCommunityMarketMapper {
    int deleteProduct(@Param("id") Long id);
}
