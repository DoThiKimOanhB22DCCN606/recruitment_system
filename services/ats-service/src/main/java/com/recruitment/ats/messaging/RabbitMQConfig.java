package com.recruitment.ats.messaging;

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
    public static final String ATS_OFFER_EXPIRED_QUEUE = "ats.offer.expired.queue";
    public static final String ATS_OFFER_EXPIRED_DLQ = "ats.offer.expired.dlq";
    public static final String ROUTING_KEY_OFFER_EXPIRED = "offer.expired";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Queue atsOfferExpiredDlq() {
        return QueueBuilder.durable(ATS_OFFER_EXPIRED_DLQ).build();
    }

    @Bean
    public Binding atsOfferExpiredDlqBinding(@Qualifier("atsOfferExpiredDlq") Queue atsOfferExpiredDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(atsOfferExpiredDlq).to(deadLetterExchange).with(ATS_OFFER_EXPIRED_DLQ);
    }

    @Bean
    public Queue atsOfferExpiredQueue() {
        return QueueBuilder.durable(ATS_OFFER_EXPIRED_QUEUE)
                .deadLetterExchange(DLX_NAME)
                .deadLetterRoutingKey(ATS_OFFER_EXPIRED_DLQ)
                .build();
    }

    @Bean
    public Binding bindingAtsOfferExpired(@Qualifier("atsOfferExpiredQueue") Queue atsOfferExpiredQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(atsOfferExpiredQueue).to(eventsExchange).with(ROUTING_KEY_OFFER_EXPIRED);
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
