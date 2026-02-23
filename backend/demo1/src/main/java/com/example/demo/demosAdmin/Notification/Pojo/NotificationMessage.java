package com.example.demo.demosAdmin.Notification.Pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 发送到 MQ 的通知消息体。
 */
@Data
public class NotificationMessage {
    private String msgId;
    private String kind;
    private String title;
    private String content;
    private String actionUrl;
    private Long targetUserId;
    private String targetType; // USER / BROADCAST / GROUP
    private String groupKey;
    private Integer priority;
    private Long ttlMs;
    private Map<String, Object> payload;
    private LocalDateTime createdAt;
}
