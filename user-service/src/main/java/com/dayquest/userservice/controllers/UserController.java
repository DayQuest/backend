package com.dayquest.userservice.controllers;

import com.dayquest.userservice.dto.*;
import com.dayquest.userservice.models.User;
import com.dayquest.userservice.repositories.BadgeRepository;
import com.dayquest.userservice.repositories.UserRepository;
import com.dayquest.userservice.services.FollowService;
import com.dayquest.userservice.services.JwtService;
import com.dayquest.userservice.services.UserService;
import com.dayquest.userservice.utils.ImageUtil;
import jakarta.validation.Valid;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserService userService;
    private final ImageUtil imageUtil;
    private final BadgeRepository badgeRepository;
    private final FollowService followService;

    public UserController(JwtService jwtService, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, UserService userService, ImageUtil imageUtil, BadgeRepository badgeRepository, FollowService followService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.imageUtil = imageUtil;
        this.badgeRepository = badgeRepository;
        this.followService = followService;
    }

    @DeleteMapping
    @Async
    public CompletableFuture<ResponseEntity<?>> deleteUser(@RequestHeader("Authorization") String token, @RequestBody PasswordDTO passwordDTO) {
        String tokenWithoutBearer = token.substring(7);
        UUID userId = jwtService.extractUserId(tokenWithoutBearer);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !passwordEncoder.matches(passwordDTO.getPassword(), user.getPassword())) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials"));
        }
        userRepository.delete(user);
        return CompletableFuture.completedFuture(ResponseEntity.ok("User deleted successfully"));
    }

    @GetMapping("/{uuid}")
    @Async
    public CompletableFuture<ResponseEntity<ProfileDTO>> getUserByUuid(@PathVariable UUID uuid, @RequestHeader("Authorization") String token) {
        Optional<User> userWithVideos = userRepository.findById(uuid);
        if (userWithVideos.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        }
        UUID requesterUUID = jwtService.extractUserId(token.substring(7));
        User requester = userRepository.findById(requesterUUID).orElse(null);
        ProfileDTO profileDTO = userService.createProfileDTO(userWithVideos.get(), requester).join();
        return CompletableFuture.completedFuture(ResponseEntity.ok(profileDTO));
    }

    @GetMapping("/profile/{username}")
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<ResponseEntity<ProfileDTO>> getUserByUsername(
            @PathVariable String username, @RequestHeader("Authorization") String token) {
            User userWithVideos = userRepository.findByUsername(username);
            Optional<User> user = userRepository.findById(jwtService.extractUserId(token.substring(7)));
            if (userWithVideos == null) {
                return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
            }
            return CompletableFuture.completedFuture(ResponseEntity.ok(userService.createProfileDTO(userWithVideos, user.get()).join()));
    }

    @GetMapping("/search")
    @Async
    public CompletableFuture<ResponseEntity<Map<String, Object>>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
            Page<User> userPage = userRepository.findUsersByUsernameContainingIgnoreCase(query, PageRequest.of(page, size));
            List<ProfileDTO> profileDTOs = userPage.getContent().stream()
                    .map(user -> userService.createProfileDTO(user, null).join())
                    .collect(Collectors.toList());

            if (profileDTOs.isEmpty()) {
                return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("users", profileDTOs);
            response.put("currentPage", userPage.getNumber());
            response.put("totalItems", userPage.getTotalElements());
            response.put("totalPages", userPage.getTotalPages());

            return CompletableFuture.completedFuture(ResponseEntity.ok(response));
    }

    @GetMapping("/{username}/uuid")
    @Async
    public CompletableFuture<ResponseEntity<UUID>> getUuidByUsername(@PathVariable String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
        }
        return CompletableFuture.completedFuture(ResponseEntity.ok(user.getUuid()));
    }

    @GetMapping("/profilepicture/{username}")
    @Async
    public CompletableFuture<ResponseEntity<ByteArrayResource>> getDecodedImage(@PathVariable("username") String username) {
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

            return CompletableFuture.completedFuture(ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(imageBytes.length)
                    .body(resource));

        } catch (IOException | IllegalArgumentException e) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new ByteArrayResource(new byte[0])));
        }
    }
    @PutMapping("/email")
    @Async
    public CompletableFuture<ResponseEntity<?>> updateEmail (@RequestBody @Valid UpdateEmailDTO updateEmailDTO, @RequestHeader("Authorization") String token){
        Optional<User> userOptional = userRepository.findById(jwtService.extractUserId(token.substring(7)));
        if (userOptional.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
        }
        User user = userOptional.get();
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
        Optional<User> userOptional = userRepository.findById(jwtService.extractUserId(token.substring(7)));
        if (userOptional.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
        }
        User user = userOptional.get();
        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), user.getPassword())) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid old password"));
        }
        user.setPassword(passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
        userRepository.save(user);
        return CompletableFuture.completedFuture(ResponseEntity.ok("Password updated successfully"));
    }

    @PostMapping("/setprofilepicture")
    @Async
    public CompletableFuture<ResponseEntity<String>> setProfilePicture(@RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String token) {
        if (file.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("File is empty"));
        }
        /*if (!userService.authenticateUserWith2FA(uuid, token, null).join()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }*/

        try {
            Optional<User> userOptional = userRepository.findById(jwtService.extractUserId(token.substring(7)));
            if(userOptional.isEmpty()){
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
            }
            User user = userOptional.get();
            byte[] fileBytes = imageUtil.compressImage(file).join();
            user.setProfilePicture(fileBytes);
            userRepository.save(user);
            return CompletableFuture.completedFuture(ResponseEntity.ok("Profile picture uploaded successfully"));
        } catch (IOException e) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process the file"));
        }
    }

    @GetMapping("/rerolls")
    @Async
    public CompletableFuture<ResponseEntity<Integer>> getRerolls(@RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            UUID userId = jwtService.extractUserId(token.substring(7));
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(0);
            }
            User user = userOptional.get();
            return user.getLastReroll() == null || user.getLastReroll().plusDays(1).isBefore(LocalDateTime.now()) ? ResponseEntity.ok(3) : ResponseEntity.ok(user.getLeftRerolls());
        });
    }

    @PutMapping("/{uuid}/badge")
    @Async
    public CompletableFuture<ResponseEntity<String>> addBadge(@PathVariable UUID uuid, @RequestBody UUID badgeId, @RequestHeader("Authorization") String token) {
            UUID userId = jwtService.extractUserId(token.substring(7));
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
            }
            User user = userOptional.get();
            if (user.getAuthorities().stream().noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is not an admin"));
            }
            User userToAddBadge = userRepository.findById(uuid).orElseThrow(() -> new RuntimeException("User not found"));
            if (userToAddBadge.getBadges().contains(badgeId)) {
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.CONFLICT).body("Badge already added"));
            }
            userToAddBadge.getBadges().add(badgeId);
            userRepository.save(userToAddBadge);
            return CompletableFuture.completedFuture(ResponseEntity.ok("Badge added"));
    }

    @GetMapping("/{uuid}/badges")
    @Async
    public CompletableFuture<ResponseEntity<List<UUID>>> getBadges(@PathVariable UUID uuid) {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(badgeRepository.findBadgeIdsByUserId(uuid)));
    }

    @GetMapping("/{uuid}/followers")
    @Async
    public CompletableFuture<ResponseEntity<List<UUID>>> getFollowers(@PathVariable UUID uuid, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestHeader("Authorization") String token) {
            CompletableFuture<List<UUID>> followersFuture = followService.getFollowerPage(token, page, size);
            List<UUID> followers = followersFuture.join();
            return CompletableFuture.completedFuture(ResponseEntity.ok(followers));
    }

    @GetMapping("/{uuid}/following")
    @Async
    public CompletableFuture<ResponseEntity<List<UUID>>> getFollowing(@PathVariable UUID uuid, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, @RequestHeader("Authorization") String token) {
            CompletableFuture<List<UUID>> followingFuture = followService.getFollowedPage(token, page, size);
            List<UUID> following = followingFuture.join();
            return CompletableFuture.completedFuture(ResponseEntity.ok(following));
    }

    @PostMapping("/{uuid}/follow")
    @Async
    public CompletableFuture<ResponseEntity<String>> followUser(
            @PathVariable UUID uuid,
            @RequestHeader("Authorization") String token,
            @RequestBody(required = false) UuidDTO videoUuid) {
            UUID userId = jwtService.extractUserId(token.substring(7));
            Optional<User> userOptional = userRepository.findById(userId);
            User userToFollow = userRepository.findById(uuid).orElseThrow(() -> new RuntimeException("User not found"));
            if (userOptional.isEmpty()) {
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
            }
            User user = userOptional.get();
            if (uuid.equals(user.getUuid())) {
                return CompletableFuture.completedFuture(ResponseEntity.unprocessableEntity().body("Cannot follow yourself"));
            }
            if (followService.isFollowing(user.getUuid(), uuid).join()) {
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.CONFLICT).body("User already followed"));
            }

            userToFollow.setFollowers(userToFollow.getFollowers() + 1);
            followService.followUser(token, uuid).join();
            userRepository.save(userToFollow);

            return CompletableFuture.completedFuture(ResponseEntity.ok("User followed"));
    }

    @DeleteMapping("/{uuid}/follow")
    @Async
    public CompletableFuture<ResponseEntity<String>> unfollowUser(
            @PathVariable UUID uuid,
            @RequestHeader("Authorization") String token) {
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

    @GetMapping("{username}/followersAsInt")
    @Async
    public CompletableFuture<ResponseEntity<Integer>> getFollowersAsInt(@PathVariable String username) {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
            }
            return CompletableFuture.completedFuture(ResponseEntity.ok(user.getFollowers()));
    }

    @GetMapping("{uuid}/isFollowed")
    @Async
    public CompletableFuture<ResponseEntity<Boolean>> isFollowed(@PathVariable UUID uuid, @RequestHeader("Authorization") String token) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(false));
        }
        UUID username = jwtService.extractUserId(token.substring(7));
        Optional<User> user = userRepository.findById(username);
        return user.map(value -> followService.isFollowing(value.getUuid(), uuid).join()
                ? CompletableFuture.completedFuture(ResponseEntity.ok(true))
                : CompletableFuture.completedFuture(ResponseEntity.ok(false))).orElseGet(() -> CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NOT_FOUND).body(false)));
    }

    @DeleteMapping("/{uuid}/badge")
    @Async
    public CompletableFuture<ResponseEntity<String>> removeBadge(@PathVariable UUID uuid, @RequestBody UUID badgeId, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            UUID userId = jwtService.extractUserId(token.substring(7));
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            User user = userOptional.get();
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
