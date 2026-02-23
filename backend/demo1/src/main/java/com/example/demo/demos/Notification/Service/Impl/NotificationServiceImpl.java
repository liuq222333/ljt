package com.example.demo.demos.Notification.Service.Impl;

import com.example.demo.demos.Notification.Dao.NotificationMapper;
import com.example.demo.demos.Notification.Pojo.Notification;
import com.example.demo.demos.Notification.Pojo.NotificationMessage;
import com.example.demo.demos.Notification.Service.NotificationSender;
import com.example.demo.demos.Notification.Service.NotificationService;
import com.example.demo.demos.generic.Resp;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationMapper notificationMapper;
    private final NotificationSender notificationSender;
    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String UNREAD_KEY_PREFIX = "notice:unread:";
    private static final Duration UNREAD_CACHE_TTL = Duration.ofDays(7);

    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   NotificationSender notificationSender,
                                   StringRedisTemplate redisTemplate,
                                   SimpMessagingTemplate messagingTemplate) {
        this.notificationMapper = notificationMapper;
        this.notificationSender = notificationSender;
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
    }

    //获取通知公告
    @Override
    public List<Notification> list(Long userId, Integer readStatus) {
        if (userId == null) {
            return java.util.Collections.emptyList();
        }
        return notificationMapper.listByUser(userId, readStatus);
    }

    @Override
    public Resp<Void> addNotification(Notification notification) {
        if (notification == null || notification.getUserId() == null) {
            return Resp.error(400, "userId/notification 不能为空");
        }
        notification.setReadStatus(notification.getReadStatus() == null ? 0 : notification.getReadStatus());
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        if (notification.getMsgId() == null) {
            notification.setMsgId(UUID.randomUUID().toString());
        }
        notificationMapper.insertNotification(notification);
        return Resp.success();
    }

    @Override
    public Resp<Void> markRead(Long id, Long userId) {
        if (id == null || userId == null) {
            return Resp.error(400, "id 或 userId 不能为空");
        }
        notificationMapper.markRead(id, userId);
//        删除缓存
        evictUnreadCache(userId);
        return Resp.success();
    }

    @Override
    public Resp<Void> markAllRead(Long userId) {
        if (userId == null) {
            return Resp.error(400, "userId 不能为空");
        }
        notificationMapper.markAllRead(userId);
        resetUnread(userId);
        return Resp.success();
    }

    @Override
    public Integer countUnread(Long userId) {
        if (userId == null) {
            return 0;
        }
        String key = UNREAD_KEY_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return Integer.parseInt(cached);
            } catch (NumberFormatException ignore) {
                // ignore parse error and fall through
            }
        }
        Integer count = notificationMapper.countUnread(userId);
        int unread = count == null ? 0 : count;
        redisTemplate.opsForValue().set(key, String.valueOf(unread), UNREAD_CACHE_TTL);
        return unread;
    }

    @Override
    public Resp<Void> sendAsync(NotificationMessage message) {
        if (message == null || message.getTargetUserId() == null) {
            return Resp.error(400, "targetUserId 不能为空");
        }
        notificationSender.send(message);
        return Resp.success();
    }

    @Override
    public Resp<Void> delete(Long id, Long userId) {
        if (id == null || userId == null) {
            return Resp.error(400, "id 或 userId 不能为空");
        }
        notificationMapper.delete(id, userId);
        evictUnreadCache(userId);
        return Resp.success();
    }

    //清除未读缓存数量使得下次读取缓存数量的时候直接从db中读取
    private void evictUnreadCache(Long userId) {
        try {
            redisTemplate.delete(UNREAD_KEY_PREFIX + userId);
        } catch (Exception ignore) {
        }
    }

    private void resetUnread(Long userId) {
        try {
            redisTemplate.opsForValue().set(UNREAD_KEY_PREFIX + userId, "0", UNREAD_CACHE_TTL);
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("userId", userId);
            payload.put("unread", 0);
            messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/notify", payload);
        } catch (Exception ignore) {
        }
    }
}
