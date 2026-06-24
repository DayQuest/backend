package com.dayquest.common.config;

import com.dayquest.common.messaging.RabbitMQConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Common RabbitMQ configuration for producer services.
 */
@Configuration
public class RabbitMQProducerConfig {

    // Notification Exchange
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(RabbitMQConstants.NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(RabbitMQConstants.EMAIL_QUEUE, true);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(emailQueue).to(notificationExchange).with(RabbitMQConstants.EMAIL_ROUTING_KEY);
    }

    // User Exchange - for user lifecycle events
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(RabbitMQConstants.USER_EXCHANGE, true, false);
    }

    @Bean
    public Queue userDeletedVideoQueue() {
        return new Queue(RabbitMQConstants.USER_DELETED_VIDEO_QUEUE, true);
    }

    @Bean
    public Queue userDeletedQuestQueue() {
        return new Queue(RabbitMQConstants.USER_DELETED_QUEST_QUEUE, true);
    }

    @Bean
    public Queue userDeletedSocialQueue() {
        return new Queue(RabbitMQConstants.USER_DELETED_SOCIAL_QUEUE, true);
    }

    @Bean
    public Binding userDeletedVideoBinding(Queue userDeletedVideoQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedVideoQueue).to(userExchange).with(RabbitMQConstants.USER_DELETED_ROUTING_KEY);
    }

    @Bean
    public Binding userDeletedQuestBinding(Queue userDeletedQuestQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedQuestQueue).to(userExchange).with(RabbitMQConstants.USER_DELETED_ROUTING_KEY);
    }

    @Bean
    public Binding userDeletedSocialBinding(Queue userDeletedSocialQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userDeletedSocialQueue).to(userExchange).with(RabbitMQConstants.USER_DELETED_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(name = "rabbitTemplate")
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
