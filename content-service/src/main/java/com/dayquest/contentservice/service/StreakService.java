package com.dayquest.contentservice.service;

import com.dayquest.contentservice.model.Streak;
import com.dayquest.contentservice.repository.StreakRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class StreakService {

    private static final Logger logger = LoggerFactory.getLogger(StreakService.class);

    private final StreakRepository streakRepository;

    public StreakService(StreakRepository streakRepository) {
        this.streakRepository = streakRepository;
    }

    @Transactional
    public Streak recordActivity(UUID userUuid) {
        Optional<Streak> existingStreak = streakRepository.findByUserUuid(userUuid);

        Streak streak;
        if (existingStreak.isPresent()) {
            streak = existingStreak.get();
            LocalDate lastActivity = streak.getLastActivityDate() != null
                    ? streak.getLastActivityDate().toLocalDate()
                    : null;
            LocalDate today = LocalDate.now();

            if (lastActivity == null || lastActivity.isBefore(today.minusDays(1))) {
                // Streak broken - reset
                streak.setCurrentStreak(1);
            } else if (lastActivity.equals(today.minusDays(1))) {
                // Consecutive day - increment
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            }
            // If same day, don't increment

            streak.setLastActivityDate(LocalDateTime.now());
        } else {
            streak = new Streak();
            streak.setUserUuid(userUuid);
            streak.setCurrentStreak(1);
            streak.setLastActivityDate(LocalDateTime.now());
        }

        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        return streakRepository.save(streak);
    }

    public Optional<Streak> getStreak(UUID userUuid) {
        return streakRepository.findByUserUuid(userUuid);
    }

    public Page<Streak> getTopStreaks(Pageable pageable) {
        return streakRepository.findTopStreaks(pageable);
    }

    public Page<Streak> getTopLongestStreaks(Pageable pageable) {
        return streakRepository.findTopLongestStreaks(pageable);
    }

    /**
     * Reset broken streaks at midnight.
     * Uses ShedLock to ensure this runs only once across all instances.
     */
    @Scheduled(cron = "0 0 0 * * *") // Run at midnight every day
    @SchedulerLock(name = "resetBrokenStreaks", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    @Transactional
    public void resetBrokenStreaks() {
        logger.info("Starting streak reset job");
        LocalDateTime cutoff = LocalDateTime.now().minusDays(2);
        // Use efficient batch update instead of loading all streaks into memory
        int resetCount = streakRepository.resetBrokenStreaks(cutoff);
        logger.info("Reset {} broken streaks", resetCount);
    }
}

