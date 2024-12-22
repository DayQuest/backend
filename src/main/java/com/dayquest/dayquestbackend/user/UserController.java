package com.dayquest.dayquestbackend.user;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import com.dayquest.dayquestbackend.JwtService;
import com.dayquest.dayquestbackend.quest.Quest;
import com.dayquest.dayquestbackend.quest.QuestService;
import com.dayquest.dayquestbackend.streak.StreakService;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
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

import java.io.InputStream;

import javax.imageio.ImageIO;
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


    @PostMapping("/register")
    @Async
    public CompletableFuture<ResponseEntity<String>> registerUser(@RequestBody UserDTO userDTO) {
        return userService.registerUser(userDTO.getUsername(), userDTO.getEmail(),
                userDTO.getPassword(), userDTO.getBetaKey());
    }

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

    @PostMapping("/login")
    @Async
    public CompletableFuture<ResponseEntity<LoginResponse>> loginUser(@Valid @RequestBody LoginDTO loginDTO) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findByUsername(loginDTO.getUsername());

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new LoginResponse(null, null, "User not found"));
            }

            if (user.isBanned()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new LoginResponse(null, null, "User has been banned"));
            }

            if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new LoginResponse(null, null, "Invalid password"));
            }

            if (!user.isEnabled()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new LoginResponse(null, null, "User not verified"));
            }


            activityUpdater.increaseInteractions(user);
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            String token = jwtService.generateToken(user);

            return ResponseEntity.ok(new LoginResponse(user.getUuid(), token, "Login successful"));
        });
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
                    user.getFollowedUsers().contains(uuid)
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
                            false
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
                    user.getFollowedUsers().contains(userWithVideos.getUuid())
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
    public ResponseEntity<String> setProfilePicture(@RequestParam("file") MultipartFile file, @RequestParam("uuid") UUID uuid) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            byte[] fileBytes = compressImage(file);
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
    public CompletableFuture<ResponseEntity<String>> rerollQuest(@RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            List<Quest> topQuests = questService.getTop10PercentQuests().join();
            Quest newQuest;
            do {
                newQuest = topQuests.get(new Random().nextInt(topQuests.size()));
            } while (user.getDailyQuest().equals(newQuest));
            user.setDailyQuest(newQuest);
            userRepository.save(user);
            return ResponseEntity.ok("Quest rerolled");
        });
    }

    public byte[] compressImage(MultipartFile originalFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(originalFile.getInputStream());

        originalImage = fixImageOrientation(originalImage, originalFile);

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double scale = Math.min(360.0 / originalWidth, 360.0 / originalHeight);

        int newWidth = (int) Math.round(originalWidth * scale);
        int newHeight = (int) Math.round(originalHeight * scale);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        // Write resized image to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpg", baos);

        return baos.toByteArray();
    }

    // Fix orientation based on Exif metadata
    private BufferedImage fixImageOrientation(BufferedImage image, MultipartFile originalFile) throws IOException {
        try (InputStream inputStream = originalFile.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
            Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                int orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);

                switch (orientation) {
                    case 6:
                        return rotateImage(image, 90);
                    case 3:
                        return rotateImage(image, 180);
                    case 8:
                        return rotateImage(image, -90);
                    default:
                        return image;
                }
            }
        } catch (Exception e) {
            System.err.println("Could not read EXIF metadata: " + e.getMessage());
        }

        return image;
    }

    private BufferedImage rotateImage(BufferedImage image, int angle) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage rotatedImage = new BufferedImage(height, width, image.getType());
        Graphics2D g2d = rotatedImage.createGraphics();

        g2d.rotate(Math.toRadians(angle), height / 2.0, height / 2.0);
        g2d.translate((height - width) / 2.0, (width - height) / 2.0);
        g2d.drawRenderedImage(image, null);
        g2d.dispose();

        return rotatedImage;
    }
}