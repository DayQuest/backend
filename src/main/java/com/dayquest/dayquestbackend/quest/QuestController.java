package com.dayquest.dayquestbackend.quest;

import com.dayquest.dayquestbackend.JwtService;
import com.dayquest.dayquestbackend.user.ActivityUpdater;
import com.dayquest.dayquestbackend.user.UserRepository;
import com.dayquest.dayquestbackend.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<ResponseEntity<List<QuestDTO>>> getQuests(@RequestHeader("Authorization") String token,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "10") int size,
                                                                       @RequestParam(defaultValue = "createdAt") String sortBy,
                                                                       @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection) {
        return CompletableFuture.supplyAsync(() -> {
            Sort sort = Sort.by(sortDirection, sortBy);
            PageRequest pageRequest = PageRequest.of(page, size, sort);
            List<Quest> quests = questRepository.findAll(pageRequest).getContent();
            List<QuestDTO> questDTOS = quests.stream().map(quest -> {
                QuestDTO questDTO = new QuestDTO();
                questDTO.setUuid(quest.getUuid());
                questDTO.setCreatorUuid(quest.getCreatorUuid());
                questDTO.setTitle(quest.getTitle());
                questDTO.setDescription(quest.getDescription());
                questDTO.setLikes(quest.getLikes());
                questDTO.setDislikes(quest.getDislikes());
                questDTO.setCreatedAt(quest.getCreatedAt());
                questDTO.setLiked(userRepository.findByUsername(jwtService.extractUsername(token.substring(7))).getLikedQuests().contains(quest.getUuid()));
                questDTO.setDisliked(userRepository.findByUsername(jwtService.extractUsername(token.substring(7))).getDislikedQuests().contains(quest.getUuid()));
                return questDTO;
            }).toList();
            return ResponseEntity.ok(questDTOS);
        });
    }

    @PostMapping("/create")
    @Async
    public CompletableFuture<ResponseEntity<Quest>> createQuest(@RequestBody Quest quest, @RequestHeader("Authorization") String token) {
        if (quest.getDescription().toLowerCase().contains("penis")) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
        }

        return questService.createQuest(quest.getTitle(), quest.getDescription(), userRepository.findByUsername(jwtService.extractUsername(token.substring(7))))
                .thenApply(newQuest -> ResponseEntity.status(HttpStatus.CREATED).body(newQuest));
    }

    @PostMapping("/like")
    @Async
    public CompletableFuture<ResponseEntity<?>> likeQuest(@RequestBody InteractionDTO interactionDTO) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(interactionDTO.getUserUuid());
            Optional<Quest> quest = questRepository.findById(interactionDTO.getUuid());
            if (user.isEmpty() || quest.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (user.get().getLikedQuests().contains(interactionDTO.getUuid())) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Already liked");
            }

            if (user.get().getDislikedQuests().contains(interactionDTO.getUuid())) {
                user.get().getDislikedQuests().remove(interactionDTO.getUuid());
                quest.get().setDislikes(quest.get().getDislikes() - 1);
            }

            user.get().getLikedQuests().add(interactionDTO.getUuid());
            quest.get().setLikes(quest.get().getLikes() + 1);
            questRepository.save(quest.get());
            activityUpdater.increaseInteractions(user);
            userRepository.save(user.get());
            return ResponseEntity.ok("Successfully liked quest");
        });
    }

    @DeleteMapping("/like")
    @Async
    public CompletableFuture<ResponseEntity<?>> unlikeQuest(@RequestBody InteractionDTO interactionDTO) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(interactionDTO.getUserUuid());
            Optional<Quest> quest = questRepository.findById(interactionDTO.getUuid());
            if (user.isEmpty() || quest.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (!user.get().getLikedQuests().contains(interactionDTO.getUuid())) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Not liked");
            }

            user.get().getLikedQuests().remove(interactionDTO.getUuid());
            quest.get().setLikes(quest.get().getLikes() - 1);
            questRepository.save(quest.get());
            userRepository.save(user.get());
            return ResponseEntity.ok("Successfully unliked quest");
        });
    }


    //TODO: Refactor this code dupe
    @PostMapping("/dislike")
    @Async
    public CompletableFuture<ResponseEntity<?>> dislikeQuest(@RequestBody InteractionDTO interactionDTO) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(interactionDTO.getUserUuid());
            Optional<Quest> quest = questRepository.findById(interactionDTO.getUuid());
            if (user.isEmpty() || quest.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (user.get().getDislikedQuests().contains(interactionDTO.getUuid())) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Already disliked");
            }

            if (user.get().getLikedQuests().contains(interactionDTO.getUuid())) {
                user.get().getLikedQuests().remove(interactionDTO.getUuid());
                quest.get().setLikes(quest.get().getLikes() - 1);
            }

            user.get().getDislikedQuests().add(interactionDTO.getUuid());
            quest.get().setDislikes(quest.get().getDislikes() + 1);
            questRepository.save(quest.get());
            activityUpdater.increaseInteractions(user);
            userRepository.save(user.get());
            return ResponseEntity.ok("Successfully disliked quest");
        });
    }

    @DeleteMapping("/dislike")
    @Async
    public CompletableFuture<ResponseEntity<?>> undislikeQuest(@RequestBody InteractionDTO interactionDTO) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(interactionDTO.getUserUuid());
            Optional<Quest> quest = questRepository.findById(interactionDTO.getUuid());
            if (user.isEmpty() || quest.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (!user.get().getDislikedQuests().contains(interactionDTO.getUuid())) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Not disliked");
            }

            user.get().getDislikedQuests().remove(interactionDTO.getUuid());
            quest.get().setDislikes(quest.get().getDislikes() - 1);
            questRepository.save(quest.get());
            userRepository.save(user.get());
            return ResponseEntity.ok("Successfully undisliked quest");
        });
    }

    //TODO: Remove after next Beta
    @PostMapping("/get-quest")
    @Async
    public CompletableFuture<ResponseEntity<String>> getQuest(@RequestBody UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(uuid);
            if (user.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Quest quest = questRepository.findById(user.get().getDailyQuest().getUuid()).orElse(null);
            if (quest == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(quest.getDescription());
        });
    }

    @GetMapping("/{userid}")
    @Async
    public CompletableFuture<ResponseEntity<QuestDTO>> getUsersQuest(@PathVariable UUID userid){
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(userid);
            if (user.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Quest quest = user.get().getDailyQuest();
            if (quest == null) {
                return ResponseEntity.notFound().build();
            }
            QuestDTO questDTO = new QuestDTO(quest);
            return ResponseEntity.ok(questDTO);
        });
    }
}