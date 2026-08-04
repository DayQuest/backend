package com.dayquest.userservice.config;

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
                title = "User Service API",
                version = "1.0",
                description = """
                        Handles user account management, authentication, profile pictures, follows, and badges.

                        **Authentication** endpoints (`/auth/**`) are public. All **User** endpoints (`/users/**`) \
                        require a valid JWT Bearer token obtained from `POST /auth/login`.
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
