package com.dayquest.dayquestbackend.quest;

import com.dayquest.dayquestbackend.auth.service.JwtService;
import com.dayquest.dayquestbackend.activity.ActivityUpdater;
import com.dayquest.dayquestbackend.quest.dto.InteractionDTO;
import com.dayquest.dayquestbackend.quest.dto.QuestDTO;
import com.dayquest.dayquestbackend.user.models.User;
import com.dayquest.dayquestbackend.user.repositories.UserRepository;
import com.dayquest.dayquestbackend.user.services.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
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

    @Autowired
    private RatingService ratingService;

    @GetMapping
    @Async
    @Cacheable(value = "quests", key = "#page + ':' + #size + ':' + #sortBy + ':' + #sortDirection")
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

            List<UUID> likedQuestIds = ratingService.getLikedQuests(currentUser);
            List<UUID> dislikedQuestIds = ratingService.getDislikedQuests(currentUser);

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
    @CacheEvict(value = "quests", allEntries = true)
    public CompletableFuture<ResponseEntity<Quest>> createQuest(
            @RequestBody Quest quest,
            @RequestHeader("Authorization") String token) {

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
    @CacheEvict(value = {"quests", "userLikedQuests"}, allEntries = true)
    public CompletableFuture<ResponseEntity<String>> likeQuest(@RequestBody InteractionDTO dto,
                                                               @RequestHeader("Authorization") String token) {
        return ratingService.rateQuest(token, dto.getUuid(), true);
    }

    @DeleteMapping("/like")
    @CacheEvict(value = {"quests", "userLikedQuests"}, allEntries = true)
    public CompletableFuture<ResponseEntity<String>> unlikeQuest(@RequestBody InteractionDTO dto,
                                                                 @RequestHeader("Authorization") String token) {
        return ratingService.removeQuestRating(token, dto.getUuid());
    }

    @PostMapping("/dislike")
    @CacheEvict(value = {"quests", "userDislikedQuests"}, allEntries = true)
    public CompletableFuture<ResponseEntity<String>> dislikeQuest(@RequestBody InteractionDTO dto,
                                                                  @RequestHeader("Authorization") String token) {
        return ratingService.rateQuest(token, dto.getUuid(), false);
    }

    @DeleteMapping("/dislike")
    @CacheEvict(value = {"quests", "userDislikedQuests"}, allEntries = true)
    public CompletableFuture<ResponseEntity<String>> undislikeQuest(@RequestBody InteractionDTO dto,
                                                                    @RequestHeader("Authorization") String token) {
        return ratingService.removeQuestRating(token, dto.getUuid());
    }

    @GetMapping("/{userid}")
    @Async
    @Cacheable(value = "quests", key = "'user:' + #userid")
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
