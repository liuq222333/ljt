package com.example.demo.demosAdmin.Notification.Service;

import com.example.demo.demos.generic.Resp;
import com.example.demo.demosAdmin.Notification.Pojo.NotificationMessage;
import org.springframework.stereotype.Service;

@Service("adminNotificationService")
public interface AdminNotificationService {
    Resp<Void> addNotification(NotificationMessage message);
}
