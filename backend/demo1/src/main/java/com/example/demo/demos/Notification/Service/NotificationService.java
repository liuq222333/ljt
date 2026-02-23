package com.example.demo.demos.Notification.Service;

import com.example.demo.demos.Notification.Pojo.Notification;
import com.example.demo.demos.Notification.Pojo.NotificationMessage;
import com.example.demo.demos.generic.Resp;
import org.springframework.stereotype.Service;

@Service
public interface NotificationService {
    java.util.List<Notification> list(Long userId, Integer readStatus);

    Resp<Void> addNotification(Notification notification);

    Resp<Void> markRead(Long id, Long userId);

    Resp<Void> markAllRead(Long userId);

    Integer countUnread(Long userId);

    /**
     * 通过 MQ 发送通知
     */
    Resp<Void> sendAsync(NotificationMessage message);

    /**
     * 删除通知
     */
    Resp<Void> delete(Long id, Long userId);
}
