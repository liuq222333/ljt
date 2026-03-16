package com.example.demo.demos.Agent.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_feedback")
public class ChatFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private String sessionId;

    @TableField("user_question")
    private String userQuestion;

    @TableField("ai_answer")
    private String aiAnswer;

    @TableField("knowledge_ids")
    private String knowledgeIds;

    @TableField("is_helpful")
    private Integer isHelpful;

    @TableField("feedback_text")
    private String feedbackText;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
