package com.example.demo.demos.Notification.Service;

import com.example.demo.demos.Notification.Config.RabbitNotificationConfig;
import com.example.demo.demos.Notification.Pojo.NotificationMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 通知消息发送器：业务侧调用后将消息推送到 MQ。
 */
@Component
public class NotificationSender {

    private final RabbitTemplate rabbitTemplate;

    public NotificationSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(NotificationMessage message) {
        if (message == null) {
            return;
        }
//        msgId 未设置，使用 UUID.randomUUID() 生成唯一 ID，确保每条消息可追溯
        if (!StringUtils.hasText(message.getMsgId())) {
            message.setMsgId(UUID.randomUUID().toString());
        }
        //时间戳补全：若 createdAt 未设置，填充当前时间（LocalDateTime.now()），用于后续时效性判断或审计
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }
//  动态生成路由键。该方法的实现可能基于消息类型、优先级或其他业务规则
// （如 message.getType()），决定消息应被分发到哪个队列。此设计允许灵活扩展路由策略
        String routingKey = resolveRoutingKey(message);
        rabbitTemplate.convertAndSend(RabbitNotificationConfig.EXCHANGE_NOTIF_TOPIC, routingKey, message, m -> {
            if (message.getTtlMs() != null) {
                m.getMessageProperties().setExpiration(String.valueOf(message.getTtlMs()));
            }
            return m;
        });
    }

    /**
     * 根据通知消息的类型和属性生成对应的路由键
     * 用于决定消息在RabbitMQ中的路由策略
     *
     * @param message 通知消息对象，包含目标类型、用户ID、群组键等路由信息
     * @return 生成的路由键字符串，格式遵循 notif.&lt;target_type&gt;.&lt;identifier&gt; 规范
     */
    private String resolveRoutingKey(NotificationMessage message) {
        // 处理群组类型消息：当目标类型为GROUP且存在群组键时
        // 生成格式为 notif.group.{groupKey} 的路由键
        if ("GROUP".equalsIgnoreCase(message.getTargetType()) && StringUtils.hasText(message.getGroupKey())) {
            return "notif.group." + message.getGroupKey();
        }

        // 处理用户类型消息：当目标类型不是BROADCAST时
        // 使用用户ID作为路由标识，若用户ID为空则使用默认值0L
        // 生成格式为 notif.user.{uid} 的路由键
        if (!"BROADCAST".equalsIgnoreCase(message.getTargetType())) {
            // 默认按用户路由
            Long uid = message.getTargetUserId() == null ? 0L : message.getTargetUserId();
            return "notif.user." + uid;
        }

        // 处理广播类型消息：直接返回预设的广播路由键常量
        // 该常量定义在 RabbitNotificationConfig.RK_BROADCAST 中
        return RabbitNotificationConfig.RK_BROADCAST;
    }
}
