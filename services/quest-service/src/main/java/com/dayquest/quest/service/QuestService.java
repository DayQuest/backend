package com.dayquest.quest.service;

import com.dayquest.common.exception.ResourceNotFoundException;
import com.dayquest.quest.dto.CreateQuestDTO;
import com.dayquest.quest.dto.QuestResponseDTO;
import com.dayquest.quest.model.Quest;
import com.dayquest.quest.repository.QuestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class QuestService {

    private static final Logger logger = LoggerFactory.getLogger(QuestService.class);

    private final QuestRepository questRepository;

    public QuestService(QuestRepository questRepository) {
        this.questRepository = questRepository;
    }

    @Cacheable(value = "quests", key = "#uuid")
    public QuestResponseDTO getQuestById(UUID uuid) {
        Quest quest = questRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found with id: " + uuid));
        return new QuestResponseDTO(quest);
    }

    public Page<QuestResponseDTO> getAllQuests(int page, int size) {
        Page<Quest> quests = questRepository.findByActiveTrue(PageRequest.of(page, size));
        return quests.map(QuestResponseDTO::new);
    }

    @Transactional
    @CacheEvict(value = "quests", allEntries = true)
    public QuestResponseDTO createQuest(CreateQuestDTO dto, UUID creatorUuid) {
        Quest quest = new Quest(dto.getTitle(), dto.getDescription(), creatorUuid);
        quest = questRepository.save(quest);
        return new QuestResponseDTO(quest);
    }

    @Transactional
    @CacheEvict(value = "quests", key = "#uuid")
    public void deleteQuest(UUID uuid) {
        Quest quest = questRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found"));
        quest.setActive(false);
        questRepository.save(quest);
    }

    @Transactional
    @CacheEvict(value = "quests", key = "#uuid")
    public void likeQuest(UUID uuid) {
        Quest quest = questRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found"));
        quest.setLikes(quest.getLikes() + 1);
        questRepository.save(quest);
    }

    @Transactional
    @CacheEvict(value = "quests", key = "#uuid")
    public void dislikeQuest(UUID uuid) {
        Quest quest = questRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found"));
        quest.setDislikes(quest.getDislikes() + 1);
        questRepository.save(quest);
    }

    @Cacheable(value = "topQuests")
    public List<QuestResponseDTO> getTop30PercentQuests() {
        long totalCount = questRepository.countByActiveTrue();
        int top30Count = Math.max(1, (int) (totalCount * 0.3));
        
        List<Quest> topQuests = questRepository.findTopRatedQuests(PageRequest.of(0, top30Count));
        return topQuests.stream()
                .map(QuestResponseDTO::new)
                .collect(Collectors.toList());
    }

    public QuestResponseDTO getRandomTopQuest() {
        List<QuestResponseDTO> topQuests = getTop30PercentQuests();
        if (topQuests.isEmpty()) {
            throw new ResourceNotFoundException("No quests available");
        }
        Random random = new Random();
        return topQuests.get(random.nextInt(topQuests.size()));
    }

    public QuestResponseDTO getRandomTopQuestExcluding(UUID excludeQuestId) {
        List<QuestResponseDTO> topQuests = getTop30PercentQuests();
        topQuests = topQuests.stream()
                .filter(q -> !q.getUuid().equals(excludeQuestId))
                .collect(Collectors.toList());
        
        if (topQuests.isEmpty()) {
            throw new ResourceNotFoundException("No alternative quests available");
        }
        
        Random random = new Random();
        return topQuests.get(random.nextInt(topQuests.size()));
    }

    public Page<QuestResponseDTO> searchQuests(String query, int page, int size) {
        Page<Quest> quests = questRepository.findByTitleContainingIgnoreCase(query, PageRequest.of(page, size));
        return quests.map(QuestResponseDTO::new);
    }

    public Page<QuestResponseDTO> getQuestsByCreator(UUID creatorUuid, int page, int size) {
        Page<Quest> quests = questRepository.findByCreatorUuid(creatorUuid, PageRequest.of(page, size));
        return quests.map(QuestResponseDTO::new);
    }

    // Scheduled task to refresh top quests cache
    @Scheduled(fixedRate = 3600000) // Every hour
    @CacheEvict(value = "topQuests", allEntries = true)
    public void refreshTopQuestsCache() {
        logger.info("Refreshing top quests cache");
    }
}
