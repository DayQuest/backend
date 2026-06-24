package com.dayquest.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Explicit mail configuration to ensure STARTTLS is properly configured.
 * This overrides the auto-configured JavaMailSender to fix SSL/TLS issues.
 */
@Configuration
public class MailConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();

        // Basic SMTP properties
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        // STARTTLS configuration (NOT SSL) - Port 587 uses STARTTLS
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        // Explicitly disable SSL - this is crucial for port 587
        props.put("mail.smtp.ssl.enable", "false");

        // Trust the Gmail SMTP server
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        // Don't use a custom socket factory - let STARTTLS handle it
        props.put("mail.smtp.socketFactory.fallback", "true");

        // Timeouts
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        // Debug mode - can be disabled in production
        props.put("mail.debug", "true");

        return mailSender;
    }
}

