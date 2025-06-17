package com.dayquest.dayquestbackend.user;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.dayquest.dayquestbackend.activity.ActivityUpdater;
import com.dayquest.dayquestbackend.auth.AuthController;
import com.dayquest.dayquestbackend.auth.service.JwtService;
import com.dayquest.dayquestbackend.badge.BadgeRepository;
import com.dayquest.dayquestbackend.common.dto.UuidDTO;
import com.dayquest.dayquestbackend.common.utils.ImageUtil;
import com.dayquest.dayquestbackend.quest.Quest;
import com.dayquest.dayquestbackend.quest.dto.QuestDTO;
import com.dayquest.dayquestbackend.quest.QuestService;
import com.dayquest.dayquestbackend.streak.StreakService;
import com.dayquest.dayquestbackend.user.dto.*;
import com.dayquest.dayquestbackend.user.models.User;
import com.dayquest.dayquestbackend.user.repositories.FollowRepository;
import com.dayquest.dayquestbackend.user.repositories.UserRepository;
import com.dayquest.dayquestbackend.user.services.FollowService;
import com.dayquest.dayquestbackend.user.services.UserService;
import com.dayquest.dayquestbackend.video.repository.VideoRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final String DEFAULT_PROFILE_PICTURE_URL = "https://static.vecteezy.com/system/resources/thumbnails/003/337/584/small/default-avatar-photo-placeholder-profile-icon-vector.jpg";
    private static final String PROFILE_PICTURE_BASE_URL = "https://api.dayquest.de/api/users/profilepicture/";

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private QuestService questService;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private StreakService streakService;
    @Autowired private ActivityUpdater activityUpdater;
    @Autowired private ImageUtil imageUtil;
    @Autowired private VideoRepository videoRepository;
    @Autowired private AuthController authController;
    @Autowired private FollowRepository followRepository;
    @Autowired private FollowService followService;
    @Autowired
    private BadgeRepository badgeRepository;

    @PostMapping("/status")
    public ResponseEntity<Object> status() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<String>> registerUser(@RequestBody UserDTO userDTO) {
        return authController.registerUser(userDTO);
    }

    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<LoginResponseDTO>> loginUser(@RequestBody LoginDTO loginDTO) {
        return authController.loginUser(loginDTO);
    }
    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestBody String token) {
        return userService.verifyAccount(token);
    }

    @PostMapping("/resendcode")
    @Async
    public CompletableFuture<ResponseEntity<String>> resendVerificationCode(@RequestBody String email) {
        userService.resendVerificationCode(email);
        return CompletableFuture.completedFuture(ResponseEntity.ok("Verification code resent"));
    }

    @PostMapping("/auth")
    @Async
    public CompletableFuture<ResponseEntity<String>> authUser(@RequestBody UUID uuid, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            if (userService.authenticateUser(uuid, token).join()) {
                streakService.checkStreak(uuid);
                User user = userRepository.findById(uuid).orElseThrow(() -> new RuntimeException("User not found"));
                if (user.getPunishment() == Punishments.TEMP_BANNED || user.getPunishment() == Punishments.BANNED) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is banned");
                }
                return ResponseEntity.ok("User authenticated");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        });
    }

    @DeleteMapping()
    @Async
    public CompletableFuture<ResponseEntity<String>> deleteUser(@RequestHeader("Authorization") String token, @RequestBody PasswordDTO passwordDTO) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            if (!passwordEncoder.matches(passwordDTO.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password");
            }
            userRepository.delete(user);
            return ResponseEntity.ok("User deleted successfully");
        });
    }

    @PostMapping("/auth2fa")
    @Async
    public CompletableFuture<ResponseEntity<String>> authUserWith2FA(
            @RequestBody UUID uuid, 
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String twoFactorCode) {
        return CompletableFuture.supplyAsync(() -> {
            if (userService.authenticateUserWith2FA(uuid, token, twoFactorCode).join()) {
                streakService.checkStreak(uuid);
                User user = userRepository.findById(uuid).orElseThrow(() -> new RuntimeException("User not found"));
                if (user.getPunishment() == Punishments.TEMP_BANNED || user.getPunishment() == Punishments.BANNED) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is banned");
                }
                return ResponseEntity.ok("User authenticated");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        });
    }

    private String extractUsername(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtService.extractUsername(token);
    }

    @GetMapping("/{uuid}")
    @Async
    public CompletableFuture<ResponseEntity<ProfileDTO>> getUserByUuid(
            @PathVariable UUID uuid, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            User userWithVideos = userRepository.findByIdWithVideos(uuid);
            if (userWithVideos == null) {
                return ResponseEntity.notFound().build();
            }
            String requesterUsername = extractUsername(token);
            User requester = userRepository.findByUsername(requesterUsername);
            return ResponseEntity.ok(createProfileDTO(userWithVideos, requester));
        });
    }

    @GetMapping("/{uuid}/followers")
    @Async
    public CompletableFuture<ResponseEntity<List<UUID>>> getFollowers(@PathVariable UUID uuid, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            CompletableFuture<List<UUID>> followersFuture = followService.getFollowerPage(token, page, size);
            List<UUID> followers = followersFuture.join();
            return ResponseEntity.ok(followers);
        });
    }

    @GetMapping("/{uuid}/following")
    @Async
    public CompletableFuture<ResponseEntity<List<UUID>>> getFollowing(@PathVariable UUID uuid, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            CompletableFuture<List<UUID>> followingFuture = followService.getFollowedPage(token, page, size);
            List<UUID> following = followingFuture.join();
            return ResponseEntity.ok(following);
        });
    }

    @PostMapping("/{uuid}/follow")
    @Async
    public CompletableFuture<ResponseEntity<String>> followUser(@PathVariable UUID uuid, @RequestHeader("Authorization") String token, @RequestBody(required = false) UuidDTO videoUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            User userToFollow = userRepository.findById(uuid).orElseThrow(() -> new RuntimeException("User not found"));

            if (uuid.equals(user.getUuid())) {
                return ResponseEntity.unprocessableEntity().body("Cannot follow yourself");
            }
            if (followService.isFollowing(user.getUuid(), uuid).join()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("User already followed");
            }

            userToFollow.setFollowers(userToFollow.getFollowers() + 1);
            followService.followUser(token, uuid).join();

            activityUpdater.increaseInteractions(user);
            userRepository.save(userToFollow);

            return ResponseEntity.ok("User followed");
        });
    }

    @DeleteMapping("/{uuid}/follow")
    @Async
    public CompletableFuture<ResponseEntity<String>> unfollowUser(@PathVariable UUID uuid, @RequestHeader("Authorization") String token) {
        return followService.unfollowUser(token, uuid)
                .thenApply(response -> {
                    if (response.getStatusCode() == HttpStatus.OK) {
                        User userToUnfollow = userRepository.findById(uuid).orElseThrow(() -> new RuntimeException("User not found"));
                        userToUnfollow.setFollowers(userToUnfollow.getFollowers() - 1);
                        userRepository.save(userToUnfollow);
                    }
                    return response;
                });
    }

    @GetMapping("/search")
    @Async
    public CompletableFuture<ResponseEntity<Map<String, Object>>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return CompletableFuture.supplyAsync(() -> {
            Page<User> userPage = userRepository.findUsersByUsernameContainingIgnoreCase(query, PageRequest.of(page, size));
            List<ProfileDTO> profileDTOs = userPage.getContent().stream()
                    .map(user -> createProfileDTO(user, null))
                    .collect(Collectors.toList());

            if (profileDTOs.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("users", profileDTOs);
            response.put("currentPage", userPage.getNumber());
            response.put("totalItems", userPage.getTotalElements());
            response.put("totalPages", userPage.getTotalPages());

            return ResponseEntity.ok(response);
        });
    }

    @GetMapping("{username}/followersAsInt")
    @Async
    public CompletableFuture<ResponseEntity<Integer>> getFollowersAsInt(@PathVariable String username) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(user.getFollowers());
        });
    }

    @GetMapping("/profile/{username}")
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<ResponseEntity<ProfileDTO>> getUserByUsername(@PathVariable String username, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            User userWithVideos = userRepository.findByUsernameWithVideos(username);
            User user = userRepository.findByUsername(jwtService.extractUsername(token.substring(7)));
            if (userWithVideos == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(createProfileDTO(userWithVideos, user));
        });
    }

    private ProfileDTO createProfileDTO(User userWithVideos, User requester) {
        return new ProfileDTO(
                userWithVideos.getUsername(),
                PROFILE_PICTURE_BASE_URL + userWithVideos.getUsername(),
                userWithVideos.getPostedVideos(),
                userWithVideos.getDailyQuest(),
                userWithVideos.getPunishment() == Punishments.BANNED,
                userWithVideos.getFollowers(),
                requester != null && followService.isFollowing(requester.getUuid(), userWithVideos.getUuid()).join(),
                userWithVideos.getBadges()
        );
    }

    @GetMapping("/{username}/uuid")
    @Async
    public CompletableFuture<ResponseEntity<UUID>> getUuidByUsername(@PathVariable String username) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(user.getUuid());
        });
    }

    @GetMapping("/profilepicture/{username}")
    @Async
    public CompletableFuture<ResponseEntity<ByteArrayResource>> getDecodedImage(@PathVariable("username") String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = userRepository.findByUsername(username);
                byte[] imageBytes;

                if (user == null || user.getProfilePicture() == null) {
                    ClassPathResource defaultPicture = new ClassPathResource("pfp.jpg");
                    imageBytes = defaultPicture.getInputStream().readAllBytes();
                } else {
                    imageBytes = user.getProfilePicture();
                }

                ByteArrayResource resource = new ByteArrayResource(imageBytes);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_JPEG);

                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(imageBytes.length)
                        .body(resource);

            } catch (IOException | IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        });
    }

    @PutMapping("/email")
    @Async
    public CompletableFuture<ResponseEntity<?>> updateEmail (@RequestBody @Valid UpdateEmailDTO updateEmailDTO, @RequestHeader("Authorization") String token){
        User user = userRepository.findByUsername(jwtService.extractUsername(token.substring(7)));
        if (user == null) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
        }
        if (!passwordEncoder.matches(updateEmailDTO.getPassword(), user.getPassword())) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password"));
        }
        if (userRepository.findByEmailIgnoreCase(updateEmailDTO.getEmail()).isPresent()) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.CONFLICT).body("Email already taken"));
        }
        user.setEmail(updateEmailDTO.getEmail());
        userRepository.save(user);
        return CompletableFuture.completedFuture(ResponseEntity.ok("Email updated successfully"));
    }

    @PutMapping("/password")
    @Async
    public CompletableFuture<ResponseEntity<String>> updatePassword(@RequestBody @Valid UpdatePasswordDTO updatePasswordDTO, @RequestHeader("Authorization") String token) {
        User user = userRepository.findByUsername(jwtService.extractUsername(token.substring(7)));
        if (user == null) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
        }
        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), user.getPassword())) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid old password"));
        }
        user.setPassword(passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
        userRepository.save(user);
        return CompletableFuture.completedFuture(ResponseEntity.ok("Password updated successfully"));
    }

    @PostMapping("/setprofilepicture")
    public ResponseEntity<String> setProfilePicture(@RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String token) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        /*if (!userService.authenticateUserWith2FA(uuid, token, null).join()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }*/

        try {
            User user = userRepository.findByUsername(jwtService.extractUsername(token.substring(7)));
            if(user == null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            byte[] fileBytes = imageUtil.compressImage(file);
            user.setProfilePicture(fileBytes);
            userRepository.save(user);
            return ResponseEntity.ok("Profile picture uploaded successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process the file");
        }
    }

    @PutMapping("/me")
    @Async
    public CompletableFuture<ResponseEntity<String>> updateUserProfile(@RequestBody UpdateUserDTO updateUserDTO, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            if (updateUserDTO.getUsername() != null) {
                user.setUsername(updateUserDTO.getUsername());
            }
            if (updateUserDTO.getEmail() != null) {
                user.setEmail(updateUserDTO.getEmail());
            }
            userRepository.save(user);
            return ResponseEntity.ok("User profile updated");
        });
    }

    @GetMapping("{uuid}/isFollowed")
    @Async
    public CompletableFuture<ResponseEntity<Boolean>> isFollowed(@PathVariable UUID uuid, @RequestHeader("Authorization") String token) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(false));
        }
        String username = jwtService.extractUsername(token.substring(7));
        User user = userRepository.findByUsername(username);
        return followService.isFollowing(user.getUuid(), uuid).join()
                ? CompletableFuture.completedFuture(ResponseEntity.ok(true))
                : CompletableFuture.completedFuture(ResponseEntity.ok(false));
    }

    @PostMapping("/rerollQuest")
    @Async
    public CompletableFuture<ResponseEntity<? extends Object>> rerollQuest(@RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            if (user.getLastReroll() == null || user.getLastReroll().plusDays(1).isBefore(LocalDateTime.now())) {
                user.setLeftRerolls(3);
            }
            if (user.getLeftRerolls() == 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("No rerolls left");
            }
            List<Quest> topQuests = questService.getTop30PercentQuests().join();
            Quest newQuest;
            do {
                newQuest = topQuests.get(new Random().nextInt(topQuests.size()));
            } while (user.getDailyQuest().equals(newQuest));
            user.setDailyQuest(newQuest);
            user.setLastReroll(LocalDateTime.now());
            user.setLeftRerolls(user.getLeftRerolls() - 1);
            userRepository.save(user);
            activityUpdater.increaseInteractions(user);
            return ResponseEntity.ok(new QuestDTO(newQuest));
        });
    }

    @GetMapping("/rerolls")
    @Async
    public CompletableFuture<ResponseEntity<Integer>> getRerolls(@RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            return user.getLastReroll() == null || user.getLastReroll().plusDays(1).isBefore(LocalDateTime.now()) ? ResponseEntity.ok(3) : ResponseEntity.ok(user.getLeftRerolls());
        });
    }

    @PutMapping("/{uuid}/badge")
    @Async
    public CompletableFuture<ResponseEntity<String>> addBadge(@PathVariable UUID uuid, @RequestBody UUID badgeId, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user.getAuthorities().stream().noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is not an admin");
            }
            User userToAddBadge = userRepository.findById(uuid).orElseThrow(() -> new RuntimeException("User not found"));
            if (userToAddBadge.getBadges().contains(badgeId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Badge already added");
            }
            userToAddBadge.getBadges().add(badgeId);
            userRepository.save(userToAddBadge);
            return ResponseEntity.ok("Badge added");
        });
    }

    @GetMapping("/{uuid}/badges")
    @Async
    public CompletableFuture<ResponseEntity<List<UUID>>> getBadges(@PathVariable UUID uuid) {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(badgeRepository.findBadgeIdsByUserId(uuid)));
    }

    @PostMapping("/forgot-password")
    @Async
    public CompletableFuture<ResponseEntity<String>> forgotPassword(@RequestBody @Valid ForgotPasswordRequestDTO forgotPasswordRequestDTO) {
        return userService.handleForgotPasswordRequest(forgotPasswordRequestDTO.getEmail())
                .thenApply(success -> ResponseEntity.ok("If an account with this email exists, a password reset link has been sent."))
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request: " + ex.getMessage()));
    }

    @DeleteMapping("/{uuid}/badge")
    @Async
    public CompletableFuture<ResponseEntity<String>> removeBadge(@PathVariable UUID uuid, @RequestBody UUID badgeId, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user.getAuthorities().stream()
                    .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is not an admin");
            }
            User userToRemoveBadge = userRepository.findById(uuid).orElse(null);
            if (userToRemoveBadge == null) {
                return ResponseEntity.notFound().build();
            }
            if (!userToRemoveBadge.getBadges().contains(badgeId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Badge not found");
            }
            userToRemoveBadge.getBadges().remove(badgeId);
            userRepository.save(userToRemoveBadge);
            return ResponseEntity.ok("Badge removed");
        });
    }
    @PostMapping("/reset-password")
    @Async
    public CompletableFuture<ResponseEntity<String>> resetPassword(@RequestBody ResetPasswordRequestDTO resetPasswordDTO) {
        return userService.handleResetPassword(resetPasswordDTO.getToken(), resetPasswordDTO.getNewPassword())
                .thenApply(message -> ResponseEntity.ok(message))
                .exceptionally(ex -> {
                    if (ex.getCause() instanceof IllegalArgumentException) {
                        return ResponseEntity.badRequest().body(ex.getCause().getMessage());
                    }
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("An error occurred while resetting the password.");
                });
    }

    @GetMapping("/reset-password")
    public ResponseEntity<String> resetPasswordPage(@RequestParam(required = false) String token) {
        String html = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>DayQuest - Reset Password</title>
        <style>
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                min-height: 100vh;
                display: flex;
                justify-content: center;
                align-items: center;
                padding: 20px;
            }
            
            .container {
                background: white;
                border-radius: 15px;
                box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
                padding: 40px;
                width: 100%;
                max-width: 450px;
                text-align: center;
            }
            
            .logo {
                font-size: 2.5em;
                font-weight: bold;
                color: #667eea;
                margin-bottom: 10px;
            }
            
            .subtitle {
                color: #666;
                margin-bottom: 30px;
                font-size: 16px;
            }
            
            .form-group {
                margin-bottom: 20px;
                text-align: left;
            }
            
            label {
                display: block;
                margin-bottom: 8px;
                color: #333;
                font-weight: 500;
            }
            
            input[type="password"] {
                width: 100%;
                padding: 15px;
                border: 2px solid #e1e5e9;
                border-radius: 8px;
                font-size: 16px;
                transition: border-color 0.3s ease;
            }
            
            input[type="password"]:focus {
                outline: none;
                border-color: #667eea;
            }
            
            .btn {
                width: 100%;
                padding: 15px;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                border: none;
                border-radius: 8px;
                font-size: 16px;
                font-weight: 600;
                cursor: pointer;
                transition: transform 0.2s ease;
                margin-top: 10px;
            }
            
            .btn:hover {
                transform: translateY(-2px);
            }
            
            .btn:disabled {
                opacity: 0.6;
                cursor: not-allowed;
                transform: none;
            }
            
            .message {
                padding: 15px;
                border-radius: 8px;
                margin-bottom: 20px;
                font-weight: 500;
            }
            
            .error {
                background-color: #fee;
                border: 1px solid #fcc;
                color: #c33;
            }
            
            .success {
                background-color: #efe;
                border: 1px solid #cfc;
                color: #363;
            }
            
            .loading {
                display: none;
                margin-top: 10px;
            }
            
            .spinner {
                border: 3px solid #f3f3f3;
                border-top: 3px solid #667eea;
                border-radius: 50%;
                width: 30px;
                height: 30px;
                animation: spin 1s linear infinite;
                margin: 0 auto;
            }
            
            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }
            
            .password-requirements {
                font-size: 14px;
                color: #666;
                text-align: left;
                margin-top: 5px;
            }
            
            .password-requirements ul {
                margin-left: 20px;
                margin-top: 5px;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="logo">DayQuest</div>
            <div class="subtitle">Reset Your Password</div>
            
            <div id="message"></div>
            
            <form id="resetForm">
                <div class="form-group">
                    <label for="newPassword">New Password:</label>
                    <input type="password" id="newPassword" name="newPassword" required minlength="6">
                    <div class="password-requirements">
                        <p>Password requirements:</p>
                        <ul>
                            <li>At least 6 characters long</li>
                            <li>Mix of letters and numbers recommended</li>
                        </ul>
                    </div>
                </div>
                
                <div class="form-group">
                    <label for="confirmPassword">Confirm New Password:</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" required minlength="6">
                </div>
                
                <button type="submit" class="btn" id="submitBtn">Reset Password</button>
                
                <div class="loading" id="loading">
                    <div class="spinner"></div>
                    <p>Resetting password...</p>
                </div>
            </form>
        </div>
        
        <script>
            const urlParams = new URLSearchParams(window.location.search);
            const token = urlParams.get('token');
            const messageDiv = document.getElementById('message');
            const form = document.getElementById('resetForm');
            const submitBtn = document.getElementById('submitBtn');
            const loading = document.getElementById('loading');
            
            if (!token) {
                showMessage('Invalid or missing reset token. Please request a new password reset.', 'error');
                submitBtn.disabled = true;
            }
            
            form.addEventListener('submit', async function(e) {
                e.preventDefault();
                
                const newPassword = document.getElementById('newPassword').value;
                const confirmPassword = document.getElementById('confirmPassword').value;
                
                if (newPassword !== confirmPassword) {
                    showMessage('Passwords do not match. Please try again.', 'error');
                    return;
                }
                
                if (newPassword.length < 6) {
                    showMessage('Password must be at least 6 characters long.', 'error');
                    return;
                }
                
                submitBtn.disabled = true;
                loading.style.display = 'block';
                messageDiv.innerHTML = '';
                
                try {
                    const response = await fetch('/api/users/reset-password', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({
                            token: token,
                            newPassword: newPassword
                        })
                    });
                    
                    const result = await response.text();
                    
                    if (response.ok) {
                        showMessage('Password reset successfully! You can now log in with your new password.', 'success');
                        form.style.display = 'none';
                    } else {
                        showMessage(result || 'Failed to reset password. Please try again.', 'error');
                        submitBtn.disabled = false;
                    }
                } catch (error) {
                    showMessage('Network error. Please check your connection and try again.', 'error');
                    submitBtn.disabled = false;
                } finally {
                    loading.style.display = 'none';
                }
            });
            
            function showMessage(text, type) {
                messageDiv.innerHTML = `<div class="message ${type}">${text}</div>`;
            }
        </script>
    </body>
    </html>
    """;

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }
}