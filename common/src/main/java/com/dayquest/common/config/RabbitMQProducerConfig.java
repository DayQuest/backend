package com.dayquest.common.config;

import com.dayquest.common.messaging.RabbitMQConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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

    // --- Dead Letter Exchange & Queue ---
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(RabbitMQConstants.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue globalDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMQConstants.GLOBAL_DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue globalDeadLetterQueue, TopicExchange deadLetterExchange) {
        // Catch all dead letters regardless of the original routing key.
        return BindingBuilder.bind(globalDeadLetterQueue).to(deadLetterExchange).with("#");
    }

    // --- Notification Exchange ---
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(RabbitMQConstants.NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(RabbitMQConstants.EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(emailQueue).to(notificationExchange).with(RabbitMQConstants.EMAIL_ROUTING_KEY);
    }

    // --- User Exchange - for user lifecycle events ---
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(RabbitMQConstants.USER_EXCHANGE, true, false);
    }

    @Bean
    public Queue userDeletedVideoQueue() {
        return QueueBuilder.durable(RabbitMQConstants.USER_DELETED_VIDEO_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Queue userDeletedQuestQueue() {
        return QueueBuilder.durable(RabbitMQConstants.USER_DELETED_QUEST_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Queue userDeletedSocialQueue() {
        return QueueBuilder.durable(RabbitMQConstants.USER_DELETED_SOCIAL_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLX_EXCHANGE)
                .build();
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
    public Queue socialDeleteQueue() {
        return QueueBuilder.durable(RabbitMQConstants.USER_DELETED_SOCIAL_QUEUE)
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "deadLetter")
                .build();
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(name = "rabbitTemplate")
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
