package com.dayquest.notificationservice.email;

import com.dayquest.common.dto.EmailTemplate;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Email Service Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private EmailTemplate testEmailTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@dayquest.com");

        testEmailTemplate = new EmailTemplate();
        testEmailTemplate.setTo("test@example.com");
        testEmailTemplate.setSubject("Test Subject");
        testEmailTemplate.setBody("<h1>Test Email</h1>");
    }

    @Test
    @DisplayName("Should send email successfully")
    void shouldSendEmail() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> emailService.sendEmail(testEmailTemplate));

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send email safely and return true on success")
    void shouldSendEmailSafelySuccess() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        boolean result = emailService.sendEmailSafely(testEmailTemplate);

        assertTrue(result);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle email sending failure gracefully and return false")
    void shouldHandleEmailSendingFailure() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        boolean result = emailService.sendEmailSafely(testEmailTemplate);

        assertFalse(result);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle null email template")
    void shouldHandleNullEmailTemplate() {
        boolean result = emailService.sendEmailSafely(null);

        assertFalse(result);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}

