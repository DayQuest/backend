package com.dayquest.dayquestbackend.user;

import com.dayquest.dayquestbackend.user.dto.TwoFactorAuthDTO;
import com.dayquest.dayquestbackend.user.dto.TwoFactorAuthResponseDTO;
import com.dayquest.dayquestbackend.user.dto.TwoFactorAuthVerifyDTO;
import com.dayquest.dayquestbackend.user.models.User;
import com.dayquest.dayquestbackend.user.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the 2FA endpoints.
 */
@RestController
@RequestMapping("/api/users/2fa")
public class TwoFactorAuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(TwoFactorAuthController.class);
    
    private final TwoFactorAuthService twoFactorAuthService;
    private final UserService userService;
    
    @Autowired
    public TwoFactorAuthController(TwoFactorAuthService twoFactorAuthService, 
                                  UserService userService) {
        this.twoFactorAuthService = twoFactorAuthService;
        this.userService = userService;
    }
    
    /**
     * Endpoint for retrieving the 2FA status of a user.
     *
     * @return ResponseEntity with the 2FA status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getTwoFactorAuthStatus() {
        try {
            // Get the current user from the security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            boolean isEnabled = twoFactorAuthService.isTwoFactorAuthEnabled(user.getUuid());
            
            Map<String, Boolean> response = new HashMap<>();
            response.put("enabled", isEnabled);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving 2FA status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }
    
    /**
     * Endpoint for setting up 2FA for a user.
     *
     * @return ResponseEntity with the setup information
     */
    @PostMapping("/setup")
    public ResponseEntity<?> setupTwoFactorAuth() {
        try {
            // Get the current user from the security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            TwoFactorAuthDTO setupInfo = twoFactorAuthService.setupTwoFactorAuth(user.getUuid());
            if (setupInfo == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error setting up 2FA");
            }
            
            return ResponseEntity.ok(setupInfo);
        } catch (Exception e) {
            logger.error("Error setting up 2FA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }
    
    /**
     * Endpoint for enabling 2FA for a user.
     *
     * @param dto DTO with the verification code
     * @return ResponseEntity with success/error message
     */
    @PostMapping("/enable")
    public ResponseEntity<?> enableTwoFactorAuth(@RequestBody TwoFactorAuthDTO dto) {
        try {
            // Get the current user from the security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            boolean success = twoFactorAuthService.enableTwoFactorAuth(user.getUuid(), dto.getCode());
            
            if (success) {
                return ResponseEntity.ok(new TwoFactorAuthResponseDTO(true, "2FA successfully activated"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TwoFactorAuthResponseDTO(false, "Invalid code"));
            }
        } catch (Exception e) {
            logger.error("Error enabling 2FA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }
    
    /**
     * Endpoint for disabling 2FA for a user.
     *
     * @param dto DTO with the verification code
     * @return ResponseEntity with success/error message
     */
    @PostMapping("/disable")
    public ResponseEntity<?> disableTwoFactorAuth(@RequestBody TwoFactorAuthDTO dto) {
        try {
            // Get the current user from the security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
            
            boolean success = twoFactorAuthService.disableTwoFactorAuth(user.getUuid(), dto.getCode());
            
            if (success) {
                return ResponseEntity.ok(new TwoFactorAuthResponseDTO(true, "2FA successfully deactivated"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new TwoFactorAuthResponseDTO(false, "Invalid code"));
            }
        } catch (Exception e) {
            logger.error("Error disabling 2FA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }
    
    /**
     * Endpoint for verifying a 2FA code during login.
     *
     * @param dto DTO with username and verification code
     * @return ResponseEntity with success/error message and possibly token and UUID
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyTwoFactorAuth(@RequestBody TwoFactorAuthVerifyDTO dto) {
        try {
            TwoFactorAuthResponseDTO response = twoFactorAuthService.verifyLoginCode(dto.getUsername(), dto.getCode());
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            logger.error("Error verifying 2FA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }
}
