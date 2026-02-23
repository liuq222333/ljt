package com.example.demo.demos.Notification.Pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Notification {

    /** 幂等键，消息唯一标识 */
    private String msgId;

    private Long id;

    /**
     * 接收通知的用户
     */
    private Long userId;

    /**
     * 通知标题，如：商品发布成功
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知类型，如：ITEM_PUBLISHED、EVENT_REMINDER
     */
    private String type;

    /**
     * 优先级，数值越大越靠前
     */
    private Integer priority;

    /**
     * 0=未读，1=已读
     */
    private Integer readStatus;

    /**
     * 阅读时间
     */
    private LocalDateTime readAt;

    /**
     * 点击通知后跳转的链接，可选
     */
    private String linkUrl;

    /**
     * 业务相关的扩展数据（JSON 字符串）
     */
    private String meta;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 过期时间（可选）
     */
    private LocalDateTime expireAt;
}
