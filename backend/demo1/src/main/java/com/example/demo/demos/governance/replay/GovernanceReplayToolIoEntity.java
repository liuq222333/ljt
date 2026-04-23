package com.example.demo.demos.governance.replay;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("governance_replay_tool_ios")
public class GovernanceReplayToolIoEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("replay_record_id")
    private Long replayRecordId;

    @TableField("step_order")
    private Integer stepOrder;

    @TableField("step_id")
    private String stepId;

    @TableField("tool_name")
    private String toolName;

    private String purpose;

    @TableField("output_key")
    private String outputKey;

    @TableField("optional_step")
    private Integer optionalStep;

    @TableField("execution_status")
    private String executionStatus;

    @TableField("input_json")
    private String inputJson;

    @TableField("output_json")
    private String outputJson;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
