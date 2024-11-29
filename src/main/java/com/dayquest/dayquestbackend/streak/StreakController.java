package com.dayquest.dayquestbackend.streak;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/streaks")
public class StreakController {
    @Autowired
    private StreakService streakService;

    @PostMapping("/create")
    @Async
    public CompletableFuture<ResponseEntity<String>> createStreak(@RequestBody UUID userId) {
        return streakService.createStreak(userId);
    }

    @PostMapping("/get")
    @Async
    public CompletableFuture<ResponseEntity<Integer>> getStreak(@RequestBody UUID userId) {
        return streakService.getStreak(userId);
    }

    @PostMapping("/getLongest")
    @Async
    public CompletableFuture<ResponseEntity<Integer>> getLongestStreak(@RequestBody UUID userId) {
        return streakService.getLongestStreak(userId);
    }

    @PostMapping("/check")
    @Async
    public CompletableFuture<ResponseEntity<String>> checkStreak(@RequestBody UUID userId) {
        return streakService.checkStreak(userId);
    }
}
