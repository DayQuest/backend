package com.dayquest.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Enterprise Request Tracking Filter.
 * Provides distributed request tracing with Redis for correlation across services.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DistributedRequestTrackingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(DistributedRequestTrackingFilter.class);

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String TRACE_ID_HEADER = "X-Trace-ID";
    public static final String SPAN_ID_HEADER = "X-Span-ID";

    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_CLIENT_IP = "clientIp";
    private static final String MDC_REQUEST_PATH = "requestPath";
    private static final String MDC_REQUEST_METHOD = "requestMethod";

    private final StringRedisTemplate redisTemplate;

    public DistributedRequestTrackingFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // Get or generate IDs
        String requestId = getOrGenerateId(request.getHeader(REQUEST_ID_HEADER));
        String correlationId = getOrGenerateId(request.getHeader(CORRELATION_ID_HEADER));
        String traceId = getOrGenerateId(request.getHeader(TRACE_ID_HEADER));
        String spanId = generateSpanId();

        // Extract user info
        String userId = extractUserId(request);
        String clientIp = getClientIp(request);

        try {
            // Set MDC for logging
            MDC.put(MDC_REQUEST_ID, requestId);
            MDC.put(MDC_CORRELATION_ID, correlationId);
            MDC.put(MDC_TRACE_ID, traceId);
            MDC.put(MDC_SPAN_ID, spanId);
            MDC.put(MDC_CLIENT_IP, clientIp);
            MDC.put(MDC_REQUEST_PATH, request.getRequestURI());
            MDC.put(MDC_REQUEST_METHOD, request.getMethod());

            if (userId != null) {
                MDC.put(MDC_USER_ID, userId);
            }

            // Set response headers for tracing
            response.setHeader(REQUEST_ID_HEADER, requestId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            response.setHeader(TRACE_ID_HEADER, traceId);
            response.setHeader(SPAN_ID_HEADER, spanId);

            // Store request trace in Redis for distributed tracing (async, non-blocking)
            storeRequestTrace(requestId, correlationId, traceId, request, clientIp, userId);

            // Continue with request
            filterChain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Log request completion with timing
            logger.info("Request completed: {} {} - Status: {} - Duration: {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);

            // Update request trace with response info
            updateRequestTrace(requestId, response.getStatus(), duration);

            // Clear MDC
            MDC.clear();
        }
    }

    private String getOrGenerateId(String existingId) {
        if (existingId != null && !existingId.isBlank()) {
            return existingId;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String extractUserId(HttpServletRequest request) {
        // Try X-User-Id header (set after authentication)
        String userId = request.getHeader("X-User-Id");
        if (userId != null) {
            return userId;
        }

        // Could also extract from JWT if needed
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Return first IP in case of multiple (proxy chain)
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Stores request trace in Redis for distributed tracing.
     */
    private void storeRequestTrace(String requestId, String correlationId, String traceId,
                                    HttpServletRequest request, String clientIp, String userId) {
        try {
            String key = "trace:request:" + requestId;
            String value = String.format(
                    "{\"correlationId\":\"%s\",\"traceId\":\"%s\",\"method\":\"%s\",\"path\":\"%s\",\"clientIp\":\"%s\",\"userId\":\"%s\",\"timestamp\":\"%s\",\"service\":\"%s\"}",
                    correlationId,
                    traceId,
                    request.getMethod(),
                    request.getRequestURI(),
                    clientIp,
                    userId != null ? userId : "anonymous",
                    Instant.now().toString(),
                    getServiceName()
            );

            // Store with 1 hour TTL
            redisTemplate.opsForValue().set(key, value, Duration.ofHours(1));

        } catch (Exception e) {
            logger.debug("Failed to store request trace in Redis: {}", e.getMessage());
        }
    }

    /**
     * Updates request trace with response info.
     */
    private void updateRequestTrace(String requestId, int status, long duration) {
        try {
            String key = "trace:request:" + requestId + ":response";
            String value = String.format(
                    "{\"status\":%d,\"duration\":%d,\"completedAt\":\"%s\"}",
                    status,
                    duration,
                    Instant.now().toString()
            );

            redisTemplate.opsForValue().set(key, value, Duration.ofHours(1));

        } catch (Exception e) {
            logger.debug("Failed to update request trace in Redis: {}", e.getMessage());
        }
    }

    private String getServiceName() {
        return System.getProperty("spring.application.name", "unknown-service");
    }
}

