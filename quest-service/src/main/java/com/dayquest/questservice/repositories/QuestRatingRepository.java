package com.dayquest.questservice.repositories;

import com.dayquest.questservice.models.QuestRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestRatingRepository extends JpaRepository<QuestRating, UUID> {

    Optional<QuestRating> findByUserIdAndQuestId(UUID userId, UUID questId);

    boolean existsByUserIdAndQuestId(UUID userId, UUID questId);

    @Query("SELECT qr.questId FROM QuestRating qr WHERE qr.userId = :userId AND qr.isLike = true")
    List<UUID> findLikedQuestIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT qr.questId FROM QuestRating qr WHERE qr.userId = :userId AND qr.isLike = false")
    List<UUID> findDislikedQuestIdsByUserId(@Param("userId") UUID userId);

    List<QuestRating> findByUserId(UUID userId);

    List<QuestRating> findByQuestId(UUID questId);

    void deleteByUserIdAndQuestId(UUID userId, UUID questId);

    void deleteByUserId(UUID userId);

    void deleteByQuestId(UUID questId);
}

