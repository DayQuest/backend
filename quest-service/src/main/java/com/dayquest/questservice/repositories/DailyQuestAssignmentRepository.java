package com.dayquest.questservice.repositories;

import com.dayquest.questservice.models.DailyQuestAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyQuestAssignmentRepository extends JpaRepository<DailyQuestAssignment, UUID> {

    Optional<DailyQuestAssignment> findByUserIdAndAssignedDate(UUID userId, LocalDate assignedDate);

    boolean existsByUserIdAndAssignedDate(UUID userId, LocalDate assignedDate);

    List<DailyQuestAssignment> findByUserId(UUID userId);

    @Query("SELECT d FROM DailyQuestAssignment d WHERE d.userId = :userId AND d.completed = false AND d.assignedDate = :date")
    Optional<DailyQuestAssignment> findActiveAssignment(@Param("userId") UUID userId, @Param("date") LocalDate date);

    @Query("SELECT d FROM DailyQuestAssignment d WHERE d.userId = :userId ORDER BY d.assignedDate DESC")
    List<DailyQuestAssignment> findRecentAssignments(@Param("userId") UUID userId);

    @Query("SELECT COUNT(d) FROM DailyQuestAssignment d WHERE d.userId = :userId AND d.completed = true")
    long countCompletedByUser(@Param("userId") UUID userId);

    void deleteByUserIdAndAssignedDate(UUID userId, LocalDate assignedDate);

    void deleteByUserId(UUID userId);
}

