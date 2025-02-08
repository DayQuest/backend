package com.dayquest.dayquestbackend.user;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import com.dayquest.dayquestbackend.activity.ActivityUpdater;
import com.dayquest.dayquestbackend.authentication.service.JwtService;
import com.dayquest.dayquestbackend.common.utils.ImageUtil;
import com.dayquest.dayquestbackend.quest.Quest;
import com.dayquest.dayquestbackend.quest.dto.QuestDTO;
import com.dayquest.dayquestbackend.quest.QuestService;
import com.dayquest.dayquestbackend.streak.StreakService;
import com.dayquest.dayquestbackend.user.dto.ProfileDTO;
import com.dayquest.dayquestbackend.user.dto.UpdateUserDTO;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestService questService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;
    @Autowired
    private StreakService streakService;

    @Autowired
    private ActivityUpdater activityUpdater;

    @Autowired
    private ImageUtil imageUtil;

    @PostMapping("/status")
    public ResponseEntity<Object> status() {
        return ResponseEntity.ok().build();
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
        return auth(uuid, token);
    }

    private CompletableFuture<ResponseEntity<String>> auth(UUID uuid, String token) {
        return CompletableFuture.supplyAsync(() -> {
            if (userService.authenticateUser(uuid, token).join()) {
                streakService.checkStreak(uuid);
                User user = userRepository.findById(uuid).get();
                if (user.isBanned()) {
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

    @GetMapping("/{uuid}")
    @Async
    public CompletableFuture<ResponseEntity<ProfileDTO>> getUserByUuid(@PathVariable UUID uuid, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            String username = userRepository.findById(uuid).get().getUsername();
            User user = userRepository.findByUsername(jwtService.extractUsername(token.substring(7)));
            User userWithVideos = userRepository.findByUsernameWithVideos(username);
            if (userWithVideos == null) {
                return ResponseEntity.notFound().build();
            }
            String profilePictureLink = "http://77.90.21.53:8010/api/users/profilepicture/" + username;
            ProfileDTO profileDTO = new ProfileDTO(
                    userWithVideos.getUsername(),
                    profilePictureLink,
                    userWithVideos.getPostedVideos(),
                    userWithVideos.getDailyQuest(),
                    userWithVideos.isBanned(),
                    userWithVideos.getFollowers(),
                    user.getFollowedUsers().contains(uuid),
                    userWithVideos.getBadges()
            );
            return ResponseEntity.ok(profileDTO);
        });
    }

    @GetMapping("/{uuid}/followers")
    @Async
    public CompletableFuture<ResponseEntity<List<UUID>>> getFollowers(@PathVariable UUID uuid, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(uuid).get();
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            List<UUID> followers = user.getFollowerList().stream()
                    .skip((long) page * size)
                    .limit(size)
                    .toList();
            return ResponseEntity.ok(followers);
        });
    }

    @GetMapping("/{uuid}/following")
    @Async
    public CompletableFuture<ResponseEntity<List<UUID>>> getFollowing(@PathVariable UUID uuid, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(uuid).get();
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            List<UUID> following = user.getFollowedUsers().stream()
                    .skip((long) page * size)
                    .limit(size)
                    .toList();
            return ResponseEntity.ok(following);
        });
    }

    @PostMapping("/{uuid}/follow")
    @Async
    public CompletableFuture<ResponseEntity<String>> followUser(@PathVariable UUID uuid, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            User userToFollow = userRepository.findById(uuid).orElse(null);

            if (user == null || userToFollow == null) {
                return ResponseEntity.notFound().build();
            }
            if(uuid.equals(user.getUuid())) {
                return ResponseEntity.badRequest().body("Cannot follow yourself");
            }
            if (user.getFollowedUsers().contains(userToFollow.getUuid())) {
                return ResponseEntity.badRequest().body("User already followed");
            }

            user.getFollowTimestamps().put(userToFollow.getUuid(), System.currentTimeMillis());
            userToFollow.getFollowerList().add(user.getUuid());
            userToFollow.setFollowers(userToFollow.getFollowers() + 1);
            user.getFollowedUsers().add(userToFollow.getUuid());

            userRepository.save(user);
            activityUpdater.increaseInteractions(user);
            userRepository.save(userToFollow);

            return ResponseEntity.ok("User followed");
        });
    }

    @DeleteMapping("/{uuid}/follow")
    @Async
    public CompletableFuture<ResponseEntity<String>> unfollowUser(@PathVariable UUID uuid, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            User userToUnfollow = userRepository.findById(uuid).orElse(null);

            if (user == null || userToUnfollow == null) {
                return ResponseEntity.notFound().build();
            }

            if (!user.getFollowedUsers().contains(userToUnfollow.getUuid())) {
                return ResponseEntity.badRequest().body("User not followed");
            }

            Long followTimestamp = user.getFollowTimestamps().get(userToUnfollow.getUuid());
            if (followTimestamp != null && System.currentTimeMillis() - followTimestamp < 3000) {
                return ResponseEntity.badRequest().body("Cannot unfollow so soon after following");
            }

            userToUnfollow.getFollowerList().remove(user.getUuid());
            userToUnfollow.setFollowers(userToUnfollow.getFollowers() - 1);
            user.getFollowedUsers().remove(userToUnfollow.getUuid());
            user.getFollowTimestamps().remove(userToUnfollow.getUuid());

            userRepository.save(user);
            activityUpdater.increaseInteractions(user);
            userRepository.save(userToUnfollow);

            return ResponseEntity.ok("User unfollowed");
        });
    }


    @GetMapping("/search")
    @Async
    public CompletableFuture<ResponseEntity<Map<String, Object>>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return CompletableFuture.supplyAsync(() -> {
            Page<User> userPage = userRepository.findUsersByUsernameContainingIgnoreCase(
                    query,
                    PageRequest.of(page, size)
            );

            List<ProfileDTO> profileDTOs = userPage.getContent().stream()
                    .map(user -> new ProfileDTO(
                            user.getUsername(),
                            "http://77.90.21.53:8010/api/users/profilepicture/" + user.getUsername(),
                            user.getPostedVideos(),
                            user.getDailyQuest(),
                            user.isBanned(),
                            user.getFollowers(),
                            false,
                            user.getBadges()
                    ))
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
            String profilePictureLink = "https://static.vecteezy.com/system/resources/thumbnails/003/337/584/small/default-avatar-photo-placeholder-profile-icon-vector.jpg";
            if (userWithVideos.getProfilePicture() != null) {
                profilePictureLink = "http://77.90.21.53:8010/api/users/profilepicture/" + username;
            }
            ProfileDTO profileDTO = new ProfileDTO(
                    userWithVideos.getUsername(),
                    profilePictureLink,
                    userWithVideos.getPostedVideos(),
                    userWithVideos.getDailyQuest(),
                    userWithVideos.isBanned(),
                    userWithVideos.getFollowers(),
                    user.getFollowedUsers().contains(userWithVideos.getUuid()),
                    userWithVideos.getBadges()
            );
            return ResponseEntity.ok(profileDTO);
        });
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
    //test
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

            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
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
            Optional<User> user = userRepository.findById(uuid);
            if (user.isEmpty()) {
                return ResponseEntity.ok("User not found");
            }
            user.get().setProfilePicture(fileBytes);
            userRepository.save(user.get());
            return ResponseEntity.ok("Profile picture uploaded successfully");

        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to process the file");
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
            //test1
        });
    }

    @GetMapping("{uuid}/isFollowed")
    @Async
    public CompletableFuture<ResponseEntity<Boolean>> isFollowed(@PathVariable UUID uuid, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(user.getFollowedUsers().contains(uuid));
        });
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
            if(user.getLastReroll() == null || user.getLastReroll().plusDays(1).isBefore(LocalDateTime.now())) {
                user.setLeftRerolls(3);
            }
            if(user.getLeftRerolls() == 0) {
                return ResponseEntity.badRequest().body("No rerolls left");
            }
            List<Quest> topQuests = questService.getTop10PercentQuests().join();
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
    public CompletableFuture<ResponseEntity<String>> addBadge(@PathVariable UUID uuid, @RequestBody UUID badgeId,  @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user.getAuthorities().stream()
                    .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is not an admin");
            }
            User userToAddBadge = userRepository.findById(uuid).orElse(null);
            if (userToAddBadge == null) {
                return ResponseEntity.notFound().build();
            }
            if (userToAddBadge.getBadges().contains(badgeId)) {
                return ResponseEntity.badRequest().body("Badge already added");
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
            User user = userRepository.findById(uuid).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(user.getBadges());
        });
    }

    @DeleteMapping("/{uuid}/badge")
    @Async
    public CompletableFuture<ResponseEntity<String>> removeBadge(@PathVariable UUID uuid, @RequestBody UUID badgeId,  @RequestHeader("Authorization") String token) {
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
                return ResponseEntity.badRequest().body("Badge not found");
            }
            userToRemoveBadge.getBadges().remove(badgeId);
            userRepository.save(userToRemoveBadge);
            return ResponseEntity.ok("Badge removed");
        });
    }
}