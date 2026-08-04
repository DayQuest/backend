package com.dayquest.contentservice.controller;

import com.dayquest.contentservice.model.Streak;
import com.dayquest.contentservice.service.StreakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/streaks")
@Tag(name = "Streaks", description = "User streak management")
public class StreakController {

    private final StreakService streakService;

    public StreakController(StreakService streakService) {
        this.streakService = streakService;
    }

    @PostMapping("/record")
    @Operation(summary = "Record daily activity for streak")
    public ResponseEntity<Streak> recordActivity(@RequestHeader("X-User-Id") String userIdHeader) {
        UUID userUuid = UUID.fromString(userIdHeader);
        Streak streak = streakService.recordActivity(userUuid);
        return ResponseEntity.ok(streak);
    }

    @GetMapping
    @Operation(summary = "Get current user's streak")
    public ResponseEntity<Object> getMyStreak(@RequestHeader("X-User-Id") String userIdHeader) {
        UUID userUuid = UUID.fromString(userIdHeader);
        return streakService.getStreak(userUuid)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Map.of("currentStreak", 0, "longestStreak", 0)));
    }

    @GetMapping("/user/{userUuid}")
    @Operation(summary = "Get a user's streak")
    public ResponseEntity<Object> getUserStreak(@PathVariable UUID userUuid) {
        return streakService.getStreak(userUuid)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Map.of("currentStreak", 0, "longestStreak", 0)));
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get streak leaderboard")
    public ResponseEntity<Page<Streak>> getLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(streakService.getTopStreaks(PageRequest.of(page, size)));
    }

    @GetMapping("/leaderboard/longest")
    @Operation(summary = "Get longest streak leaderboard")
    public ResponseEntity<Page<Streak>> getLongestStreakLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(streakService.getTopLongestStreaks(PageRequest.of(page, size)));
    }
}

