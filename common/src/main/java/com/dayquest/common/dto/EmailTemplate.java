package com.dayquest.common.dto;

import java.io.Serializable;

/**
 * Common email template DTO used for sending emails via RabbitMQ.
 */
public class EmailTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    private String to;
    private String subject;
    private String body;

    public EmailTemplate() {}

    public EmailTemplate(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "EmailTemplate{" +
                "to='" + to + '\'' +
                ", subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}
