package com.chatsever.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình RabbitMQ phía consumer (notification-service).
 * Exchange: chat.exchange (topic) → Queue: chat.notification.queue → Routing: notify.#
 * Copy pattern từ log-service, đổi queue name + routing key.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "chat.exchange";
    public static final String QUEUE = "chat.notification.queue";
    public static final String ROUTING_KEY_PATTERN = "notify.#";

    // Khai báo Topic Exchange — durable, không auto-delete
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // Queue notification — durable (không mất khi RabbitMQ restart)
    @Bean
    public Queue notificationQueue() {
        return new Queue(QUEUE, true);
    }

    // Bind queue vào exchange theo pattern "notify.#"
    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(notificationQueue).to(chatExchange).with(ROUTING_KEY_PATTERN);
    }

    // Converter JSON ↔ Object cho RabbitMQ, hỗ trợ LocalDateTime
    @Bean
    public MessageConverter jacksonConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }
}
