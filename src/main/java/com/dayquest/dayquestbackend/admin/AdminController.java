package com.dayquest.dayquestbackend.admin;
import com.dayquest.dayquestbackend.JwtService;
import com.dayquest.dayquestbackend.quest.Quest;
import com.dayquest.dayquestbackend.quest.QuestRepository;
import com.dayquest.dayquestbackend.quest.QuestService;
import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.user.UserRepository;
import com.dayquest.dayquestbackend.video.Video;
import com.dayquest.dayquestbackend.video.VideoDTO;
import com.dayquest.dayquestbackend.video.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.hibernate.internal.util.collections.CollectionHelper.listOf;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private QuestService questService;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private VideoRepository videoRepository;

    @PostMapping("/auth")
    @Async
    public CompletableFuture<ResponseEntity<String>> test(@RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user.getAuthorities().stream()
                    .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                return ResponseEntity.ok("Authenticated");
            }


            return ResponseEntity.status(403).body("You are not an admin");
        });
    }

    @GetMapping("/videos")
    @Async
    public CompletableFuture<ResponseEntity<List<VideoDTO>>> getVideos(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);

            if (user.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {

                Sort sort = Sort.by(sortDirection, sortBy);

                PageRequest pageRequest = PageRequest.of(page, size, sort);

                Page<Video> videoPage = videoRepository.findAll(pageRequest);

                List<VideoDTO> videoDTOS = videoPage.getContent().stream()
                        .map(video -> new VideoDTO(
                                video.getTitle(),
                                video.getDescription(),
                                video.getUpVotes(),
                                video.getDownVotes(),
                                video.getUser().getUsername(),
                                video.getFilePath(),
                                "http://77.90.21.53:8010/api/videos/thumbnail/" + video.getUuid().toString(),
                                questRepository.findByUuid(video.getQuestUuid()),
                                video.getUuid(),
                                video.getCreatedAt()))
                        .collect(Collectors.toList());

                return ResponseEntity.ok(videoDTOS);
            }

            return ResponseEntity.ok(null);
        });
    }



    @PostMapping("/deleteVideo")
    @Async
    public CompletableFuture<ResponseEntity<String>> deleteVideo(@RequestHeader("Authorization") String token, @RequestBody String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                Video video = videoRepository.findById(UUID.fromString(uuid)).orElse(null);
                videoRepository.delete(video);
                return ResponseEntity.ok("Video deleted");
            }
            return ResponseEntity.badRequest().body("You are not an admin");
        });
    }

    @PostMapping("/deleteQuest")
    @Async
    public CompletableFuture<ResponseEntity<String>> deleteQuest(@RequestHeader("Authorization") String token, @RequestBody String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                List<User> usersWithQuest = userRepository.findAllByDailyQuestUuid(UUID.fromString(uuid));
                List<Quest> topQuests = questService.getTop10PercentQuests().join();
                Random random = new Random();
                int i = 0;
                if (topQuests == null || topQuests.isEmpty()) {
                    throw new IllegalArgumentException("The list of top quests must not be null or empty");
                }

                for (User userWithQuest : usersWithQuest) {
                    Quest lastQuest = userWithQuest.getDailyQuest();
                    Quest randomQuest = topQuests.get(random.nextInt(topQuests.size()));
                    while (randomQuest.equals(lastQuest) || userWithQuest.getDoneQuests().contains(randomQuest.getUuid()) && topQuests.size() > 1) {
                        randomQuest = topQuests.get(random.nextInt(topQuests.size()));
                        i++;
                        if(i>100) {
                            break;
                        }
                    }
                    userWithQuest.addDoneQuest(lastQuest.getUuid());
                    userWithQuest.setDailyQuest(randomQuest);
                    userRepository.save(userWithQuest);
                }
                questRepository.deleteById(UUID.fromString(uuid));
                return ResponseEntity.ok("Quest deleted");
            }
            return ResponseEntity.badRequest().body("You are not an admin");
        });
    }

    @PostMapping("/banUser")
    @Async
    public CompletableFuture<ResponseEntity<String>> banUser(@RequestHeader("Authorization") String token, @RequestBody String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                User userToBan = userRepository.findById(UUID.fromString(uuid)).orElse(null);
                userToBan.setBanned(true);
                userRepository.save(userToBan);
                return ResponseEntity.ok("User banned");
            }
            return ResponseEntity.badRequest().body("You are not an admin");
        });
    }

    @PostMapping("/unbanUser")
    @Async
    public CompletableFuture<ResponseEntity<String>> unbanUser(@RequestHeader("Authorization") String token, @RequestBody String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                User userToUnban = userRepository.findById(UUID.fromString(uuid)).orElse(null);
                userToUnban.setBanned(false);
                userRepository.save(userToUnban);
                return ResponseEntity.ok("User unbanned");
            }
            return ResponseEntity.badRequest().body("You are not an admin");
        });
    }

    @PostMapping("/setAdmin")
    @Async
    public CompletableFuture<ResponseEntity<String>> setAdmin(@RequestBody String username) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
            user.setAuthorities(listOf("ROLE_USER", "ROLE_ADMIN"));
            userRepository.save(user);
            return ResponseEntity.ok("User is now an admin");
        });
    }

    @GetMapping("/users/{uuid}")
    @Async
    public CompletableFuture<ResponseEntity<UserDetailsDTO>> getUserDetails(@RequestHeader("Authorization") String token, @PathVariable String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                User userToGet = userRepository.findById(UUID.fromString(uuid)).orElse(null);
                UserDetailsDTO userDetailsDTO = new UserDetailsDTO();
                userDetailsDTO.setUsername(userToGet.getUsername());
                userDetailsDTO.setEmail(userToGet.getEmail());
                userDetailsDTO.setAdminComment(userToGet.getAdminComment());
                userDetailsDTO.setEnabled(userToGet.isEnabled());
                userDetailsDTO.setBanned(userToGet.isBanned());
                userDetailsDTO.setVerificationCode(userToGet.getVerificationCode());
                return ResponseEntity.ok(userDetailsDTO);
            }
            return ResponseEntity.badRequest().body(null);
        });
    }

    @PutMapping("/users/{uuid}")
    @Async
    public CompletableFuture<ResponseEntity<String>> updateUserDetails(@RequestHeader("Authorization") String token, @PathVariable String uuid, @RequestBody UserDetailsDTO userDetailsDTO) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")) || user.getUsername().equals(username)) {
                User userToUpdate = userRepository.findById(UUID.fromString(uuid)).orElse(null);
                userToUpdate.setUsername(userDetailsDTO.getUsername());
                userToUpdate.setEmail(userDetailsDTO.getEmail());
                userToUpdate.setAdminComment(userDetailsDTO.getAdminComment());
                userToUpdate.setEnabled(userDetailsDTO.isEnabled());
                userToUpdate.setBanned(userDetailsDTO.isBanned());
                userToUpdate.setVerificationCode(userDetailsDTO.getVerificationCode());
                userRepository.save(userToUpdate);
                return ResponseEntity.ok("User details updated");
            }
            return ResponseEntity.badRequest().body("You are not an admin");
        });
    }

    @DeleteMapping("/users/{uuid}")
    @Async
    public CompletableFuture<ResponseEntity<String>> deleteUser(@RequestHeader("Authorization") String token, @PathVariable String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User user = userRepository.findByUsername(username);
            if (user.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
                User userToDelete = userRepository.findById(UUID.fromString(uuid)).orElse(null);
                if (userToDelete == null) {
                    return ResponseEntity.badRequest().body("User not found");
                }
                userRepository.delete(userToDelete);
                return ResponseEntity.ok("User deleted");
            }
            return ResponseEntity.badRequest().body("You are not an admin");
        });
    }
}
