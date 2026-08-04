package com.dayquest.contentservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Content Service API",
                version = "1.0",
                description = """
                        Manages content moderation (reports), gamification (badges), and user streaks.

                        **Access levels:**
                        - `Authenticated` — any logged-in user
                        - `Admin` — requires ROLE_ADMIN authority

                        All endpoints require a valid JWT Bearer token. The gateway injects `X-User-Id` automatically.
                        """,
                contact = @Contact(name = "DayQuest Team")
        ),
        servers = @Server(url = "/", description = "Via API Gateway (http://localhost:8080)"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Provide the JWT access token obtained from POST /auth/login. Format: Bearer <token>"
)
public class OpenApiConfig {
}
