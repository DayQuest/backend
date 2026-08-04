package com.dayquest.contentservice.controller;

import com.dayquest.contentservice.model.Badge;
import com.dayquest.contentservice.service.BadgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/badges")
@Tag(name = "Badges", description = "Badge management endpoints")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    @GetMapping
    @Operation(summary = "Get all badges")
    public ResponseEntity<List<Badge>> getAllBadges() {
        return ResponseEntity.ok(badgeService.getAllBadges());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get badge by ID")
    public ResponseEntity<Badge> getBadge(@PathVariable UUID id) {
        return badgeService.getBadge(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userUuid}")
    @Operation(summary = "Get badges for a user")
    public ResponseEntity<List<Badge>> getUserBadges(@PathVariable UUID userUuid) {
        return ResponseEntity.ok(badgeService.getUserBadges(userUuid));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my badges")
    public ResponseEntity<List<Badge>> getMyBadges(@RequestHeader("X-User-Id") String userIdHeader) {
        UUID userUuid = UUID.fromString(userIdHeader);
        return ResponseEntity.ok(badgeService.getUserBadges(userUuid));
    }

    @PostMapping
    @Operation(summary = "Create a new badge (admin)")
    public ResponseEntity<Badge> createBadge(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.get("description");
        String iconUrl = body.get("iconUrl");

        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Badge badge = badgeService.createBadge(name, description, iconUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(badge);
    }

    @PostMapping("/{badgeId}/award/{userUuid}")
    @Operation(summary = "Award badge to user (admin)")
    public ResponseEntity<?> awardBadge(
            @PathVariable UUID badgeId,
            @PathVariable UUID userUuid) {

        boolean awarded = badgeService.awardBadge(badgeId, userUuid);
        if (!awarded) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("message", "Badge awarded"));
    }
}

