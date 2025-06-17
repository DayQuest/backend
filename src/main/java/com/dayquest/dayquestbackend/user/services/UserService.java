package com.dayquest.dayquestbackend.user.services;

import com.dayquest.dayquestbackend.notification.service.EmailService;
import com.dayquest.dayquestbackend.auth.service.JwtService;
import com.dayquest.dayquestbackend.beta.BetaKey;
import com.dayquest.dayquestbackend.beta.KeyRepository;
import com.dayquest.dayquestbackend.quest.Quest;
import com.dayquest.dayquestbackend.streak.StreakService;
import com.dayquest.dayquestbackend.user.Punishments;
import com.dayquest.dayquestbackend.user.TwoFactorAuthService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.dayquest.dayquestbackend.quest.QuestService;
import com.dayquest.dayquestbackend.user.dto.ResetPasswordRequestDTO;
import com.dayquest.dayquestbackend.user.models.User;
import com.dayquest.dayquestbackend.user.repositories.UserRepository;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private KeyRepository keyRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private QuestService questService;

    @Autowired
    private TwoFactorAuthService twoFactorAuthService;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);


    private final Random random = new Random();
    @Autowired
    private StreakService streakService;

    /**
     * Findet einen Benutzer anhand seines Benutzernamens.
     *
     * @param username Der Benutzername
     * @return Der Benutzer oder null, wenn nicht gefunden
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Prüft, ob für einen Benutzer 2FA aktiviert ist.
     *
     * @param username Der Benutzername
     * @return true, wenn 2FA aktiviert ist, sonst false
     */
    public boolean isTwoFactorAuthRequired(String username) {
        User user = getUserByUsername(username);
        if (user == null) {
            return false;
        }
        return twoFactorAuthService.isTwoFactorAuthEnabled(user.getUuid());
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> registerUser(
            String username, String email, String password, String betaKey) {
        return CompletableFuture.supplyAsync(() -> {
            if (username == null || email == null || password == null || username.isEmpty()
                    || email.isEmpty() || password.isEmpty() || betaKey == null || betaKey.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
            }

            if (userRepository.findByUsername(username) != null) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("User with that name already exists");
            }
            if (userRepository.findByEmail(email) != null) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("User with that email already exists");
            }
            if (!keyRepository.existsByKey(betaKey)) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Invalid beta key");
            }
            if (keyRepository.findByKey(betaKey).isInUse()) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Beta key already in use");
            }

            BetaKey key = keyRepository.findByKey(betaKey);
            List<Quest> topQuests = questService.getTop30PercentQuests().join();
            Quest randomQuest = topQuests.get(new Random().nextInt(topQuests.size()));

            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setUuid(UUID.randomUUID());
            newUser.setVerificationCode(generateVerificationCode());
            newUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
            newUser.setEnabled(false);
            newUser.setAuthorities(List.of("ROLE_USER"));
            newUser.setDailyQuest(randomQuest);

            sendVerificationEmail(newUser);
            userRepository.save(newUser);

            key.setInUse(true);
            key.setUsername(username);
            keyRepository.save(key);

            streakService.createStreak(newUser.getUuid());

            return ResponseEntity.ok("Successfully registered new user");
        });
    }

    @Async
    public CompletableFuture<Boolean> authenticateUser(UUID uuid, String token) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> userOptional = userRepository.findById(uuid);
            if (userOptional.isEmpty() || userOptional.get().getPunishment() == Punishments.TEMP_BANNED || userOptional.get().getPunishment() == Punishments.BANNED) {
                return false;
            }
            User user = userOptional.get();
            return jwtService.isTokenValid(token, user);
        });
    }

    /**
     * Authentifiziert einen Benutzer mit 2FA-Code.
     *
     * @param uuid Der UUID des Benutzers
     * @param token Das JWT-Token
     * @param twoFactorCode Der 2FA-Code (kann null sein, wenn 2FA nicht aktiviert ist)
     * @return true, wenn die Authentifizierung erfolgreich war, sonst false
     */
    @Async
    public CompletableFuture<Boolean> authenticateUserWith2FA(UUID uuid, String token, String twoFactorCode) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> userOptional = userRepository.findById(uuid);
            if (userOptional.isEmpty()) {
                return false;
            }

            if (userOptional.get().getPunishment() == Punishments.TEMP_BANNED || userOptional.get().getPunishment() == Punishments.BANNED){
                return false;
            }
            User user = userOptional.get();
            if (!jwtService.isTokenValid(token, user)) {
                return false;
            }
            if (isTwoFactorAuthRequired(user.getUsername())) {
                return twoFactorAuthService.verifyTwoFactorCode(user.getUuid(), twoFactorCode);
            }
            return true;
        });
    }

    @Async
    public CompletableFuture<Void> assignDailyQuests(List<Quest> topQuests) {
        return CompletableFuture.runAsync(() -> {
            int i = 0;
            if (topQuests == null || topQuests.isEmpty()) {
                throw new IllegalArgumentException("The list of top quests must not be null or empty");
            }

            List<User> allUsers = userRepository.findAll();
            for (User user : allUsers) {
                Quest lastQuest = user.getDailyQuest();
                Quest randomQuest = topQuests.get(random.nextInt(topQuests.size()));
                while (randomQuest.equals(lastQuest) || user.getDoneQuests().contains(randomQuest.getUuid()) && topQuests.size() > 1) {
                    randomQuest = topQuests.get(random.nextInt(topQuests.size()));
                    i++;
                    if (i > 100) {
                        break;
                    }
                }
                user.addDoneQuest(lastQuest.getUuid());
                user.setDailyQuest(randomQuest);
                userRepository.save(user);
            }
        });
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> changeBanStatus(UUID uuid, boolean banned) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(uuid);
            if (user.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (user.get().getPunishment() == Punishments.TEMP_BANNED || user.get().getPunishment() == Punishments.BANNED) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("User already has ban status: " + banned);
            }
            user.get().setPunishment(Punishments.BANNED);

            if (banned) {
                user.get().setUsername(user.get().getUsername() + "_banned");
            } else user.get().setUsername(user.get().getUsername().replaceAll("_banned", ""));

            userRepository.save(user.get());
            return ResponseEntity.ok("Ban status changed successfully");
        });
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> updateUserProfile(UUID uuid, String username) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(uuid);
            if (user.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            user.get().setUsername(username);
            userRepository.save(user.get());
            return ResponseEntity.ok("Updated successfully");
        });
    }

    public void resendVerificationCode(String email) {
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEnabled()) {
                throw new RuntimeException("Account is already verified");
            }
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
            sendVerificationEmail(user);
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    private void sendVerificationEmail(User user) {
        String subject = "Account Verification";
        String verificationCode = user.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Willkommen bei DayQuest!</h2>"
                + "<p style=\"font-size: 16px;\">Hier ist dein Code:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public ResponseEntity<String> verifyAccount(String verificationCode) {
        System.out.println(verificationCode);
        User user = userRepository.findByVerificationCode(verificationCode);
        if (user != null) {
            if (user.getVerificationCode().equals(verificationCode)) {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiresAt(null);
                userRepository.save(user);
                return ResponseEntity.ok(user.getUuid().toString());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Invalid verification code");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }


    @Async
    @Transactional
    public CompletableFuture<Boolean> handleForgotPasswordRequest(String email) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (!user.isEnabled()) {
                    logger.warn("Password reset requested for unverified account: {}", email);
                }

                String token = UUID.randomUUID().toString();
                user.setPasswordResetToken(token);
                user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
                userRepository.save(user);

                try {
                    sendPasswordResetEmailInternal(user, token);
                } catch (MessagingException e) {
                    logger.error("Failed to send password reset email to {}: {}", email, e.getMessage());
                }
            } else {
                logger.info("Password reset requested for non-existent email: {}", email);
            }
            return true;
        });
    }


    private void sendPasswordResetEmailInternal(User user, String token) throws MessagingException {
        String subject = "DayQuest Password Reset Request";

        String frontendResetUrl = "https://apiv2.dayquest.de/api/users/reset-password?token=" + token;

        String htmlMessage = String.format(
                "<html><body style=\"font-family: Arial, sans-serif;\">" +
                        "<div style=\"background-color: #f5f5f5; padding: 20px; text-align: center;\">" +
                        "<h2 style=\"color: #333;\">Password Reset Request</h2>" +
                        "<p style=\"font-size: 16px;\">You (or someone else) requested a password reset for your DayQuest account. " +
                        "If this was not you, please ignore this email.</p>" +
                        "<p style=\"font-size: 16px;\">To reset your password, please click the link below.:</p>" +
                        "<p style=\"margin-top: 20px; margin-bottom: 20px;\"><a href=\"%s\" style=\"background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; font-size: 16px;\">Reset Your Password</a></p>" +
                        "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1); display: inline-block; margin-top:10px;\">" +
                        "</div>" +
                        "<p style=\"font-size: 14px; color: #777; margin-top: 20px;\">This link and token are valid for 1 hour.</p>" +
                        "</div></body></html>",
                frontendResetUrl);

        emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
    }

    @Async
    @Transactional
    public CompletableFuture<String> handleResetPassword(String token, String newPassword) {
        return CompletableFuture.supplyAsync(() -> {
            if (token == null || token.isEmpty() || newPassword == null || newPassword.isEmpty()) {
                throw new IllegalArgumentException("Token and new password must not be empty.");
            }
            Optional<User> userOptional = userRepository.findByPasswordResetToken(token);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (user.getPasswordResetTokenExpiry() != null && user.getPasswordResetTokenExpiry().isAfter(LocalDateTime.now())) {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setPasswordResetToken(null);
                    user.setPasswordResetTokenExpiry(null);
                    userRepository.save(user);
                    return "Password has been reset successfully.";
                } else {
                    user.setPasswordResetToken(null);
                    user.setPasswordResetTokenExpiry(null);
                    userRepository.save(user);
                    throw new IllegalArgumentException("Password reset token is invalid or has expired.");
                }
            } else {
                throw new IllegalArgumentException("Password reset token is invalid or has expired.");
            }
        });
    }
}