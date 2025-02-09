package com.dayquest.dayquestbackend.quest;

import com.dayquest.dayquestbackend.auth.service.JwtService;
import com.dayquest.dayquestbackend.activity.ActivityUpdater;
import com.dayquest.dayquestbackend.quest.dto.InteractionDTO;
import com.dayquest.dayquestbackend.quest.dto.QuestDTO;
import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quests")
public class QuestController {

    @Autowired
    private QuestService questService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private ActivityUpdater activityUpdater;

    @Autowired
    private JwtService jwtService;


    @GetMapping
    @Async
    public CompletableFuture<ResponseEntity<List<QuestDTO>>> getQuests(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection) {
        return CompletableFuture.supplyAsync(() -> {
            String username = jwtService.extractUsername(token.substring(7));
            User currentUser = userRepository.findByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            List<UUID> likedQuestIds = currentUser.getLikedQuests();
            List<UUID> dislikedQuestIds = currentUser.getDislikedQuests();

            Sort sort = Sort.by(sortDirection, sortBy);
            PageRequest pageRequest = PageRequest.of(page, size, sort);
            List<Quest> quests = questRepository.findAll(pageRequest).getContent();
            List<QuestDTO> questDTOS = quests.stream().map(quest -> {
                QuestDTO dto = new QuestDTO();
                dto.setUuid(quest.getUuid());
                dto.setCreatorUuid(quest.getCreatorUuid());
                dto.setTitle(quest.getTitle());
                dto.setDescription(quest.getDescription());
                dto.setLikes(quest.getLikes());
                dto.setDislikes(quest.getDislikes());
                dto.setCreatedAt(quest.getCreatedAt());
                dto.setLiked(likedQuestIds.contains(quest.getUuid()));
                dto.setDisliked(dislikedQuestIds.contains(quest.getUuid()));
                return dto;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(questDTOS);
        });
    }


    @PostMapping("/create")
    @Async
    public CompletableFuture<ResponseEntity<Quest>> createQuest(
            @RequestBody Quest quest,
            @RequestHeader("Authorization") String token) {

        if (quest.getDescription().toLowerCase().contains("penis")) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
        }
        String username = jwtService.extractUsername(token.substring(7));
        User creator = userRepository.findByUsername(username);
        if (creator == null) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null));
        }
        return questService.createQuest(quest.getTitle(), quest.getDescription(), creator)
                .thenApply(newQuest -> ResponseEntity.status(HttpStatus.CREATED).body(newQuest));
    }


    @PostMapping("/like")
    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<?>> likeQuest(@RequestBody InteractionDTO interactionDTO) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> userOpt = userRepository.findById(interactionDTO.getUserUuid());
            Optional<Quest> questOpt = questRepository.findById(interactionDTO.getUuid());
            if (userOpt.isEmpty() || questOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            User user = userOpt.get();
            Quest quest = questOpt.get();

            if (user.getLikedQuests().contains(quest.getUuid())) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Already liked");
            }
            if (user.getDislikedQuests().remove(quest.getUuid())) {
                questRepository.decrementDislikes(quest.getUuid());
            }
            user.getLikedQuests().add(quest.getUuid());
            questRepository.incrementLikes(quest.getUuid());

            questRepository.save(quest);
            activityUpdater.increaseInteractions(user);
            userRepository.save(user);

            return ResponseEntity.ok("Successfully liked quest");
        });
    }

    @DeleteMapping("/like")
    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<?>> unlikeQuest(@RequestBody InteractionDTO interactionDTO) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> userOpt = userRepository.findById(interactionDTO.getUserUuid());
            Optional<Quest> questOpt = questRepository.findById(interactionDTO.getUuid());
            if (userOpt.isEmpty() || questOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            User user = userOpt.get();
            Quest quest = questOpt.get();

            if (!user.getLikedQuests().contains(quest.getUuid())) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Not liked");
            }
            user.getLikedQuests().remove(quest.getUuid());
            questRepository.decrementLikes(quest.getUuid());
            questRepository.save(quest);
            userRepository.save(user);
            return ResponseEntity.ok("Successfully unliked quest");
        });
    }

    @PostMapping("/dislike")
    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<?>> dislikeQuest(@RequestBody InteractionDTO interactionDTO) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> userOpt = userRepository.findById(interactionDTO.getUserUuid());
            Optional<Quest> questOpt = questRepository.findById(interactionDTO.getUuid());
            if (userOpt.isEmpty() || questOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            User user = userOpt.get();
            Quest quest = questOpt.get();

            if (user.getDislikedQuests().contains(quest.getUuid())) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Already disliked");
            }
            if (user.getLikedQuests().remove(quest.getUuid())) {
                questRepository.decrementLikes(quest.getUuid());
            }
            user.getDislikedQuests().add(quest.getUuid());
            questRepository.incrementDislikes(quest.getUuid());
            questRepository.save(quest);
            activityUpdater.increaseInteractions(user);
            userRepository.save(user);
            return ResponseEntity.ok("Successfully disliked quest");
        });
    }

    @DeleteMapping("/dislike")
    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<?>> undislikeQuest(@RequestBody InteractionDTO interactionDTO) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> userOpt = userRepository.findById(interactionDTO.getUserUuid());
            Optional<Quest> questOpt = questRepository.findById(interactionDTO.getUuid());
            if (userOpt.isEmpty() || questOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            User user = userOpt.get();
            Quest quest = questOpt.get();

            if (!user.getDislikedQuests().contains(quest.getUuid())) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Not disliked");
            }
            user.getDislikedQuests().remove(quest.getUuid());
            questRepository.decrementDislikes(quest.getUuid());
            questRepository.save(quest);
            userRepository.save(user);
            return ResponseEntity.ok("Successfully undisliked quest");
        });
    }

    @PostMapping("/get-quest")
    @Async
    public CompletableFuture<ResponseEntity<String>> getQuest(@RequestBody UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> userOpt = userRepository.findById(uuid);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Quest dailyQuest = userOpt.get().getDailyQuest();
            if (dailyQuest == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(dailyQuest.getDescription());
        });
    }

    @GetMapping("/{userid}")
    @Async
    public CompletableFuture<ResponseEntity<QuestDTO>> getUsersQuest(@PathVariable UUID userid) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> userOpt = userRepository.findById(userid);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Quest quest = userOpt.get().getDailyQuest();
            if (quest == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(new QuestDTO(quest));
        });
    }
}
