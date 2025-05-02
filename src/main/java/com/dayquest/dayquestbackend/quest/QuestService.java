package com.dayquest.dayquestbackend.quest;

import com.dayquest.dayquestbackend.activity.ActivityUpdater;
import com.dayquest.dayquestbackend.quest.dto.InteractionDTO;
import com.dayquest.dayquestbackend.user.models.User;
import com.dayquest.dayquestbackend.user.repositories.UserRepository;
import com.dayquest.dayquestbackend.user.services.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@Service
public class QuestService {

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestReportRepository questReportRepository;

    @Autowired
    private ActivityUpdater activityUpdater;

    private static final Logger logger = Logger.getLogger(QuestService.class.getName());
    @Autowired
    private RatingService ratingService;

    @Async
    @Transactional
    public CompletableFuture<Quest> createQuest(String title, String description, User user) {
        return CompletableFuture.supplyAsync(() -> {
            if (title == null || description == null || title.isBlank() || description.isBlank()) {
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
            if (allQuests.isEmpty()) return allQuests;
            int topCount = Math.max(1, (int) Math.ceil(allQuests.size() * 0.3));
            return allQuests.subList(0, topCount);
        });
    }
}
