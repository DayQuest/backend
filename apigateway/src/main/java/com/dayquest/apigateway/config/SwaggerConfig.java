package com.dayquest.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

/**
 * Configuration to ensure Swagger UI paths are handled locally and not routed to backend services.
 */
@Configuration
public class SwaggerConfig {

    /**
     * Predicate that matches paths that should NOT be handled by the gateway router.
     * These paths are handled by SpringDoc's built-in controllers.
     */
    public static RequestPredicate swaggerPaths() {
        return RequestPredicates.path("/swagger-ui/**")
                .or(RequestPredicates.path("/swagger-ui.html"))
                .or(RequestPredicates.path("/v3/api-docs/**"))
                .or(RequestPredicates.path("/v3/api-docs"))
                .or(RequestPredicates.path("/v3/api-docs.yaml"))
                .or(RequestPredicates.path("/swagger-resources/**"))
                .or(RequestPredicates.path("/webjars/**"));
    }

    /**
     * High-priority route that redirects /swagger-ui.html to the actual Swagger UI index.
     */
    @Bean
    @Order(-1)
    public RouterFunction<ServerResponse> swaggerRedirect() {
        return RouterFunctions.route()
                .GET("/swagger-ui.html", request ->
                    ServerResponse.temporaryRedirect(URI.create("/swagger-ui/index.html")).build())
                .build();
    }
}

