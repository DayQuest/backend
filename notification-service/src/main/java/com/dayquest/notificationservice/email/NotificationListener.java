package com.dayquest.notificationservice.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationListener.class);

    @Autowired
    private EmailService emailService;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void handleEmailNotification(EmailTemplate emailTemplate) {
        logger.info("Received email notification: {}", emailTemplate);

        try {
            if (emailTemplate == null) {
                logger.error("Received null email template");
                return;
            }

            if (emailTemplate.getTo() == null || emailTemplate.getTo().trim().isEmpty()) {
                logger.error("Received email template with empty recipient: {}", emailTemplate);
                return;
            }

            if (emailTemplate.getSubject() == null || emailTemplate.getSubject().trim().isEmpty()) {
                logger.warn("Received email template with empty subject, using default");
                emailTemplate.setSubject("No Subject");
            }

            if (emailTemplate.getBody() == null) {
                emailTemplate.setBody("");
            }

            boolean success = emailService.sendEmailSafely(emailTemplate);

            if (success) {
                logger.info("Email notification processed successfully for: {}", emailTemplate.getTo());
            } else {
                logger.error("Failed to process email notification for: {}", emailTemplate.getTo());
            }

        } catch (Exception e) {
            logger.error("Error processing email notification: {}", e.getMessage(), e);
        }
    }
}
