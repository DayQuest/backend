package com.dayquest.dayquestbackend.quest;

import com.dayquest.dayquestbackend.activity.ActivityUpdater;
import com.dayquest.dayquestbackend.quest.dto.InteractionDTO;
import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class QuestService {

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityUpdater activityUpdater;

    @Async
    @Transactional
    public CompletableFuture<Quest> createQuest(String title, String description, User user) {
        return CompletableFuture.supplyAsync(() -> {
            if (title == null || description == null ||
                    title.isBlank() || description.isBlank()) {
                return null;
            }
            Quest quest = new Quest();
            quest.setTitle(title);
            quest.setDescription(description);
            quest.setCreatorUuid(user.getUuid());
            questRepository.save(quest);
            return quest;
        });
    }

    @Async
    public CompletableFuture<List<Quest>> getTop30PercentQuests() {
        return CompletableFuture.supplyAsync(() -> {
            List<Quest> allQuests = questRepository.findAll();
            allQuests.sort((q1, q2) -> (q2.getLikes() - q2.getDislikes()) - (q1.getLikes() - q1.getDislikes()));
            if (allQuests.isEmpty()) {
                return allQuests;
            }
            int topCount = Math.max(1, (int) Math.ceil(allQuests.size() * 0.3));
            return allQuests.subList(0, topCount);
        });
    }

    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<?>> likeQuest(InteractionDTO dto) {
        Optional<User> userOpt = userRepository.findById(dto.getUserUuid());
        Optional<Quest> questOpt = questRepository.findById(dto.getUuid());
        if (userOpt.isEmpty() || questOpt.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
        }
        User user = userOpt.get();
        Quest quest = questOpt.get();

        if (user.getLikedQuests().contains(quest.getUuid())) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Already liked")
            );
        }
        if (user.getDislikedQuests().remove(quest.getUuid())) {
            questRepository.decrementDislikes(quest.getUuid());
        }
        user.getLikedQuests().add(quest.getUuid());
        questRepository.incrementLikes(quest.getUuid());

        activityUpdater.increaseInteractions(user);
        userRepository.save(user);
        return CompletableFuture.completedFuture(
                ResponseEntity.ok("Successfully liked quest")
        );
    }

    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<?>> unlikeQuest(InteractionDTO dto) {
        Optional<User> userOpt = userRepository.findById(dto.getUserUuid());
        Optional<Quest> questOpt = questRepository.findById(dto.getUuid());
        if (userOpt.isEmpty() || questOpt.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
        }
        User user = userOpt.get();
        Quest quest = questOpt.get();

        if (!user.getLikedQuests().contains(quest.getUuid())) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Not liked")
            );
        }
        user.getLikedQuests().remove(quest.getUuid());
        questRepository.decrementLikes(quest.getUuid());

        userRepository.save(user);
        return CompletableFuture.completedFuture(
                ResponseEntity.ok("Successfully unliked quest")
        );
    }

    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<?>> dislikeQuest(InteractionDTO dto) {
        Optional<User> userOpt = userRepository.findById(dto.getUserUuid());
        Optional<Quest> questOpt = questRepository.findById(dto.getUuid());
        if (userOpt.isEmpty() || questOpt.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
        }
        User user = userOpt.get();
        Quest quest = questOpt.get();

        if (user.getDislikedQuests().contains(quest.getUuid())) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.CONFLICT).body("Already disliked")
            );
        }
        if (user.getLikedQuests().remove(quest.getUuid())) {
            questRepository.decrementLikes(quest.getUuid());
        }
        user.getDislikedQuests().add(quest.getUuid());
        questRepository.incrementDislikes(quest.getUuid());

        activityUpdater.increaseInteractions(user);
        userRepository.save(user);
        return CompletableFuture.completedFuture(
                ResponseEntity.ok("Successfully disliked quest")
        );
    }

    @Async
    @Transactional
    public CompletableFuture<ResponseEntity<?>> undislikeQuest(InteractionDTO dto) {
        Optional<User> userOpt = userRepository.findById(dto.getUserUuid());
        Optional<Quest> questOpt = questRepository.findById(dto.getUuid());
        if (userOpt.isEmpty() || questOpt.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
        }
        User user = userOpt.get();
        Quest quest = questOpt.get();

        if (!user.getDislikedQuests().contains(quest.getUuid())) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.CONFLICT).body("Not disliked")
            );
        }
        user.getDislikedQuests().remove(quest.getUuid());
        questRepository.decrementDislikes(quest.getUuid());

        userRepository.save(user);
        return CompletableFuture.completedFuture(
                ResponseEntity.ok("Successfully undisliked quest")
        );
    }

}
