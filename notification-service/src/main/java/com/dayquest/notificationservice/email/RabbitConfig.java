package com.dayquest.notificationservice.email;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Configuration;

/**
 * Notification service specific RabbitMQ configuration for message consumers.
 * The rabbitListenerContainerFactory is provided by the common module (RabbitMQConsumerConfig).
 */
@Configuration
@EnableRabbit
public class RabbitConfig {
    // rabbitListenerContainerFactory is now provided by com.dayquest.common.config.RabbitMQConsumerConfig
    // No additional beans needed here - common module provides all required configuration
}
