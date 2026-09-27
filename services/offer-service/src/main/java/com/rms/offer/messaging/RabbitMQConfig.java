package com.rms.offer.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "rms.events";
    public static final String DLX_NAME = "rms.dlx";
    public static final String OFFER_ACCEPTANCE_RETRY_QUEUE = "offer.acceptance.retry.queue";
    public static final String OFFER_ACCEPTANCE_RETRY_DLQ = "offer.acceptance.retry.dlq";
    public static final String OFFER_ACCEPTANCE_RETRY_ROUTING_KEY = "offer.acceptance.retry";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Queue offerAcceptanceRetryDlq() {
        return QueueBuilder.durable(OFFER_ACCEPTANCE_RETRY_DLQ).build();
    }

    @Bean
    public Binding offerAcceptanceRetryDlqBinding(@Qualifier("offerAcceptanceRetryDlq") Queue offerAcceptanceRetryDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(offerAcceptanceRetryDlq).to(deadLetterExchange).with(OFFER_ACCEPTANCE_RETRY_DLQ);
    }

    @Bean
    public Queue offerAcceptanceRetryQueue() {
        return QueueBuilder.durable(OFFER_ACCEPTANCE_RETRY_QUEUE)
                .deadLetterExchange(DLX_NAME)
                .deadLetterRoutingKey(OFFER_ACCEPTANCE_RETRY_DLQ)
                .build();
    }

    @Bean
    public Binding offerAcceptanceRetryBinding(@Qualifier("offerAcceptanceRetryQueue") Queue offerAcceptanceRetryQueue, TopicExchange exchange) {
        return BindingBuilder.bind(offerAcceptanceRetryQueue).to(exchange).with(OFFER_ACCEPTANCE_RETRY_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
