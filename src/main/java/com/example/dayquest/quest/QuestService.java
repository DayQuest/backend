package com.example.dayquest.quest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestService {
    @Autowired
    private QuestRepository questRepository;

    public List<Quest> getAllQuests() {
        return questRepository.findAll();
    }

    public Quest suggestQuest(String title, String description) {
        Quest quest = new Quest();
        quest.setTitle(title);
        quest.setDescription(description);
        return questRepository.save(quest);
    }

    public List<Quest> getTop10PercentQuests() {
        List<Quest> allQuests = questRepository.findAll();
        allQuests.sort((q1, q2) -> (q2.getLikes() - q2.getDislikes()) - (q1.getLikes() - q1.getDislikes()));

        if (allQuests.isEmpty()) {
            return allQuests;
        }

        int top10PercentCount = Math.max(1, (int) Math.ceil(allQuests.size() * 0.1));
        return allQuests.subList(0, top10PercentCount);
    }

    public Quest likeQuest(Long id) {
        Quest quest = questRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quest nicht gefunden"));
        quest.setLikes(quest.getLikes() + 1);
        return questRepository.save(quest);
    }

    public Quest dislikeQuest(Long id) {
        Quest quest = questRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quest nicht gefunden"));
        quest.setDislikes(quest.getDislikes() + 1);
        return questRepository.save(quest);
    }
}
