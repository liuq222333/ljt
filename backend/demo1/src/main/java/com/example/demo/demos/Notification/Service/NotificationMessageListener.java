package com.example.demo.demos.Notification.Service;

import com.example.demo.demos.Notification.Config.RabbitNotificationConfig;
import com.example.demo.demos.Notification.Dao.NotificationMapper;
import com.example.demo.demos.Notification.Pojo.Notification;
import com.example.demo.demos.Notification.Pojo.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MQ 消费者：收到通知消息后落库，供前端查询。
 */
@Component
public class NotificationMessageListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationMessageListener.class);
    private static final String UNREAD_KEY_PREFIX = "notice:unread:";

    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationMessageListener(NotificationMapper notificationMapper,
                                       Jackson2ObjectMapperBuilder mapperBuilder,
                                       StringRedisTemplate redisTemplate,
                                       SimpMessagingTemplate messagingTemplate) {
        this.notificationMapper = notificationMapper;
        this.objectMapper = mapperBuilder.build();
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = {
            RabbitNotificationConfig.QUEUE_USER,
            RabbitNotificationConfig.QUEUE_GROUP,
            RabbitNotificationConfig.QUEUE_BROADCAST
    })
    public void onMessage(NotificationMessage message) {
        if (message == null) {
            return;
        }
        if (!StringUtils.hasText(message.getKind())) {
            message.setKind("SYSTEM");
        }
        Long userId = message.getTargetUserId();
        if (userId == null) {
            log.warn("Skip notification without target user, msgId={}", message.getMsgId());
            return;
        }
        // 幂等校验
        if (StringUtils.hasText(message.getMsgId())) {
            Notification existed = notificationMapper.findByMsgId(message.getMsgId(), userId);
            if (existed != null) {
                return;
            }
        }

        Notification po = new Notification();
        po.setMsgId(message.getMsgId());
        po.setUserId(userId);
        po.setTitle(message.getTitle());
        po.setContent(message.getContent());
        po.setType(message.getKind());
        po.setPriority(message.getPriority());
        po.setReadStatus(0);
        po.setLinkUrl(message.getActionUrl());
        po.setCreatedAt(message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now());
        if (message.getPayload() != null) {
            try {
                po.setMeta(objectMapper.writeValueAsString(message.getPayload()));
            } catch (Exception ignore) {
                po.setMeta(String.valueOf(message.getPayload()));
            }
        }
        notificationMapper.insertNotification(po);
        // 记录未读计数并推送增量
        try {
            String key = UNREAD_KEY_PREFIX + userId;
            redisTemplate.opsForValue().increment(key, 1);
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("userId", userId);
            payload.put("delta", 1);
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notify", payload);
        } catch (Exception e) {
            log.warn("Notify unread delta failed for userId={}", userId, e);
        }
    }
}
