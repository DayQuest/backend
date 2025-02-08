package com.dayquest.dayquestbackend.quest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.dayquest.dayquestbackend.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class QuestService {

    @Autowired
    private QuestRepository questRepository;

    @Async
    public CompletableFuture<Quest> createQuest(String title, String description, User user) {
        return CompletableFuture.supplyAsync(() -> {
            if (title == null || description == null || title.isEmpty() || description.isEmpty() || title.isBlank() || description.isBlank()) {
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
    public CompletableFuture<List<Quest>> getTop10PercentQuests() {
        return CompletableFuture.supplyAsync(() -> {
            List<Quest> allQuests = questRepository.findAll();
            allQuests.sort((q1, q2) -> (q2.getLikes() - q2.getDislikes()) - (q1.getLikes()
                    - q1.getDislikes()));

            if (allQuests.isEmpty()) {
                return allQuests;
            }

            int top40PercentCount = Math.max(1, (int) Math.ceil(allQuests.size() * 0.8));
            return allQuests.subList(0, top40PercentCount);
        });
    }
}