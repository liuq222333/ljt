package com.example.demo.demosAdmin.Notification.Controller;

import com.example.demo.demos.generic.Resp;
import com.example.demo.demosAdmin.Notification.Service.AdminNotificationService;
import com.example.demo.demosAdmin.Notification.Pojo.NotificationMessage;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController("adminNotificationController")
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {
    private final AdminNotificationService notificationService;

    public AdminNotificationController(AdminNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "系统发布通知公告")
    @PostMapping("/add")
    public Resp<Void> add(@RequestBody NotificationMessage message){
        return notificationService.addNotification(message);
    }
}
