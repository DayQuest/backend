package com.dayquest.dayquestbackend.quest;

import com.dayquest.dayquestbackend.user.UserRepository;
import com.dayquest.dayquestbackend.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/quests")
public class QuestController {

    // The static initializer block with prohibitedPatterns remains unchanged

    @Autowired
    private QuestService questService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestRepository questRepository;

    @GetMapping
    @Async
    public CompletableFuture<ResponseEntity<List<Quest>>> getAllQuests() {
        return CompletableFuture.supplyAsync(() -> {
           List<Quest> quests = questRepository.findAll();
           Collections.shuffle(quests);
           return ResponseEntity.ok(quests);
        });
    }

    @PostMapping("/create")
    @Async
    public CompletableFuture<ResponseEntity<Quest>> createQuest(@RequestBody Quest quest) {
        if (quest.getDescription().toLowerCase().contains("penis")) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
        }
        return questService.createQuest(quest.getTitle(), quest.getDescription())
            .thenApply(newQuest -> ResponseEntity.status(HttpStatus.CREATED).body(newQuest));
    }

    @PostMapping("/like")
    @Async
    public CompletableFuture<ResponseEntity<?>> likeQuest(@RequestBody UUID uuid, @RequestBody UUID userUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(userUuid);
            Optional<Quest> quest = questRepository.findById(uuid);
            if (user.isEmpty() || quest.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (user.get().getLikedQuests().contains(uuid)) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Already liked");
            }

            user.get().getLikedQuests().add(uuid);
            userRepository.save(user.get());
            return ResponseEntity.ok("Successfully liked quest");
        });
    }


    //TODO: Refactor this code dupe
    @PostMapping("/dislike")
    @Async
    public CompletableFuture<ResponseEntity<?>> dislikeQuest(@RequestBody UUID uuid, @RequestBody UUID userUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(userUuid);
            Optional<Quest> quest = questRepository.findById(uuid);
            if (user.isEmpty() || quest.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (user.get().getDislikedQuests().contains(uuid)) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Already disliked");
            }

            user.get().getDislikedVideos().add(uuid);
            userRepository.save(user.get());
            return ResponseEntity.ok("Successfully disliked quest");
        });
    }

    @PostMapping("/get-quest")
    @Async
    public CompletableFuture<ResponseEntity<Quest>> getQuest(@RequestBody UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
          if (questRepository.findById(uuid).isEmpty()) {
              return ResponseEntity.notFound().build();
          }

          return ResponseEntity.ok(questRepository.findById(uuid).get());
        });
    }
}