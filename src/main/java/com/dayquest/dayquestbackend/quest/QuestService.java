package com.dayquest.dayquestbackend.quest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class QuestService {
    @Autowired
    private QuestRepository questRepository;

    @Async
    public CompletableFuture<List<Quest>> getAllQuests() {
        return CompletableFuture.completedFuture(questRepository.findAll());
    }

    @Async
    public CompletableFuture<Quest> suggestQuest(String title, String description) {
        Quest quest = new Quest();
        quest.setTitle(title);
        quest.setDescription(description);
        return CompletableFuture.completedFuture(questRepository.save(quest));
    }

    @Async
    public CompletableFuture<List<Quest>> getTop10PercentQuests() {
        return CompletableFuture.supplyAsync(() -> {
            List<Quest> allQuests = questRepository.findAll();
            allQuests.sort((q1, q2) -> (q2.getLikes() - q2.getDislikes()) - (q1.getLikes() - q1.getDislikes()));

            if (allQuests.isEmpty()) {
                return allQuests;
            }

            int top10PercentCount = Math.max(1, (int) Math.ceil(allQuests.size() * 0.1));
            return allQuests.subList(0, top10PercentCount);
        });
    }

    @Async
    public CompletableFuture<Quest> likeQuest(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Quest> quest = questRepository.findById(id);
            if (quest.isEmpty()) {
                return null;
            }

            quest.get().setLikes(quest.get().getLikes() + 1);
            return questRepository.save(quest.get());
        });
    }

    @Async
    public CompletableFuture<Quest> dislikeQuest(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Quest> quest = questRepository.findById(id);
            if (quest.isEmpty()) {
                return null;
            }

            quest.get().setDislikes(quest.get().getDislikes() + 1);
            return questRepository.save(quest.get());
        });
    }
}