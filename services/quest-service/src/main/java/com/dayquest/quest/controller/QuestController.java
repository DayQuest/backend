package com.dayquest.quest.controller;

import com.dayquest.quest.dto.CreateQuestDTO;
import com.dayquest.quest.dto.QuestResponseDTO;
import com.dayquest.quest.service.QuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/quests")
@Tag(name = "Quest Service", description = "Quest management endpoints")
public class QuestController {

    private final QuestService questService;

    public QuestController(QuestService questService) {
        this.questService = questService;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Quest Service is running");
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get quest by UUID")
    public ResponseEntity<QuestResponseDTO> getQuestById(@PathVariable UUID uuid) {
        QuestResponseDTO quest = questService.getQuestById(uuid);
        return ResponseEntity.ok(quest);
    }

    @GetMapping
    @Operation(summary = "Get all quests")
    public ResponseEntity<Map<String, Object>> getAllQuests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<QuestResponseDTO> questPage = questService.getAllQuests(page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("quests", questPage.getContent());
        response.put("currentPage", questPage.getNumber());
        response.put("totalItems", questPage.getTotalElements());
        response.put("totalPages", questPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new quest")
    public ResponseEntity<QuestResponseDTO> createQuest(
            @RequestBody @Valid CreateQuestDTO dto,
            @RequestHeader("X-User-Id") String userId) {
        QuestResponseDTO quest = questService.createQuest(dto, UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(quest);
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Delete a quest")
    public ResponseEntity<Void> deleteQuest(@PathVariable UUID uuid) {
        questService.deleteQuest(uuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{uuid}/like")
    @Operation(summary = "Like a quest")
    public ResponseEntity<String> likeQuest(
            @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") String userId) {
        questService.likeQuest(uuid);
        return ResponseEntity.ok("Quest liked");
    }

    @PostMapping("/{uuid}/dislike")
    @Operation(summary = "Dislike a quest")
    public ResponseEntity<String> dislikeQuest(
            @PathVariable UUID uuid,
            @RequestHeader("X-User-Id") String userId) {
        questService.dislikeQuest(uuid);
        return ResponseEntity.ok("Quest disliked");
    }

    @GetMapping("/top")
    @Operation(summary = "Get top 30% rated quests")
    public ResponseEntity<List<QuestResponseDTO>> getTopQuests() {
        List<QuestResponseDTO> quests = questService.getTop30PercentQuests();
        return ResponseEntity.ok(quests);
    }

    @GetMapping("/random")
    @Operation(summary = "Get random top-rated quest")
    public ResponseEntity<QuestResponseDTO> getRandomQuest() {
        QuestResponseDTO quest = questService.getRandomTopQuest();
        return ResponseEntity.ok(quest);
    }

    @GetMapping("/random/exclude/{excludeUuid}")
    @Operation(summary = "Get random top-rated quest excluding a specific quest")
    public ResponseEntity<QuestResponseDTO> getRandomQuestExcluding(@PathVariable UUID excludeUuid) {
        QuestResponseDTO quest = questService.getRandomTopQuestExcluding(excludeUuid);
        return ResponseEntity.ok(quest);
    }

    @GetMapping("/search")
    @Operation(summary = "Search quests by title")
    public ResponseEntity<Map<String, Object>> searchQuests(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<QuestResponseDTO> questPage = questService.searchQuests(query, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("quests", questPage.getContent());
        response.put("currentPage", questPage.getNumber());
        response.put("totalItems", questPage.getTotalElements());
        response.put("totalPages", questPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/creator/{creatorUuid}")
    @Operation(summary = "Get quests by creator")
    public ResponseEntity<Map<String, Object>> getQuestsByCreator(
            @PathVariable UUID creatorUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<QuestResponseDTO> questPage = questService.getQuestsByCreator(creatorUuid, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("quests", questPage.getContent());
        response.put("currentPage", questPage.getNumber());
        response.put("totalItems", questPage.getTotalElements());
        response.put("totalPages", questPage.getTotalPages());

        return ResponseEntity.ok(response);
    }
}
