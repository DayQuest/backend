package com.dayquest.dayquestbackend.user;

import com.dayquest.dayquestbackend.auth.service.JwtService;
import com.dayquest.dayquestbackend.user.dto.TwoFactorAuthDTO;
import com.dayquest.dayquestbackend.user.dto.TwoFactorAuthResponseDTO;
import com.dayquest.dayquestbackend.user.models.User;
import com.dayquest.dayquestbackend.user.repositories.UserRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

/**
 * Service for managing Two-Factor Authentication.
 */
@Service
public class TwoFactorAuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(TwoFactorAuthService.class);
    private static final String ISSUER = "DayQuest";
    
    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    
    @Autowired
    public TwoFactorAuthService(TwoFactorAuthRepository twoFactorAuthRepository, 
                               UserRepository userRepository,
                               JwtService jwtService) {
        this.twoFactorAuthRepository = twoFactorAuthRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }
    
    /**
     * Generates a new secret key and QR code for 2FA setup.
     *
     * @param userUuid The UUID of the user
     * @return DTO with setup information or null if the user was not found
     */
    @Transactional
    public TwoFactorAuthDTO setupTwoFactorAuth(UUID userUuid) {
        Optional<User> userOptional = userRepository.findById(userUuid);
        if (userOptional.isEmpty()) {
            logger.error("User with UUID {} not found", userUuid);
            return null;
        }
        
        User user = userOptional.get();
        
        // Generate a new secret key
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secretKey = secretGenerator.generate();
        
        // Create QR code data
        QrData qrData = new QrData.Builder()
                .label(user.getUsername())
                .secret(secretKey)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        
        // Generate QR code
        QrGenerator qrGenerator = new ZxingPngQrGenerator();
        byte[] qrCodeImage;
        try {
            qrCodeImage = qrGenerator.generate(qrData);
        } catch (QrGenerationException e) {
            logger.error("Error generating QR code", e);
            return null;
        }
        
        // Convert QR code to data URI
        String qrCodeUrl = getDataUriForImage(qrCodeImage, qrGenerator.getImageMimeType());
        
        // Save or update 2FA configuration in the database
        TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
                .orElse(new TwoFactorAuth(user, secretKey));
        
        // If a configuration already exists, update the secret key
        if (twoFactorAuth.getId() != null) {
            twoFactorAuth.setSecretKey(secretKey);
            twoFactorAuth.setEnabled(false); // Disable 2FA until the user activates it again
        }
        
        twoFactorAuthRepository.save(twoFactorAuth);
        
        // Create the OTP auth URL for manual configuration
        String otpAuthUrl = qrData.getUri();
        
        return new TwoFactorAuthDTO(secretKey, qrCodeUrl, otpAuthUrl);
    }
    
    /**
     * Activates Two-Factor Authentication for a user.
     *
     * @param userUuid The UUID of the user
     * @param code The verification code entered by the user
     * @return true if activation was successful, otherwise false
     */
    @Transactional
    public boolean enableTwoFactorAuth(UUID userUuid, String code) {
        Optional<User> userOptional = userRepository.findById(userUuid);
        if (userOptional.isEmpty()) {
            logger.error("User with UUID {} not found", userUuid);
            return false;
        }
        
        User user = userOptional.get();
        Optional<TwoFactorAuth> twoFactorAuthOpt = twoFactorAuthRepository.findByUser(user);
        
        if (twoFactorAuthOpt.isEmpty()) {
            logger.error("No 2FA configuration found for user with UUID {}", userUuid);
            return false;
        }
        
        TwoFactorAuth twoFactorAuth = twoFactorAuthOpt.get();
        
        // Verify the code
        if (verifyCode(twoFactorAuth.getSecretKey(), code)) {
            twoFactorAuth.setEnabled(true);
            twoFactorAuthRepository.save(twoFactorAuth);
            logger.info("2FA enabled for user with UUID {}", userUuid);
            return true;
        }
        
        logger.warn("Invalid verification code provided for enabling 2FA for user with UUID {}", userUuid);
        return false;
    }
    
    /**
     * Deactivates Two-Factor Authentication for a user.
     *
     * @param userUuid The UUID of the user
     * @param code The verification code entered by the user
     * @return true if deactivation was successful, otherwise false
     */
    @Transactional
    public boolean disableTwoFactorAuth(UUID userUuid, String code) {
        Optional<User> userOptional = userRepository.findById(userUuid);
        if (userOptional.isEmpty()) {
            logger.error("User with UUID {} not found", userUuid);
            return false;
        }
        
        User user = userOptional.get();
        Optional<TwoFactorAuth> twoFactorAuthOpt = twoFactorAuthRepository.findByUser(user);
        
        if (twoFactorAuthOpt.isEmpty()) {
            logger.error("No 2FA configuration found for user with UUID {}", userUuid);
            return false;
        }
        
        TwoFactorAuth twoFactorAuth = twoFactorAuthOpt.get();
        
        // Check if 2FA is even enabled
        if (!twoFactorAuth.isEnabled()) {
            logger.warn("Attempted to disable 2FA for user with UUID {} but it's not enabled", userUuid);
            return false;
        }
        
        // Verify the code
        if (verifyCode(twoFactorAuth.getSecretKey(), code)) {
            twoFactorAuth.setEnabled(false);
            twoFactorAuthRepository.save(twoFactorAuth);
            logger.info("2FA disabled for user with UUID {}", userUuid);
            return true;
        }
        
        logger.warn("Invalid verification code provided for disabling 2FA for user with UUID {}", userUuid);
        return false;
    }
    
    /**
     * Checks if Two-Factor Authentication is enabled for a user.
     *
     * @param userUuid The UUID of the user
     * @return true if 2FA is enabled, otherwise false
     */
    public boolean isTwoFactorAuthEnabled(UUID userUuid) {
        Optional<User> userOptional = userRepository.findById(userUuid);
        if (userOptional.isEmpty()) {
            logger.error("User with UUID {} not found", userUuid);
            return false;
        }
        
        User user = userOptional.get();
        Optional<TwoFactorAuth> twoFactorAuthOpt = twoFactorAuthRepository.findByUser(user);
        
        return twoFactorAuthOpt.isPresent() && twoFactorAuthOpt.get().isEnabled();
    }
    
    /**
     * Verifies a 2FA code for a specific user.
     *
     * @param userUuid The UUID of the user
     * @param code The verification code entered by the user
     * @return true if the code is valid, otherwise false
     */
    public boolean verifyTwoFactorCode(UUID userUuid, String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        
        Optional<User> userOptional = userRepository.findById(userUuid);
        if (userOptional.isEmpty()) {
            logger.error("User with UUID {} not found", userUuid);
            return false;
        }
        
        User user = userOptional.get();
        Optional<TwoFactorAuth> twoFactorAuthOpt = twoFactorAuthRepository.findByUser(user);
        
        if (twoFactorAuthOpt.isEmpty() || !twoFactorAuthOpt.get().isEnabled()) {
            // If 2FA is not set up or not enabled, the verification is considered successful
            return true;
        }
        
        TwoFactorAuth twoFactorAuth = twoFactorAuthOpt.get();
        
        // Verify the code
        return verifyCode(twoFactorAuth.getSecretKey(), code);
    }
    
    /**
     * Verifies a 2FA code during login.
     *
     * @param username The username
     * @param code The verification code entered by the user
     * @return DTO with success/error message and possibly token and UUID
     */
    public TwoFactorAuthResponseDTO verifyLoginCode(String username, String code) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.error("User with username {} not found", username);
            return new TwoFactorAuthResponseDTO(false, "User not found");
        }
        
        Optional<TwoFactorAuth> twoFactorAuthOpt = twoFactorAuthRepository.findByUser(user);
        if (twoFactorAuthOpt.isEmpty() || !twoFactorAuthOpt.get().isEnabled()) {
            logger.error("2FA is not enabled for user {}", username);
            return new TwoFactorAuthResponseDTO(false, "2FA is not enabled");
        }
        
        TwoFactorAuth twoFactorAuth = twoFactorAuthOpt.get();
        
        // Verify the code
        if (verifyCode(twoFactorAuth.getSecretKey(), code)) {
            // Generate JWT token and UUID for the user
            String token = jwtService.generateToken(user);
            String uuid = user.getUuid().toString();
            
            return new TwoFactorAuthResponseDTO(true, token, uuid);
        }
        
        return new TwoFactorAuthResponseDTO(false, "Invalid code");
    }
    
    /**
     * Verifies a TOTP code.
     *
     * @param secretKey The secret key
     * @param code The code to verify
     * @return true if the code is valid, otherwise false
     */
    private boolean verifyCode(String secretKey, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        
        // Check the current, previous, and next time period for a better user experience
        return verifier.isValidCode(secretKey, code);
    }
}
