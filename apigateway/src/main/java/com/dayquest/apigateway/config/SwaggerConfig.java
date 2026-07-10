package com.dayquest.apigateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
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
 * Central API documentation configuration for the DayQuest API Gateway.
 *
 * <p>This gateway aggregates documentation from all microservices. Use the
 * Swagger UI dropdown to switch between service docs, or browse the unified
 * gateway-level spec at {@code /v3/api-docs}.
 *
 * <p>A ReDoc-powered single-page view is also available at {@code /redoc}.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "DayQuest API",
                version = "1.0",
                description = """
                        ## DayQuest — Unified API Gateway

                        **Base URL**: `http://localhost:8080`

                        This is the aggregated API documentation for all DayQuest microservices, \
                        served through the central API Gateway.

                        ### Authentication
                        All protected endpoints require a **JWT Bearer token**. Obtain one from \
                        `POST /auth/login` and pass it as:
                        ```
                        Authorization: Bearer <access_token>
                        ```
                        Access tokens are valid for **15 minutes**. Use `POST /auth/refresh` to \
                        obtain a new pair using the refresh token (valid for **7 days**).

                        ### Rate Limits
                        | Endpoint group | Limit |
                        |---|---|
                        | Default | 100 req/s |
                        | Login | 10 req/min |
                        | Register | 10 req/min |
                        | Upload | 20 req/min |
                        | Search | 60 req/min |

                        ### Services
                        Use the **dropdown** in Swagger UI to switch between individual service docs:
                        - **user-service** — Auth, users, profiles, follows
                        - **quest-service** — Quest CRUD, ratings, daily quest
                        - **video-service** — Video upload, feed, voting
                        - **social-service** — Comments, friendships, hashtags
                        - **content-service** — Reports, badges, streaks
                        - **notification-service** — Email notifications (internal)
                        """,
                contact = @Contact(name = "DayQuest Team")
        ),
        servers = @Server(url = "http://localhost:8080", description = "API Gateway (local)")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "JWT access token obtained from POST /auth/login. Valid for 15 minutes."
)
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
                .or(RequestPredicates.path("/webjars/**"))
                .or(RequestPredicates.path("/redoc"));
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
                .GET("/redoc", request ->
                    ServerResponse.ok()
                            .header("Content-Type", "text/html; charset=UTF-8")
                            .body(buildReDocHtml()))
                .build();
    }

    /**
     * Gateway-level OpenAPI bean with rich metadata, security schemes, and tag groupings.
     */
    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("DayQuest API")
                        .version("1.0.0")
                        .description("Unified API Gateway — aggregates all DayQuest microservices")
                        .contact(new io.swagger.v3.oas.models.info.Contact()
                                .name("DayQuest Team"))
                        .license(new License().name("Proprietary")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT access token from POST /auth/login. Format: Bearer <token>")))
                .addTagsItem(new Tag().name("Authentication")
                        .description("Registration, login, token refresh, email verification, and password reset"))
                .addTagsItem(new Tag().name("Users")
                        .description("User profiles, follow/unfollow, profile pictures, and badge management"))
                .addTagsItem(new Tag().name("Quests")
                        .description("Quest CRUD, like/dislike ratings, and the daily quest system"))
                .addTagsItem(new Tag().name("Videos")
                        .description("Video upload, feed algorithm, voting (upvote/downvote), and lifecycle"))
                .addTagsItem(new Tag().name("Comments")
                        .description("Comments on videos and quests"))
                .addTagsItem(new Tag().name("Friendships")
                        .description("Friend requests, acceptance/decline, blocking, and friend lists"))
                .addTagsItem(new Tag().name("Hashtags")
                        .description("Hashtag discovery and trending content"))
                .addTagsItem(new Tag().name("Badges")
                        .description("Badge creation, listing, and awarding (admin)"))
                .addTagsItem(new Tag().name("Streaks")
                        .description("Daily activity streaks and leaderboards"))
                .addTagsItem(new Tag().name("Reports")
                        .description("Content moderation reports and admin resolution"));
    }

    /**
     * Customizes the gateway OpenAPI spec to apply a global security requirement
     * to all operations that don't explicitly opt out.
     */
    @Bean
    public OpenApiCustomizer globalSecurityCustomizer() {
        return openApi -> {
            var securityRequirement = new io.swagger.v3.oas.models.security.SecurityRequirement()
                    .addList("bearerAuth");
            if (openApi.getPaths() != null) {
                openApi.getPaths().values().forEach(pathItem ->
                        pathItem.readOperations().forEach(operation -> {
                            // Only add security if not already defined
                            if (operation.getSecurity() == null) {
                                operation.addSecurityItem(securityRequirement);
                            }
                        })
                );
            }
        };
    }

    /**
     * Generates the HTML page for the ReDoc documentation UI.
     * ReDoc provides a cleaner, read-only view of the API documentation.
     *
     * @return HTML string for the ReDoc page
     */
    private String buildReDocHtml() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                  <head>
                    <title>DayQuest API Reference</title>
                    <meta charset="utf-8"/>
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <meta name="description" content="DayQuest API Reference Documentation">
                    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
                    <style>
                      * { box-sizing: border-box; }
                      body {
                        margin: 0;
                        padding: 0;
                        font-family: 'Inter', sans-serif;
                        background: #0f0f1a;
                      }
                      .topbar {
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        padding: 12px 24px;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        position: fixed;
                        top: 0;
                        left: 0;
                        right: 0;
                        z-index: 1000;
                        box-shadow: 0 2px 20px rgba(102, 126, 234, 0.4);
                      }
                      .topbar-brand {
                        color: white;
                        font-size: 1.4rem;
                        font-weight: 700;
                        letter-spacing: -0.5px;
                        display: flex;
                        align-items: center;
                        gap: 10px;
                      }
                      .topbar-brand .badge {
                        background: rgba(255,255,255,0.2);
                        padding: 2px 10px;
                        border-radius: 20px;
                        font-size: 0.75rem;
                        font-weight: 500;
                        letter-spacing: 0.5px;
                        text-transform: uppercase;
                      }
                      .topbar-links {
                        display: flex;
                        gap: 16px;
                      }
                      .topbar-links a {
                        color: rgba(255,255,255,0.85);
                        text-decoration: none;
                        font-size: 0.875rem;
                        font-weight: 500;
                        padding: 6px 14px;
                        border-radius: 6px;
                        border: 1px solid rgba(255,255,255,0.25);
                        transition: all 0.2s;
                      }
                      .topbar-links a:hover {
                        background: rgba(255,255,255,0.15);
                        color: white;
                        border-color: rgba(255,255,255,0.4);
                      }
                      .redoc-wrapper {
                        margin-top: 60px;
                      }
                    </style>
                  </head>
                  <body>
                    <div class="topbar">
                      <div class="topbar-brand">
                        ⚡ DayQuest API
                        <span class="badge">v1.0</span>
                      </div>
                      <div class="topbar-links">
                        <a href="/swagger-ui/index.html">Swagger UI</a>
                        <a href="/v3/api-docs">OpenAPI JSON</a>
                      </div>
                    </div>
                    <div class="redoc-wrapper">
                      <redoc
                        spec-url='/v3/api-docs'
                        hide-download-button
                        expand-responses="200,201"
                        required-props-first
                        sort-props-alphabetically
                        theme='{
                          "colors": {
                            "primary": { "main": "#667eea" },
                            "tonalOffset": 0.2
                          },
                          "typography": {
                            "fontSize": "15px",
                            "fontFamily": "Inter, sans-serif",
                            "headings": {
                              "fontFamily": "Inter, sans-serif",
                              "fontWeight": "600"
                            },
                            "code": {
                              "fontSize": "13px",
                              "fontFamily": "\\"Fira Code\\", monospace",
                              "backgroundColor": "#1a1a2e"
                            }
                          },
                          "sidebar": {
                            "backgroundColor": "#13131f",
                            "textColor": "#c8c8d4",
                            "activeTextColor": "#a78bfa",
                            "groupItems": {
                              "textTransform": "uppercase"
                            }
                          },
                          "rightPanel": {
                            "backgroundColor": "#0f0f1a",
                            "textColor": "#e2e2f0"
                          },
                          "schema": {
                            "defaultDetailsIsOpen": false
                          }
                        }'
                      ></redoc>
                    </div>
                    <script src="https://cdn.jsdelivr.net/npm/redoc@latest/bundles/redoc.standalone.js"></script>
                  </body>
                </html>
                """;
    }
}
