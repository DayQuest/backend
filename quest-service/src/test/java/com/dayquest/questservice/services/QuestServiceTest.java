package com.dayquest.questservice.services;

import com.dayquest.questservice.models.Quest;
import com.dayquest.questservice.repositories.QuestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Quest Service Tests")
class QuestServiceTest {

    @Mock
    private QuestRepository questRepository;

    @InjectMocks
    private QuestService questService;

    private Quest testQuest;
    private UUID testUserId;
    private UUID testQuestId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testQuestId = UUID.randomUUID();

        testQuest = new Quest();
        testQuest.setUuid(testQuestId);
        testQuest.setCreatorUuid(testUserId);
        testQuest.setTitle("Test Quest Title");
        testQuest.setDescription("Test Quest Description with sufficient length");
        testQuest.setLikes(10);
        testQuest.setDislikes(2);
    }

    @Test
    @DisplayName("Should create quest successfully")
    void shouldCreateQuest() throws ExecutionException, InterruptedException {
        when(questRepository.save(any(Quest.class))).thenAnswer(invocation -> {
            Quest q = invocation.getArgument(0);
            q.setUuid(testQuestId);
            return q;
        });

        CompletableFuture<Quest> result = questService.createQuest(
                "Test Quest Title",
                "Test Quest Description with sufficient length",
                testUserId,
                "testuser"
        );

        Quest quest = result.get();
        assertNotNull(quest);
        assertEquals("Test Quest Title", quest.getTitle());
        assertEquals(testUserId, quest.getCreatorUuid());
        verify(questRepository).save(any(Quest.class));
    }

    @Test
    @DisplayName("Should return null when title is blank")
    void shouldReturnNullWhenTitleIsBlank() throws ExecutionException, InterruptedException {
        CompletableFuture<Quest> result = questService.createQuest(
                "",
                "Test Quest Description",
                testUserId,
                "testuser"
        );

        Quest quest = result.get();
        assertNull(quest);
        verify(questRepository, never()).save(any(Quest.class));
    }

    @Test
    @DisplayName("Should return null when description is blank")
    void shouldReturnNullWhenDescriptionIsBlank() throws ExecutionException, InterruptedException {
        CompletableFuture<Quest> result = questService.createQuest(
                "Test Quest Title",
                "",
                testUserId,
                "testuser"
        );

        Quest quest = result.get();
        assertNull(quest);
        verify(questRepository, never()).save(any(Quest.class));
    }

    @Test
    @DisplayName("Should return null when title is null")
    void shouldReturnNullWhenTitleIsNull() throws ExecutionException, InterruptedException {
        CompletableFuture<Quest> result = questService.createQuest(
                null,
                "Test Quest Description",
                testUserId,
                "testuser"
        );

        Quest quest = result.get();
        assertNull(quest);
        verify(questRepository, never()).save(any(Quest.class));
    }

    @Test
    @DisplayName("Should get top 30 percent quests")
    void shouldGetTop30PercentQuests() throws ExecutionException, InterruptedException {
        Quest quest1 = new Quest();
        quest1.setLikes(10);
        quest1.setDislikes(2);

        List<Quest> topQuests = List.of(quest1);
        Page<Quest> questPage = new PageImpl<>(topQuests);

        when(questRepository.countActiveQuests()).thenReturn(3L);
        when(questRepository.findTopQuestsByScore(any(Pageable.class))).thenReturn(questPage);

        CompletableFuture<List<Quest>> result = questService.getTop30PercentQuests();

        List<Quest> resultQuests = result.get();
        assertFalse(resultQuests.isEmpty());
        assertEquals(1, resultQuests.size());
        assertEquals(quest1, resultQuests.get(0));
        verify(questRepository).countActiveQuests();
        verify(questRepository).findTopQuestsByScore(any(Pageable.class));
    }

    @Test
    @DisplayName("Should return empty list when no quests exist")
    void shouldReturnEmptyListWhenNoQuestsExist() throws ExecutionException, InterruptedException {
        when(questRepository.countActiveQuests()).thenReturn(0L);

        CompletableFuture<List<Quest>> result = questService.getTop30PercentQuests();

        List<Quest> topQuests = result.get();
        assertTrue(topQuests.isEmpty());
        verify(questRepository).countActiveQuests();
        verify(questRepository, never()).findTopQuestsByScore(any(Pageable.class));
    }

    @Test
    @DisplayName("Should return at least one quest when few quests exist")
    void shouldReturnAtLeastOneQuestWhenFewExist() throws ExecutionException, InterruptedException {
        List<Quest> quests = new ArrayList<>();
        quests.add(testQuest);
        Page<Quest> questPage = new PageImpl<>(quests);

        when(questRepository.countActiveQuests()).thenReturn(1L);
        when(questRepository.findTopQuestsByScore(any(Pageable.class))).thenReturn(questPage);

        CompletableFuture<List<Quest>> result = questService.getTop30PercentQuests();

        List<Quest> topQuests = result.get();
        assertEquals(1, topQuests.size());
        verify(questRepository).countActiveQuests();
        verify(questRepository).findTopQuestsByScore(any(Pageable.class));
    }
}
