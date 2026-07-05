package com.dayquest.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise Redis-based Rate Limiting configuration.
 * Uses Redis sliding window algorithm for distributed rate limiting.
 *
 * Enable by setting: spring.data.redis.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(RedisRateLimitConfig.RateLimitProperties.class)
public class RedisRateLimitConfig {

    /**
     * Rate limit service for distributed rate limiting using Redis.
     */
    @Bean
    public RedisRateLimitService redisRateLimitService(StringRedisTemplate redisTemplate,
                                                         RateLimitProperties properties) {
        return new RedisRateLimitService(redisTemplate, properties);
    }

    /**
     * Rate limit configurations for different API endpoints.
     */
    @Bean
    public RateLimitConfigurations rateLimitConfigurations(RateLimitProperties properties) {
        return new RateLimitConfigurations(properties);
    }

    /**
     * Rate limit properties configurable via application.yml.
     */
    @ConfigurationProperties(prefix = "rate-limit")
    public static class RateLimitProperties {
        private Map<String, EndpointLimit> endpoints = new HashMap<>();
        private DefaultLimits defaults = new DefaultLimits();

        public static class EndpointLimit {
            private int capacity = 100;
            private Duration window = Duration.ofMinutes(1);

            public int getCapacity() { return capacity; }
            public void setCapacity(int capacity) { this.capacity = capacity; }
            public Duration getWindow() { return window; }
            public void setWindow(Duration window) { this.window = window; }
        }

        public static class DefaultLimits {
            private int anonymousCapacity = 100;
            private Duration anonymousWindow = Duration.ofMinutes(1);
            private int authenticatedCapacity = 500;
            private Duration authenticatedWindow = Duration.ofMinutes(1);
            private int premiumCapacity = 2000;
            private Duration premiumWindow = Duration.ofMinutes(1);

            // Getters and setters
            public int getAnonymousCapacity() { return anonymousCapacity; }
            public void setAnonymousCapacity(int anonymousCapacity) { this.anonymousCapacity = anonymousCapacity; }
            public Duration getAnonymousWindow() { return anonymousWindow; }
            public void setAnonymousWindow(Duration anonymousWindow) { this.anonymousWindow = anonymousWindow; }
            public int getAuthenticatedCapacity() { return authenticatedCapacity; }
            public void setAuthenticatedCapacity(int authenticatedCapacity) { this.authenticatedCapacity = authenticatedCapacity; }
            public Duration getAuthenticatedWindow() { return authenticatedWindow; }
            public void setAuthenticatedWindow(Duration authenticatedWindow) { this.authenticatedWindow = authenticatedWindow; }
            public int getPremiumCapacity() { return premiumCapacity; }
            public void setPremiumCapacity(int premiumCapacity) { this.premiumCapacity = premiumCapacity; }
            public Duration getPremiumWindow() { return premiumWindow; }
            public void setPremiumWindow(Duration premiumWindow) { this.premiumWindow = premiumWindow; }
        }

        public Map<String, EndpointLimit> getEndpoints() { return endpoints; }
        public void setEndpoints(Map<String, EndpointLimit> endpoints) { this.endpoints = endpoints; }
        public DefaultLimits getDefaults() { return defaults; }
        public void setDefaults(DefaultLimits defaults) { this.defaults = defaults; }
    }

    /**
     * Redis-based rate limiting service using sliding window algorithm.
     */
    public static class RedisRateLimitService {
        private final StringRedisTemplate redisTemplate;
        private final RateLimitProperties properties;

        public RedisRateLimitService(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
            this.redisTemplate = redisTemplate;
            this.properties = properties;
        }

        /**
         * Check if request is allowed and consume a token.
         * Returns remaining tokens or -1 if rate limited.
         */
        public RateLimitResult tryConsume(String key, int limit, Duration window) {
            String redisKey = "ratelimit:" + key;
            long now = System.currentTimeMillis();
            long windowStart = now - window.toMillis();

            org.springframework.data.redis.core.ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            if (zSetOps == null) {
                return new RateLimitResult(true, limit - 1, 0);
            }

            // Remove old entries
            zSetOps.removeRangeByScore(redisKey, 0, windowStart);

            // Count current requests
            Long count = zSetOps.zCard(redisKey);
            long actualCount = (count != null) ? count : 0L;

            if (actualCount < limit) {
                // Add current request
                zSetOps.add(redisKey, String.valueOf(now), now);
                redisTemplate.expire(redisKey, window);
                return new RateLimitResult(true, limit - (int) actualCount - 1, 0);
            } else {
                // Rate limited - calculate wait time
                java.util.Set<String> oldestSet = zSetOps.range(redisKey, 0, 0);
                long waitTime = window.getSeconds();
                if (oldestSet != null && !oldestSet.isEmpty()) {
                    String oldestElement = oldestSet.iterator().next();
                    if (oldestElement != null) {
                        Double score = zSetOps.score(redisKey, oldestElement);
                        if (score != null) {
                            waitTime = (long) (score.doubleValue() + window.toMillis() - now) / 1000;
                        }
                    }
                }
                return new RateLimitResult(false, 0, waitTime);
            }
        }

        /**
         * Get rate limit for anonymous users.
         */
        public RateLimitResult tryConsumeAnonymous(String key) {
            return tryConsume("anon:" + key,
                    properties.getDefaults().getAnonymousCapacity(),
                    properties.getDefaults().getAnonymousWindow());
        }

        /**
         * Get rate limit for authenticated users.
         */
        public RateLimitResult tryConsumeAuthenticated(String userId) {
            return tryConsume("auth:" + userId,
                    properties.getDefaults().getAuthenticatedCapacity(),
                    properties.getDefaults().getAuthenticatedWindow());
        }

        /**
         * Get rate limit for premium users.
         */
        public RateLimitResult tryConsumePremium(String userId) {
            return tryConsume("premium:" + userId,
                    properties.getDefaults().getPremiumCapacity(),
                    properties.getDefaults().getPremiumWindow());
        }

        /**
         * Get rate limit for specific endpoint.
         */
        public RateLimitResult tryConsumeEndpoint(String key, String endpoint) {
            RateLimitProperties.EndpointLimit limit = properties.getEndpoints().get(endpoint);
            if (limit != null) {
                return tryConsume("endpoint:" + endpoint + ":" + key, limit.getCapacity(), limit.getWindow());
            }
            return tryConsumeAuthenticated(key);
        }
    }

    /**
     * Result of rate limit check.
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final int remainingTokens;
        private final long retryAfterSeconds;

        public RateLimitResult(boolean allowed, int remainingTokens, long retryAfterSeconds) {
            this.allowed = allowed;
            this.remainingTokens = remainingTokens;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public boolean isAllowed() { return allowed; }
        public int getRemainingTokens() { return remainingTokens; }
        public long getRetryAfterSeconds() { return retryAfterSeconds; }
    }

    /**
     * Holds rate limit configurations for different scenarios.
     */
    public static class RateLimitConfigurations {
        private final RateLimitProperties properties;

        public RateLimitConfigurations(RateLimitProperties properties) {
            this.properties = properties;
        }

        public RateLimitProperties.DefaultLimits getDefaults() {
            return properties.getDefaults();
        }

        public RateLimitProperties.EndpointLimit getEndpointLimit(String endpoint) {
            return properties.getEndpoints().get(endpoint);
        }

        // Pre-defined limits for critical endpoints (development-friendly values)
        public int getLoginLimit() { return 100; }
        public Duration getLoginWindow() { return Duration.ofMinutes(1); }

        public int getRegisterLimit() { return 50; }
        public Duration getRegisterWindow() { return Duration.ofMinutes(1); }

        public int getVideoUploadLimit() { return 50; }
        public Duration getVideoUploadWindow() { return Duration.ofMinutes(1); }

        public int getSearchLimit() { return 200; }
        public Duration getSearchWindow() { return Duration.ofMinutes(1); }
    }
}

