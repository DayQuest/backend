package com.dayquest.apigateway.config;

import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.rewritePath;

/**
 * Gateway routing configuration for all microservices.
 * Routes requests to the appropriate backend services via Eureka service discovery.
 * Note: Swagger UI paths (/swagger-ui/**, /v3/api-docs/**) are excluded and handled by SpringDoc.
 */
@Configuration
public class GatewayRoutingConfig {

    /**
     * Routes for Authentication endpoints (public)
     * POST /auth/register - Register new user
     * POST /auth/login - Login user
     * POST /auth/refresh - Refresh tokens
     * POST /auth/logout - Logout user
     * POST /auth/verify - Verify email
     * POST /auth/forgot-password - Request password reset
     * POST /auth/reset-password - Reset password
     * POST /auth/token/validate - Validate token
     * Note: Auth endpoints are handled by user-service
     */
    @Bean
    @Order(0)
    public RouterFunction<ServerResponse> authRoutes() {
        return route("auth-routes")
                .route(RequestPredicates.path("/auth/**"), HandlerFunctions.http())
                .filter(lb("user-service"))
                .build();
    }

    // Note: authentication lives in user-service; there is no standalone auth-service module

    /**
     * Routes for User endpoints (authenticated)
     * GET /users/{uuid} - Get user by UUID
     * GET /users/profile/{username} - Get user profile by username
     * GET /users/search - Search users
     * GET /users/{username}/uuid - Get UUID by username
     * GET /users/profilepicture/{username} - Get profile picture
     * DELETE /users - Delete user account
     * PUT /users/email - Update email
     * PUT /users/password - Update password
     * POST /users/{uuid}/follow - Follow user
     * DELETE /users/{uuid}/follow - Unfollow user
     * GET /users/followers - Get followers
     * GET /users/following - Get following
     * POST /users/profilepicture - Upload profile picture
     * GET /users/badges - Get user badges
     */
    @Bean
    @Order(0)
    public RouterFunction<ServerResponse> userRoutes() {
        return route("user-service")
                .route(RequestPredicates.path("/users").or(RequestPredicates.path("/users/**")), HandlerFunctions.http())
                .filter(lb("user-service"))
                .build();
    }

    @Bean
    @Order(0)
    public RouterFunction<ServerResponse> userApiDocsRoute() {
        return route("user-service-docs")
                .route(RequestPredicates.path("/user-service/v3/api-docs"), HandlerFunctions.http())
                .filter(rewritePath("/user-service/(?<path>.*)", "/${path}"))
                .filter(lb("user-service"))
                .build();
    }

    /**
     * Routes for Quest endpoints (authenticated)
     * GET /quests - Get all quests (paginated)
     * GET /quests/{uuid} - Get quest by UUID
     * POST /quests/create - Create new quest
     * POST /quests/like - Like a quest
     * DELETE /quests/like - Unlike a quest
     * POST /quests/dislike - Dislike a quest
     * DELETE /quests/dislike - Remove dislike
     * GET /quests/daily - Get daily quest
     * POST /quests/reroll - Reroll daily quest
     */
    @Bean
    @Order(0)
    public RouterFunction<ServerResponse> questRoutes() {
        return route("quest-service")
                .route(RequestPredicates.path("/quests/**"), HandlerFunctions.http())
                .filter(lb("quest-service"))
                .build();
    }

    @Bean
    @Order(0)
    public RouterFunction<ServerResponse> questApiDocsRoute() {
        return route("quest-service-docs")
                .route(RequestPredicates.path("/quest-service/v3/api-docs"), HandlerFunctions.http())
                .filter(rewritePath("/quest-service/(?<path>.*)", "/${path}"))
                .filter(lb("quest-service"))
                .build();
    }

    /**
     * Routes for Notification endpoints (internal/admin)
     */
    @Bean
    @Order(0)
    public RouterFunction<ServerResponse> notificationRoutes() {
        return route("notification-service")
                .route(RequestPredicates.path("/notifications/**"), HandlerFunctions.http())
                .filter(lb("notification-service"))
                .build();
    }

    @Bean
    @Order(0)
    public RouterFunction<ServerResponse> notificationApiDocsRoute() {
        return route("notification-service-docs")
                .route(RequestPredicates.path("/notification-service/v3/api-docs"), HandlerFunctions.http())
                .filter(rewritePath("/notification-service/(?<path>.*)", "/${path}"))
                .filter(lb("notification-service"))
                .build();
    }

    /**
     * Routes for Video endpoints (authenticated)
     * GET /videos - Get videos (paginated, with filters)
     * GET /videos/{uuid} - Get video by UUID
     * POST /videos/upload - Upload new video
     * DELETE /videos/{uuid} - Delete video
     * POST /videos/{uuid}/upvote - Upvote video
     * POST /videos/{uuid}/downvote - Downvote video
     * GET /videos/next - Get next video for user feed
     */
    @Bean
    @Order(0)
    public RouterFunction<ServerResponse> videoRoutes() {
        return route("video-service")
                .route(RequestPredicates.path("/videos/**"), HandlerFunctions.http())
                .filter(lb("video-service"))
                .build();
    }

    @Bean
    @Order(0)
    public RouterFunction<ServerResponse> videoApiDocsRoute() {
        return route("video-service-docs")
                .route(RequestPredicates.path("/video-service/v3/api-docs"), HandlerFunctions.http())
                .filter(rewritePath("/video-service/(?<path>.*)", "/${path}"))
                .filter(lb("video-service"))
                .build();
    }

    /**
     * Routes for Social endpoints (authenticated)
     * GET /comments - Get comments for entity
     * POST /comments - Create comment
     * DELETE /comments/{uuid} - Delete comment
     * GET /friends - Get friends list
     * POST /friends/request - Send friend request
     * GET /hashtags - Get trending hashtags
     * GET /hashtags/{tag} - Get content by hashtag
     */
    @Bean
    @Order(0)
    public RouterFunction<ServerResponse> socialRoutes() {
        return route("social-service")
                .route(RequestPredicates.path("/comments/**"), HandlerFunctions.http())
                .route(RequestPredicates.path("/friends/**"), HandlerFunctions.http())
                .route(RequestPredicates.path("/hashtags/**"), HandlerFunctions.http())
                .filter(lb("social-service"))
                .build();
    }

    @Bean
    @Order(0)
    public RouterFunction<ServerResponse> socialApiDocsRoute() {
        return route("social-service-docs")
                .route(RequestPredicates.path("/social-service/v3/api-docs"), HandlerFunctions.http())
                .filter(rewritePath("/social-service/(?<path>.*)", "/${path}"))
                .filter(lb("social-service"))
                .build();
    }

    /**
     * Routes for Content endpoints (authenticated/admin)
     * GET /reports - Get reports (admin)
     * POST /reports - Submit report
     * GET /badges - Get available badges
     * GET /streaks - Get user streaks
     */
    @Bean
    @Order(0)
    public RouterFunction<ServerResponse> contentRoutes() {
        return route("content-service")
                .route(RequestPredicates.path("/reports/**"), HandlerFunctions.http())
                .route(RequestPredicates.path("/badges/**"), HandlerFunctions.http())
                .route(RequestPredicates.path("/streaks/**"), HandlerFunctions.http())
                .filter(lb("content-service"))
                .build();
    }

    @Bean
    @Order(0)
    public RouterFunction<ServerResponse> contentApiDocsRoute() {
        return route("content-service-docs")
                .route(RequestPredicates.path("/content-service/v3/api-docs"), HandlerFunctions.http())
                .filter(rewritePath("/content-service/(?<path>.*)", "/${path}"))
                .filter(lb("content-service"))
                .build();
    }

    // Note: Swagger UI routes (/swagger-ui/**, /v3/api-docs/**) are NOT defined here
    // They are handled by SpringDoc's built-in controllers
}
