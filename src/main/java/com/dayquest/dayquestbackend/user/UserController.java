package com.dayquest.dayquestbackend.user;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.dayquest.dayquestbackend.activity.ActivityUpdater;
import com.dayquest.dayquestbackend.auth.AuthController;
import com.dayquest.dayquestbackend.auth.service.JwtService;
import com.dayquest.dayquestbackend.common.dto.UuidDTO;
import com.dayquest.dayquestbackend.common.utils.ImageUtil;
import com.dayquest.dayquestbackend.quest.Quest;
import com.dayquest.dayquestbackend.quest.dto.QuestDTO;
import com.dayquest.dayquestbackend.quest.QuestService;
import com.dayquest.dayquestbackend.streak.StreakService;
import com.dayquest.dayquestbackend.user.dto.*;
import com.dayquest.dayquestbackend.user.repositories.UserRepository;
import com.dayquest.dayquestbackend.user.models.User;
import com.dayquest.dayquestbackend.user.services.FollowService;
import com.dayquest.dayquestbackend.user.services.UserService;
import com.dayquest.dayquestbackend.video.models.Video;
import com.dayquest.dayquestbackend.video.repository.VideoRepository;
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
    @Autowired
    private FollowService followService;

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
                if (user.getPunishment() == Punishments.BANNED || user.getPunishment() == Punishments.TEMP_BANNED) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User has been banned");
                }
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                return ResponseEntity.ok("User authenticated");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }
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

    @PostMapping("/setprofilepicture")
    public ResponseEntity<String> setProfilePicture(@RequestParam("file") MultipartFile file, @RequestParam("uuid") UUID uuid, @RequestHeader("Authorization") String token) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        if (!userService.authenticateUser(uuid, token).join()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        try {
            byte[] fileBytes = imageUtil.compressImage(file);
            User user = userRepository.findById(uuid).orElseThrow(() -> new RuntimeException("User not found"));
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
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(uuid).orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(user.getBadges());
        });
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
}