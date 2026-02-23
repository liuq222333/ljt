package com.example.demo.demos.Notification.Config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 通知/公告的 RabbitMQ 拓扑配置。
 * 交换机：topic 模式，根据 routingKey 将消息分发到用户/分组/广播队列。
 */
@Configuration
@EnableRabbit
public class RabbitNotificationConfig {

    public static final String EXCHANGE_NOTIF_TOPIC = "notif.topic";
    public static final String EXCHANGE_NOTIF_DLX = "notif.dlx";

    public static final String QUEUE_USER = "notif.user";
    public static final String QUEUE_GROUP = "notif.group";
    public static final String QUEUE_BROADCAST = "notif.broadcast";
    public static final String QUEUE_RETRY = "notif.retry";
    public static final String QUEUE_DLX = "notif.dlx";

    public static final String RK_USER = "notif.user.*";
    public static final String RK_GROUP = "notif.group.*";
    public static final String RK_BROADCAST = "notif.broadcast";

    @Bean
    public TopicExchange notificationTopicExchange() {
        return new TopicExchange(EXCHANGE_NOTIF_TOPIC, true, false);
    }

    @Bean
    public DirectExchange notificationDlxExchange() {
        return new DirectExchange(EXCHANGE_NOTIF_DLX, true, false);
    }

    @Bean
    public Queue userQueue() {
        return QueueBuilderWithDlx.durable(QUEUE_USER, EXCHANGE_NOTIF_DLX);
    }

    @Bean
    public Queue groupQueue() {
        return QueueBuilderWithDlx.durable(QUEUE_GROUP, EXCHANGE_NOTIF_DLX);
    }

    @Bean
    public Queue broadcastQueue() {
        return QueueBuilderWithDlx.durable(QUEUE_BROADCAST, EXCHANGE_NOTIF_DLX);
    }

    @Bean
    public Queue retryQueue() {
        return QueueBuilderWithDlx.durable(QUEUE_RETRY, EXCHANGE_NOTIF_DLX);
    }

    @Bean
    public Queue dlq() {
        // 死信队列不再声明 DLX，避免循环转发
        return new Queue(QUEUE_DLX, true);
    }

    @Bean
    public Binding userBinding(Queue userQueue, TopicExchange notificationTopicExchange) {
        return BindingBuilder.bind(userQueue).to(notificationTopicExchange).with(RK_USER);
    }

    @Bean
    public Binding groupBinding(Queue groupQueue, TopicExchange notificationTopicExchange) {
        return BindingBuilder.bind(groupQueue).to(notificationTopicExchange).with(RK_GROUP);
    }

    @Bean
    public Binding broadcastBinding(Queue broadcastQueue, TopicExchange notificationTopicExchange) {
        return BindingBuilder.bind(broadcastQueue).to(notificationTopicExchange).with(RK_BROADCAST);
    }

    @Bean
    public Binding retryBinding(Queue retryQueue, TopicExchange notificationTopicExchange) {
        // 重试队列也挂在主交换机，routingKey 可按需传入
        return BindingBuilder.bind(retryQueue).to(notificationTopicExchange).with(QUEUE_RETRY);
    }

    @Bean
    public Binding dlqBinding(Queue dlq, DirectExchange notificationDlxExchange) {
        return BindingBuilder.bind(dlq).to(notificationDlxExchange).with(QUEUE_DLX);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * 简化带 DLX 的队列构造。
     */
    private static class QueueBuilderWithDlx {
        static Queue durable(String name, String dlx) {
            return org.springframework.amqp.core.QueueBuilder
                    .durable(name)
                    .withArgument("x-dead-letter-exchange", dlx)
                    .withArgument("x-dead-letter-routing-key", QUEUE_DLX)
                    .build();
        }
    }
}
