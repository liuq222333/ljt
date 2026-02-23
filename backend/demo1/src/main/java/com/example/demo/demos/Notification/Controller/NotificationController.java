package com.example.demo.demos.Notification.Controller;

import com.example.demo.demos.Notification.Pojo.Notification;
import com.example.demo.demos.Notification.Pojo.NotificationMessage;
import com.example.demo.demos.Notification.Service.NotificationService;
import com.example.demo.demos.generic.Resp;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "添加通知公告")
    @PostMapping("/add")
    public Resp<Void> add(@RequestBody Notification notification){
        return notificationService.addNotification(notification);
    }

    @Operation(summary="获取通知公告列表")
    @GetMapping
    public Resp<java.util.List<Notification>> getNotifications(@RequestParam Long userId,
                                                               @RequestParam(required = false) Integer readStatus) {
        return Resp.success(notificationService.list(userId, readStatus));
    }

    @Operation(summary = "标记单条已读")
    @PatchMapping("/{id}/read")
    public Resp<Void> markRead(@PathVariable Long id, @RequestParam Long userId) {
        return notificationService.markRead(id, userId);
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/markAllRead")
    public Resp<Void> markAllRead(@RequestParam Long userId) {
        return notificationService.markAllRead(userId);
    }

    @Operation(summary = "未读数量")
    @GetMapping("/unreadCount")
    public Resp<Integer> unreadCount(@RequestParam Long userId) {
        return Resp.success(notificationService.countUnread(userId));
    }

    @Operation(summary = "未读数量快照（兼容文档路径）")
    @GetMapping("/unread")
    public Resp<Integer> unread(@RequestParam Long userId) {
        return Resp.success(notificationService.countUnread(userId));
    }

    @Operation(summary = "通过 MQ 异步发送通知")
    @PostMapping("/send")
    public Resp<Void> send(@RequestBody NotificationMessage message) {
        return notificationService.sendAsync(message);
    }

    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    public Resp<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        return notificationService.delete(id, userId);
    }

}
