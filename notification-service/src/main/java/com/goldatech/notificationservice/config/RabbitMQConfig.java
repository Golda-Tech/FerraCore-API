package com.goldatech.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${notification.exchange}")
    private String notificationExchangeName;

    @Value("${notification.payment.queue}")
    private String paymentQueueName;
    @Value("${notification.collection.queue}")
    private String collectionQueueName;
    @Value("${notification.auth.queue}")
    private String authQueueName;

    @Value("${notification.payment.routing-key}")
    private String paymentRoutingKey;
    @Value("${notification.collection.routing-key}")
    private String collectionRoutingKey;
    @Value("${notification.auth.routing-key}")
    private String authRoutingKey;

    @Value("${notification.otp.queue}")
    private String otpQueueName;
    @Value("${notification.otp.routing-key}")
    private String otpRoutingKey;

    @Bean
    DirectExchange notificationExchange() {
        return new DirectExchange(notificationExchangeName);
    }

    @Bean
    Queue paymentQueue() {
        return QueueBuilder.durable(paymentQueueName).build();
    }

    @Bean
    Queue collectionQueue() {
        return QueueBuilder.durable(collectionQueueName).build();
    }

    @Bean
    Queue authQueue() {
        return QueueBuilder.durable(authQueueName).build();
    }

    @Bean
    Queue otpQueue() {
        return QueueBuilder.durable(otpQueueName).build();
    }

    @Bean
    Binding paymentBinding(DirectExchange notificationExchange, Queue paymentQueue) {
        return BindingBuilder.bind(paymentQueue).to(notificationExchange).with(paymentRoutingKey);
    }

    @Bean
    Binding collectionBinding(DirectExchange notificationExchange, Queue collectionQueue) {
        return BindingBuilder.bind(collectionQueue).to(notificationExchange).with(collectionRoutingKey);
    }

    @Bean
    Binding authBinding(DirectExchange notificationExchange, Queue authQueue) {
        return BindingBuilder.bind(authQueue).to(notificationExchange).with(authRoutingKey);
    }

    @Bean
    Binding otpBinding(DirectExchange notificationExchange, Queue otpQueue) {
        return BindingBuilder.bind(otpQueue).to(notificationExchange).with(otpRoutingKey);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper){
        final var rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonConverter(objectMapper));
        return rabbitTemplate;
    }

    @Bean
    public Jackson2JsonMessageConverter jacksonConverter(ObjectMapper mapper){
        return new Jackson2JsonMessageConverter(mapper);
    }
}