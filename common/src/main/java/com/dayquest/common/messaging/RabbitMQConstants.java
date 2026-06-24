package com.dayquest.common.messaging;

/**
 * Common RabbitMQ constants for messaging between services.
 */
public final class RabbitMQConstants {

    private RabbitMQConstants() {
        // Utility class - no instantiation
    }

    // Exchange names
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String USER_EXCHANGE = "user.exchange";

    // Queue names
    public static final String EMAIL_QUEUE = "notification.email.queue";
    public static final String USER_DELETED_VIDEO_QUEUE = "user.deleted.video.queue";
    public static final String USER_DELETED_QUEST_QUEUE = "user.deleted.quest.queue";
    public static final String USER_DELETED_SOCIAL_QUEUE = "user.deleted.social.queue";

    // Routing keys
    public static final String EMAIL_ROUTING_KEY = "email.send";
    public static final String USER_DELETED_ROUTING_KEY = "user.deleted";
}

