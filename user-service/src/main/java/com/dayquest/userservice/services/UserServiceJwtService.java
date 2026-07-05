package com.dayquest.userservice.services;

import com.dayquest.common.dto.AuthTokenResponse;
import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service("userServiceJwtService")
public class UserServiceJwtService extends com.dayquest.common.jwt.JwtService {

    private final UserRepository userRepository;

    public UserServiceJwtService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public AuthTokenResponse generateTokenPair(User user) {
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String accessToken = generateAccessToken(user.getUuid(), user.getUsername(), roles);
        String refreshToken = generateRefreshToken(user.getUuid());

        return new AuthTokenResponse(accessToken, refreshToken, getAccessTokenExpiration());
    }


    public String generateToken(UserDetails userDetails) {
        User user = (User) userDetails;
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return generateAccessToken(user.getUuid(), user.getUsername(), roles);
    }

    /**
     * Refresh tokens using a valid refresh token.
     */
    public AuthTokenResponse refreshTokens(String refreshToken) {
        if (!isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token type");
        }

        if (isTokenExpired(refreshToken)) {
            throw new RuntimeException("Refresh token has expired");
        }

        UUID userId = extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return generateTokenPair(user);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final UUID userId = super.extractUserId(token);
            User user = (User) userDetails;
            return (userId.equals(user.getUuid()) && !super.isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }
}