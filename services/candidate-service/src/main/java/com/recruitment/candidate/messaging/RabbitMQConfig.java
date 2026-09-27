package com.recruitment.candidate.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "rms.events";
    public static final String DLX_NAME = "rms.dlx";
    public static final String USER_CREATED_QUEUE = "candidate.user.created.queue";
    public static final String USER_CREATED_DLQ = "candidate.user.created.dlq";
    public static final String ROUTING_KEY_USER_CREATED = "iam.user.created";

    public static final String USER_ANONYMIZED_QUEUE = "candidate.user.anonymized.queue";
    public static final String USER_ANONYMIZED_DLQ = "candidate.user.anonymized.dlq";
    public static final String ROUTING_KEY_USER_ANONYMIZED = "iam.user.anonymized";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Queue userCreatedDlq() {
        return QueueBuilder.durable(USER_CREATED_DLQ).build();
    }

    @Bean
    public Binding userCreatedDlqBinding(@Qualifier("userCreatedDlq") Queue userCreatedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(userCreatedDlq).to(deadLetterExchange).with(USER_CREATED_DLQ);
    }

    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder.durable(USER_CREATED_QUEUE)
                .deadLetterExchange(DLX_NAME)
                .deadLetterRoutingKey(USER_CREATED_DLQ)
                .build();
    }

    @Bean
    public Binding bindingUserCreated(@Qualifier("userCreatedQueue") Queue userCreatedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(userCreatedQueue).to(eventsExchange).with(ROUTING_KEY_USER_CREATED);
    }

    @Bean
    public Queue userAnonymizedDlq() {
        return QueueBuilder.durable(USER_ANONYMIZED_DLQ).build();
    }

    @Bean
    public Binding userAnonymizedDlqBinding(@Qualifier("userAnonymizedDlq") Queue userAnonymizedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(userAnonymizedDlq).to(deadLetterExchange).with(USER_ANONYMIZED_DLQ);
    }

    @Bean
    public Queue userAnonymizedQueue() {
        return QueueBuilder.durable(USER_ANONYMIZED_QUEUE)
                .deadLetterExchange(DLX_NAME)
                .deadLetterRoutingKey(USER_ANONYMIZED_DLQ)
                .build();
    }

    @Bean
    public Binding bindingUserAnonymized(@Qualifier("userAnonymizedQueue") Queue userAnonymizedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(userAnonymizedQueue).to(eventsExchange).with(ROUTING_KEY_USER_ANONYMIZED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
