package com.dayquest.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT service providing common JWT operations.
 * Auto-registered as Spring bean via @Service annotation.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:900000}")
    private long accessTokenExpiration; // 15 minutes default

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshTokenExpiration;

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String ROLES_CLAIM = "roles";
    private static final String USERNAME_CLAIM = "username";

    public UUID extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String uuidString = claims.getSubject();
            return UUID.fromString(uuidString);
        } catch (Exception e) {
            throw new RuntimeException("User ID extraction error: " + e.getMessage(), e);
        }
    }

    public String extractUsername(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get(USERNAME_CLAIM, String.class);
        } catch (Exception e) {
            return null; // Username may not be present in all tokens
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get(ROLES_CLAIM, List.class);
    }

    public String extractTokenType(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get(TOKEN_TYPE_CLAIM, String.class);
    }

    // Generate access token
    public String generateAccessToken(UUID userId, List<String> roles) {
        return generateAccessToken(userId, null, roles);
    }

    public String generateAccessToken(UUID userId, String username, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS);
        claims.put(ROLES_CLAIM, roles);
        if (username != null) {
            claims.put(USERNAME_CLAIM, username);
        }
        return buildToken(claims, userId, accessTokenExpiration);
    }

    // Generate refresh token
    public String generateRefreshToken(UUID userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, TOKEN_TYPE_REFRESH);
        return buildToken(claims, userId, refreshTokenExpiration);
    }

    // Legacy method for backward compatibility
    public String generateToken(UUID userId, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS);
        return buildToken(claims, userId, accessTokenExpiration);
    }

    public String generateToken(UUID userId) {
        return generateAccessToken(userId, List.of("ROLE_USER"));
    }

    private String buildToken(Map<String, Object> extraClaims, UUID userId, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userId.toString())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw e; // Re-throw to handle specifically
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean isTokenValid(String token, UUID userId) {
        try {
            final UUID extractedUserId = extractUserId(token);
            return (extractedUserId.equals(userId) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            String type = extractTokenType(token);
            return TOKEN_TYPE_ACCESS.equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            String type = extractTokenType(token);
            return TOKEN_TYPE_REFRESH.equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
