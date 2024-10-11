package com.dayquest.dayquestbackend.quest;

import com.dayquest.dayquestbackend.user.UserRepository;
import com.dayquest.dayquestbackend.user.UserService;
import com.dayquest.dayquestbackend.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

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
    public CompletableFuture<ResponseEntity<List<Quest>>> getAllQuests() {
        return questService.getAllQuests()
            .thenApply(quests -> {
                Collections.shuffle(quests);
                return ResponseEntity.ok(quests);
            });
    }

    @PostMapping("/suggest")
    public CompletableFuture<ResponseEntity<Quest>> suggestQuest(@RequestBody Quest quest) {
        if (quest.getDescription().toLowerCase().contains("penis")) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
        }
        return questService.suggestQuest(quest.getTitle(), quest.getDescription())
            .thenApply(newQuest -> ResponseEntity.status(HttpStatus.CREATED).body(newQuest));
    }

    @PostMapping("/{id}/like")
    public CompletableFuture<ResponseEntity<Void>> likeQuest(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String uuid = body.get("uuid");
        if (uuid == null) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().build());
        }
        try {
            UUID userUuid = UUID.fromString(uuid);
            User user = userRepository.findByUuid(userUuid);
            if (user == null) {
                return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(null));
            }
            if (user.getLikedQuests().contains(id)) {
                return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(null));
            } else {
                user.getLikedQuests().add(id);
                return questService.likeQuest(id)
                    .thenApply(quest -> ResponseEntity.ok().build());
            }
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(null));
        }
    }

    @PostMapping("/{id}/dislike")
    public CompletableFuture<ResponseEntity<Void>> dislikeQuest(@PathVariable Long id, @RequestBody UUID uuid) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().build());
        }
        try {
            User user = userRepository.findByUuid(uuid);
            if (user == null) {
                return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(null));
            }
            if (user.getDislikedQuests().contains(id)) {
                return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(null));
            } else {
                user.getDislikedQuests().add(id);
                return questService.dislikeQuest(id)
                    .thenApply(quest -> ResponseEntity.ok().build());
            }
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(null));
        }
    }

    @PostMapping("/getquest")
    public ResponseEntity<String> getQuest(@RequestBody UUID uuid) {
        User user = userRepository.findByUuid(uuid);
        Quest quest = user.getDailyQuest();
        return ResponseEntity.ok(quest.getDescription());
    }
}