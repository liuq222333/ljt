package com.example.demo.demosAdmin.ApiManagement.Dao;

import com.example.demo.demosAdmin.ApiManagement.Pojo.ApiRoute;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ApiManagementMapper {

    @Select("<script>" +
            "SELECT * FROM api_routes " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (resource LIKE CONCAT('%',#{keyword},'%') OR description LIKE CONCAT('%',#{keyword},'%') OR path_template LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "ORDER BY id DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<ApiRoute> listRoutes(@Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT count(*) FROM api_routes")
    long countRoutes();

    @Insert("INSERT INTO api_routes (resource, action, http_method, path_template, path_params, operation_type, description, enabled, query_schema, body_schema, created_at, updated_at) " +
            "VALUES (#{resource}, #{action}, #{httpMethod}, #{pathTemplate}, #{pathParams}, #{operationType}, #{description}, #{enabled}, #{querySchema}, #{bodySchema}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRoute(ApiRoute route);

    @Update("UPDATE api_routes SET resource=#{resource}, action=#{action}, http_method=#{httpMethod}, path_template=#{pathTemplate}, " +
            "path_params=#{pathParams}, operation_type=#{operationType}, description=#{description}, enabled=#{enabled}, " +
            "query_schema=#{querySchema}, body_schema=#{bodySchema}, updated_at=NOW() WHERE id=#{id}")
    int updateRoute(ApiRoute route);

    @Update("UPDATE api_routes SET enabled = #{enabled}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("enabled") Integer enabled);

    @Delete("DELETE FROM api_routes WHERE id = #{id}")
    int deleteRoute(@Param("id") Long id);
}
