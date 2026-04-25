package com.chatsever.log.config;

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
 * Khai bao Topic Exchange + Queue + Binding phia consumer.
 * Idempotent voi declaration phia producer (messaging-service).
 *
 * Spec: doc/04_giao_thuc_truyen_thong.md § 4.6
 *   Exchange: chat.exchange (topic)
 *   Queue:    chat.log.queue (durable)
 *   Routing:  log.#
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "chat.exchange";
    public static final String QUEUE = "chat.log.queue";
    public static final String ROUTING_KEY_PATTERN = "log.#";

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue logQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding logBinding(Queue logQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(logQueue).to(chatExchange).with(ROUTING_KEY_PATTERN);
    }

    @Bean
    public MessageConverter jacksonConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }
}
