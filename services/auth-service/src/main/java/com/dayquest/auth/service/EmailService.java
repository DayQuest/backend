package com.dayquest.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
        logger.info("Verification email sent to: {}", to);
    }

    @Async
    public void sendPasswordResetEmail(String to, String token) throws MessagingException {
        String frontendResetUrl = "https://api.dayquest.de/api/auth/reset-password?token=" + token;

        String htmlMessage = String.format(
                "<html><body style=\"font-family: Arial, sans-serif;\">" +
                        "<div style=\"background-color: #f5f5f5; padding: 20px; text-align: center;\">" +
                        "<h2 style=\"color: #333;\">Password Reset Request</h2>" +
                        "<p style=\"font-size: 16px;\">You requested a password reset for your DayQuest account.</p>" +
                        "<p style=\"margin-top: 20px; margin-bottom: 20px;\">" +
                        "<a href=\"%s\" style=\"background-color: #007bff; color: white; padding: 10px 20px; " +
                        "text-decoration: none; border-radius: 5px; font-size: 16px;\">Reset Your Password</a></p>" +
                        "<p style=\"font-size: 14px; color: #777;\">This link is valid for 1 hour.</p>" +
                        "</div></body></html>",
                frontendResetUrl);

        sendVerificationEmail(to, "DayQuest Password Reset Request", htmlMessage);
    }

    @Async
    public void sendVerificationCode(String to, String verificationCode) throws MessagingException {
        String htmlMessage = "<html>" +
                "<body style=\"font-family: Arial, sans-serif;\">" +
                "<div style=\"background-color: #f5f5f5; padding: 20px;\">" +
                "<h2 style=\"color: #333;\">Welcome to DayQuest!</h2>" +
                "<p style=\"font-size: 16px;\">Here is your verification code:</p>" +
                "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; " +
                "box-shadow: 0 0 10px rgba(0,0,0,0.1);\">" +
                "<h3 style=\"color: #333;\">Verification Code:</h3>" +
                "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendVerificationEmail(to, "Account Verification", htmlMessage);
    }
}
