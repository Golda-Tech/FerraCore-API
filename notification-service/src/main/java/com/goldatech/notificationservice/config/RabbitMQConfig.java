package com.goldatech.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_EXCHANGE = "notification_exchange";
    public static final String PAYMENT_QUEUE = "payments_queue";
    public static final String COLLECTION_QUEUE = "collections_queue";
    public static final String AUTH_QUEUE = "auth_queue";
    public static final String PAYMENT_ROUTING_KEY = "events.payment";
    public static final String COLLECTION_ROUTING_KEY = "events.collection";
    public static final String AUTH_ROUTING_KEY = "events.auth";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    public Queue collectionQueue() {
        return new Queue(COLLECTION_QUEUE, true);
    }

    @Bean
    public Queue authQueue() {
        return new Queue(AUTH_QUEUE, true);
    }

    @Bean
    public Binding paymentBinding(TopicExchange notificationExchange, Queue paymentQueue) {
        return BindingBuilder.bind(paymentQueue)
                .to(notificationExchange)
                .with(PAYMENT_ROUTING_KEY);
    }

    @Bean
    public Binding collectionBinding(TopicExchange notificationExchange, Queue collectionQueue) {
        return BindingBuilder.bind(collectionQueue)
                .to(notificationExchange)
                .with(COLLECTION_ROUTING_KEY);
    }

    @Bean
    public Binding authBinding(TopicExchange notificationExchange, Queue authQueue) {
        return BindingBuilder.bind(authQueue)
                .to(notificationExchange)
                .with(AUTH_ROUTING_KEY);
    }
}