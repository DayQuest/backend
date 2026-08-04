package com.dayquest.contentservice.repository;

import com.dayquest.contentservice.model.Streak;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StreakRepository extends JpaRepository<Streak, UUID> {

    Optional<Streak> findByUserUuid(UUID userUuid);

    @Query("SELECT s FROM Streak s ORDER BY s.currentStreak DESC")
    Page<Streak> findTopStreaks(Pageable pageable);

    @Query("SELECT s FROM Streak s ORDER BY s.longestStreak DESC")
    Page<Streak> findTopLongestStreaks(Pageable pageable);

    /**
     * Efficiently reset all broken streaks in a single query.
     * This is much more efficient than loading all streaks into memory.
     */
    @Modifying
    @Query("UPDATE Streak s SET s.currentStreak = 0 WHERE s.lastActivityDate < :cutoff AND s.currentStreak > 0")
    int resetBrokenStreaks(@Param("cutoff") LocalDateTime cutoff);
}

