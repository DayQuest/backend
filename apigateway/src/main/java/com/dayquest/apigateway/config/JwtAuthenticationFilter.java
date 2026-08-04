package com.dayquest.apigateway.config;

import com.dayquest.apigateway.services.GatewayJwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final GatewayJwtService jwtService;
    private final SecurityProperties securityProperties;

    public JwtAuthenticationFilter(GatewayJwtService jwtService, SecurityProperties securityProperties) {
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        logger.debug("Incoming request path: {}", path);

        if (isPublicEndpoint(path)) {
            logger.debug("Path {} is public, skipping authentication", path);
            filterChain.doFilter(request, response);
            return;
        }

        logger.debug("Path {} requires authentication", path);
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedError(response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (jwtService.isTokenExpired(token)) {
                sendUnauthorizedError(response, "Token has expired");
                return;
            }

            if (!jwtService.isAccessToken(token)) {
                sendUnauthorizedError(response, "Invalid token type");
                return;
            }

            UUID userId = jwtService.extractUserId(token);
            String username = jwtService.extractUsername(token);
            List<String> roles = jwtService.extractRoles(token);

            // Set Spring Security Authentication in SecurityContext
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId.toString(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            request.setAttribute("userId", userId.toString());
            request.setAttribute("username", username);
            request.setAttribute("roles", roles);

            AuthenticatedRequestWrapper wrappedRequest = new AuthenticatedRequestWrapper(request);
            wrappedRequest.setHeader("X-User-Id", userId.toString());
            if (username != null) {
                wrappedRequest.setHeader("X-Username", username);
            }
            wrappedRequest.setHeader("X-User-Roles", String.join(",", roles));

            filterChain.doFilter(wrappedRequest, response);

        } catch (Exception e) {
            logger.error("JWT validation failed: {}", e.getMessage());
            sendUnauthorizedError(response, "Invalid token");
        }
    }

    private boolean isPublicEndpoint(String path) {
        boolean isPublic = securityProperties.getPublicPaths().stream()
                .anyMatch(pattern -> {
                    boolean matches = matchesPattern(path, pattern);
                    if (matches) {
                        logger.debug("Path {} matches pattern {}", path, pattern);
                    }
                    return matches;
                });

        if (!isPublic) {
            logger.debug("Path {} did not match any public patterns. Available patterns: {}",
                    path, securityProperties.getPublicPaths());
        }

        return isPublic;
    }

    private boolean matchesPattern(String path, String pattern) {
        // Handle /** suffix (matches everything under prefix)
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix) || path.equals(prefix);
        }

        // Handle /* suffix (matches one path segment)
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            if (!path.startsWith(prefix)) {
                return false;
            }
            String remaining = path.substring(prefix.length());
            return remaining.isEmpty() || (remaining.startsWith("/") && !remaining.substring(1).contains("/"));
        }

        // Handle *-service pattern (e.g., /*-service/v3/api-docs)
        if (pattern.contains("*")) {
            String regex = pattern
                    .replace(".", "\\.")
                    .replace("**", ".*")
                    .replace("*", "[^/]*");
            return path.matches(regex);
        }

        // Exact match
        return path.equals(pattern);
    }

    private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"error\": \"Unauthorized\", \"message\": \"%s\", \"status\": 401}", message));
    }
}

