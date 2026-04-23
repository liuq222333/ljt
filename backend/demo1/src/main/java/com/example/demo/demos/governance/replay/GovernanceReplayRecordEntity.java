package com.example.demo.demos.governance.replay;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("governance_replay_records")
public class GovernanceReplayRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("request_id")
    private String requestId;

    @TableField("trace_id")
    private String traceId;

    @TableField("session_id")
    private String sessionId;

    @TableField("user_id")
    private String userId;

    @TableField("last_user_message")
    private String lastUserMessage;

    @TableField("task_type")
    private String taskType;

    @TableField("plan_type")
    private String planType;

    @TableField("answer_type")
    private String answerType;

    @TableField("error_code")
    private String errorCode;

    @TableField("failed_node")
    private String failedNode;

    private Integer degraded;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("request_json")
    private String requestJson;

    @TableField("state_json")
    private String stateJson;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
