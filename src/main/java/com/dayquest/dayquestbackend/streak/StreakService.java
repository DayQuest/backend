package com.dayquest.dayquestbackend.streak;

import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class StreakService {
    @Autowired
    private StreakRepository streakRepository;

    @Autowired
    private UserRepository userRepository;


    public CompletableFuture<ResponseEntity<String>> createStreak(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }
            Streak streak = new Streak();
            streak.setUserId(userId);
            streak.setStreak(0);
            streak.setLongestStreak(0);
            streak.setLastUpdated(LocalDateTime.now());
            streakRepository.save(streak);
            return ResponseEntity.ok("Streak created");
        });
    }

    public void updateStreak(UUID userId) {
            Streak streak = streakRepository.findByUserId(userId);
            if (streak == null) {
                throw new IllegalArgumentException("Streak not found");
            }
            streak.setStreak(streak.getStreak() + 1);
            if (streak.getStreak() > streak.getLongestStreak()) {
                streak.setLongestStreak(streak.getStreak());
            }
            if(!isConsecutiveDay(streak.getLastUpdated(), LocalDateTime.now())) {
                streak.setStreak(0);
            }
            streak.setLastUpdated(LocalDateTime.now());
            streakRepository.save(streak);
            return;
    }

    private boolean isConsecutiveDay(LocalDateTime lastUpdated, LocalDateTime now) {
        return lastUpdated.toLocalDate().plusDays(1).isEqual(now.toLocalDate());
    }

    public CompletableFuture<ResponseEntity<Integer>> getStreak(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            Streak streak = streakRepository.findByUserId(userId);
            if (streak == null) {
                return ResponseEntity.badRequest().body(null);
            }
            return ResponseEntity.ok(streak.getStreak());
        });
    }

    public CompletableFuture<ResponseEntity<Integer>> getLongestStreak(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            Streak streak = streakRepository.findByUserId(userId);
            if (streak == null) {
                return ResponseEntity.badRequest().body(null);
            }
            return ResponseEntity.ok(streak.getLongestStreak());
        });
    }

    public CompletableFuture<ResponseEntity<String>> checkStreak(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            Streak streak = streakRepository.findByUserId(userId);
            if (streak == null) {
                return ResponseEntity.badRequest().body("Streak not found");
            }
            if(!isConsecutiveDay(streak.getLastUpdated(), LocalDateTime.now())) {
                streak.setStreak(0);
                streakRepository.save(streak);
                return ResponseEntity.ok("Streak reset");
            }
            return ResponseEntity.ok("Streak valid");
        });
    }
}
