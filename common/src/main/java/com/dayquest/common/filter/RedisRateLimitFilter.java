package com.dayquest.common.filter;

import com.dayquest.common.config.RedisRateLimitConfig.RateLimitConfigurations;
import com.dayquest.common.config.RedisRateLimitConfig.RateLimitResult;
import com.dayquest.common.config.RedisRateLimitConfig.RedisRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enterprise Redis-based Rate Limiting Filter.
 * Provides distributed rate limiting using Redis sliding window algorithm.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnBean(RedisRateLimitService.class)
public class RedisRateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RedisRateLimitFilter.class);

    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private final RedisRateLimitService rateLimitService;
    private final RateLimitConfigurations rateLimitConfigurations;

    public RedisRateLimitFilter(RedisRateLimitService rateLimitService,
                                 RateLimitConfigurations rateLimitConfigurations) {
        this.rateLimitService = rateLimitService;
        this.rateLimitConfigurations = rateLimitConfigurations;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        // Skip rate limiting for health checks and actuator endpoints
        String path = request.getRequestURI();
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey = resolveBucketKey(request);
        RateLimitResult result = resolveAndApplyLimit(request, path, bucketKey);

        // Add rate limit headers
        response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(result.getRemainingTokens()));

        if (result.isAllowed()) {
            logger.debug("Rate limit OK for key: {}, remaining: {}", bucketKey, result.getRemainingTokens());
            filterChain.doFilter(request, response);
        } else {
            long waitTimeSeconds = result.getRetryAfterSeconds();

            response.setHeader(RETRY_AFTER_HEADER, String.valueOf(waitTimeSeconds));
            response.setHeader(RATE_LIMIT_RESET_HEADER, String.valueOf(System.currentTimeMillis() / 1000 + waitTimeSeconds));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            String errorResponse = String.format(
                    "{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Try again in %d seconds.\", \"status\": 429, \"retryAfter\": %d}",
                    waitTimeSeconds, waitTimeSeconds
            );

            response.getWriter().write(errorResponse);

            logger.warn("Rate limit exceeded for key: {}, retry after: {} seconds", bucketKey, waitTimeSeconds);
        }
    }

    /**
     * Resolves the bucket key based on authentication and IP.
     */
    private String resolveBucketKey(HttpServletRequest request) {
        // Try to get user ID from header (set after authentication)
        String userId = request.getHeader("X-User-Id");

        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        // Fall back to IP-based rate limiting
        return getClientIp(request);
    }

    /**
     * Resolves and applies rate limit based on endpoint and user type.
     */
    private RateLimitResult resolveAndApplyLimit(HttpServletRequest request, String path, String key) {
        // Critical endpoint-specific limits
        if (path.contains("/auth/login")) {
            return rateLimitService.tryConsume("login:" + key,
                    rateLimitConfigurations.getLoginLimit(),
                    rateLimitConfigurations.getLoginWindow());
        }
        if (path.contains("/auth/register")) {
            return rateLimitService.tryConsume("register:" + key,
                    rateLimitConfigurations.getRegisterLimit(),
                    rateLimitConfigurations.getRegisterWindow());
        }
        if (path.contains("/videos/upload") || (path.contains("/videos") && "POST".equals(request.getMethod()))) {
            return rateLimitService.tryConsume("upload:" + key,
                    rateLimitConfigurations.getVideoUploadLimit(),
                    rateLimitConfigurations.getVideoUploadWindow());
        }
        if (path.contains("/search") || path.contains("/users/search")) {
            return rateLimitService.tryConsume("search:" + key,
                    rateLimitConfigurations.getSearchLimit(),
                    rateLimitConfigurations.getSearchWindow());
        }

        // User type based limits
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return rateLimitService.tryConsumeAnonymous(key);
        }

        // Check for premium user
        if (isPremiumUser(request)) {
            return rateLimitService.tryConsumePremium(key);
        }

        return rateLimitService.tryConsumeAuthenticated(key);
    }

    /**
     * Checks if user is premium (from header set by gateway).
     */
    private boolean isPremiumUser(HttpServletRequest request) {
        String premiumHeader = request.getHeader("X-User-Premium");
        return "true".equalsIgnoreCase(premiumHeader);
    }

    /**
     * Gets client IP, considering proxy headers.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Checks if path should be excluded from rate limiting.
     */
    private boolean isExcludedPath(String path) {
        return path.startsWith("/actuator") ||
               path.startsWith("/health") ||
               path.startsWith("/swagger") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/favicon.ico");
    }
}

