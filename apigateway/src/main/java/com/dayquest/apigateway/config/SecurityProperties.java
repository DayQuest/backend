package com.dayquest.apigateway.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private List<String> publicPaths = new ArrayList<>();

    private static final List<String> DEFAULT_PUBLIC_PATHS = Arrays.asList(
            // Authentication endpoints
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/verify",
            "/auth/resend-verification",
            "/auth/forgot-password",
            "/auth/reset-password",
            // Public resources
            "/users/profilepicture/**",
            // Gateway info
            "/gateway/**",
            // Actuator
            "/actuator/**",
            // Swagger/OpenAPI
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/webjars/**",
            "/swagger-resources/**",
            "/swagger-config",
            "/api-docs/**",
            "/redoc",
            // Service API docs
            "/auth-service/v3/api-docs",
            "/user-service/v3/api-docs",
            "/quest-service/v3/api-docs",
            "/notification-service/v3/api-docs",
            "/video-service/v3/api-docs",
            "/social-service/v3/api-docs",
            "/content-service/v3/api-docs"
    );

    @PostConstruct
    public void init() {
        // Merge defaults with any configured paths
        for (String defaultPath : DEFAULT_PUBLIC_PATHS) {
            if (!publicPaths.contains(defaultPath)) {
                publicPaths.add(defaultPath);
            }
        }
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = new ArrayList<>(publicPaths);
    }
}

