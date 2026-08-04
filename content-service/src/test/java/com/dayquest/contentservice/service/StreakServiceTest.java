package com.dayquest.contentservice.service;

import com.dayquest.contentservice.model.Streak;
import com.dayquest.contentservice.repository.StreakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Streak Service Tests")
class StreakServiceTest {

    @Mock
    private StreakRepository streakRepository;

    @InjectMocks
    private StreakService streakService;

    private Streak testStreak;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        testStreak = new Streak();
        testStreak.setId(UUID.randomUUID());
        testStreak.setUserUuid(testUserId);
        testStreak.setCurrentStreak(5);
        testStreak.setLongestStreak(10);
        testStreak.setLastActivityDate(LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("Should record activity and increment streak for consecutive day")
    void shouldIncrementStreakForConsecutiveDay() {
        testStreak.setLastActivityDate(LocalDateTime.now().minusDays(1));
        when(streakRepository.findByUserUuid(testUserId)).thenReturn(Optional.of(testStreak));
        when(streakRepository.save(any(Streak.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Streak result = streakService.recordActivity(testUserId);

        assertNotNull(result);
        assertEquals(6, result.getCurrentStreak());
        verify(streakRepository).save(testStreak);
    }

    @Test
    @DisplayName("Should create new streak for new user")
    void shouldCreateNewStreakForNewUser() {
        when(streakRepository.findByUserUuid(testUserId)).thenReturn(Optional.empty());
        when(streakRepository.save(any(Streak.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Streak result = streakService.recordActivity(testUserId);

        assertNotNull(result);
        assertEquals(1, result.getCurrentStreak());
        assertEquals(testUserId, result.getUserUuid());
        verify(streakRepository).save(any(Streak.class));
    }

    @Test
    @DisplayName("Should reset streak when activity broken for more than 1 day")
    void shouldResetStreakWhenBroken() {
        testStreak.setLastActivityDate(LocalDateTime.now().minusDays(3));
        when(streakRepository.findByUserUuid(testUserId)).thenReturn(Optional.of(testStreak));
        when(streakRepository.save(any(Streak.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Streak result = streakService.recordActivity(testUserId);

        assertNotNull(result);
        assertEquals(1, result.getCurrentStreak());
        verify(streakRepository).save(testStreak);
    }

    @Test
    @DisplayName("Should not increment streak for same day activity")
    void shouldNotIncrementStreakForSameDayActivity() {
        testStreak.setLastActivityDate(LocalDateTime.now());
        int initialStreak = testStreak.getCurrentStreak();
        when(streakRepository.findByUserUuid(testUserId)).thenReturn(Optional.of(testStreak));
        when(streakRepository.save(any(Streak.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Streak result = streakService.recordActivity(testUserId);

        assertEquals(initialStreak, result.getCurrentStreak());
    }

    @Test
    @DisplayName("Should get streak by user ID")
    void shouldGetStreakByUserId() {
        when(streakRepository.findByUserUuid(testUserId)).thenReturn(Optional.of(testStreak));

        Optional<Streak> result = streakService.getStreak(testUserId);

        assertTrue(result.isPresent());
        assertEquals(testStreak.getCurrentStreak(), result.get().getCurrentStreak());
    }

    @Test
    @DisplayName("Should return empty when streak not found")
    void shouldReturnEmptyWhenStreakNotFound() {
        when(streakRepository.findByUserUuid(testUserId)).thenReturn(Optional.empty());

        Optional<Streak> result = streakService.getStreak(testUserId);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should get top streaks")
    void shouldGetTopStreaks() {
        Page<Streak> streakPage = new PageImpl<>(List.of(testStreak));
        when(streakRepository.findTopStreaks(any(PageRequest.class))).thenReturn(streakPage);

        Page<Streak> result = streakService.getTopStreaks(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        verify(streakRepository).findTopStreaks(any(PageRequest.class));
    }

    @Test
    @DisplayName("Should get top longest streaks")
    void shouldGetTopLongestStreaks() {
        Page<Streak> streakPage = new PageImpl<>(List.of(testStreak));
        when(streakRepository.findTopLongestStreaks(any(PageRequest.class))).thenReturn(streakPage);

        Page<Streak> result = streakService.getTopLongestStreaks(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        verify(streakRepository).findTopLongestStreaks(any(PageRequest.class));
    }
}
