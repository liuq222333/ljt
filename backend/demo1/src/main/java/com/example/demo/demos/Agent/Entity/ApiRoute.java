package com.example.demo.demos.Agent.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应数据库表 api_routes，记录代理允许调用的后端接口。
 * 通过该表可控制可调用的资源/动作、HTTP 方法、路径模板以及参数规范。
 */
@Data
@TableName("api_routes")
public class ApiRoute {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String resource;
    private String action;

    @TableField("http_method")
    private String httpMethod;

    @TableField("path_template")
    private String pathTemplate;

    @TableField("path_params")
    private String pathParams;

    @TableField("operation_type")
    private String operationType;

    private String description;

    private Integer enabled;

    @TableField("query_schema")
    private String querySchema;

    @TableField("body_schema")
    private String bodySchema;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
