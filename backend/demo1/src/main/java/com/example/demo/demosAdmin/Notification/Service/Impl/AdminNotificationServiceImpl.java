package com.example.demo.demosAdmin.Notification.Service.Impl;

import com.example.demo.demos.Notification.Service.NotificationSender;
import com.example.demo.demos.generic.Resp;
import com.example.demo.demosAdmin.Notification.Pojo.NotificationMessage;
import com.example.demo.demosAdmin.Notification.Service.AdminNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service("adminNotificationService")
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final NotificationSender notificationSender;

    public AdminNotificationServiceImpl(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    @Override
    public Resp<Void> addNotification(NotificationMessage message) {
        if (message == null) {
            return Resp.error("消息体不能为空");
        }
        if (!StringUtils.hasText(message.getTitle()) || !StringUtils.hasText(message.getContent())) {
            return Resp.error("标题/内容不能为空");
        }
        if (!StringUtils.hasText(message.getTargetType())) {
            message.setTargetType("USER");
        }
        if (!StringUtils.hasText(message.getKind())) {
            message.setKind("SYSTEM");
        }
        if (!"BROADCAST".equalsIgnoreCase(message.getTargetType()) && message.getTargetUserId() == null) {
            return Resp.error("targetUserId 不能为空");
        }
        if (!StringUtils.hasText(message.getMsgId())) {
            message.setMsgId(UUID.randomUUID().toString());
        }
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }
        notificationSender.send(convert(message));
        return Resp.success();
    }

    private com.example.demo.demos.Notification.Pojo.NotificationMessage convert(NotificationMessage m) {
        com.example.demo.demos.Notification.Pojo.NotificationMessage target = new com.example.demo.demos.Notification.Pojo.NotificationMessage();
        target.setMsgId(m.getMsgId());
        target.setKind(m.getKind());
        target.setTitle(m.getTitle());
        target.setContent(m.getContent());
        target.setActionUrl(m.getActionUrl());
        target.setTargetUserId(m.getTargetUserId());
        target.setTargetType(m.getTargetType());
        target.setGroupKey(m.getGroupKey());
        target.setPriority(m.getPriority());
        target.setTtlMs(m.getTtlMs());
        target.setPayload(m.getPayload());
        target.setCreatedAt(m.getCreatedAt());
        return target;
    }
}
