package com.example.demo.demos.governance.replay;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("governance_replay_checkpoints")
public class GovernanceReplayCheckpointEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("replay_record_id")
    private Long replayRecordId;

    @TableField("checkpoint_order")
    private Integer checkpointOrder;

    @TableField("node_name")
    private String nodeName;

    @TableField("state_snapshot_json")
    private String stateSnapshotJson;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
