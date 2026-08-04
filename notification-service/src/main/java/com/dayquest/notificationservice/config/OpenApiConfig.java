package com.dayquest.notificationservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Notification Service API",
                version = "1.0",
                description = """
                        Handles email notifications for DayQuest events (registration verification, \
                        friend requests, etc.).

                        This service is **internal** — it is triggered by RabbitMQ messages from \
                        other services, not directly by client requests. The single HTTP endpoint is \
                        reserved for internal or admin use only.
                        """,
                contact = @Contact(name = "DayQuest Team")
        ),
        servers = @Server(url = "/", description = "Via API Gateway (http://localhost:8080)")
)
public class OpenApiConfig {
}
