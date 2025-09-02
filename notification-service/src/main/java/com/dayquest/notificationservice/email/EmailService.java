package com.dayquest.notificationservice.email;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);


    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(EmailTemplate emailTemplate) throws MessagingException, MailException {
        logger.info("Preparing to send email to: {}", emailTemplate.getTo());

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(emailTemplate.getTo());
        helper.setSubject(emailTemplate.getSubject());
        helper.setText(emailTemplate.getBody(), true);

        mailSender.send(mimeMessage);

        logger.info("Email sent successfully to: {}", emailTemplate.getTo());
    }

    public boolean sendEmailSafely(EmailTemplate emailTemplate) {
        try {
            sendEmail(emailTemplate);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", emailTemplate.getTo(), e.getMessage(), e);
            return false;
        }
    }
}
