package com.dayquest.questservice.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dayquest.questservice.models.Quest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestRepository extends JpaRepository<Quest, UUID> {

    Optional<Quest> findByUuidAndDeletedFalse(UUID uuid);

    @Modifying
    @Query("UPDATE Quest q SET q.likes = q.likes + 1, q.score = q.likes + 1 - q.dislikes WHERE q.uuid = :uuid")
    int incrementLikes(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE Quest q SET q.dislikes = q.dislikes + 1, q.score = q.likes - q.dislikes - 1 WHERE q.uuid = :uuid")
    int incrementDislikes(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE Quest q SET q.likes = q.likes - 1, q.score = q.likes - 1 - q.dislikes WHERE q.uuid = :uuid AND q.likes > 0")
    int decrementLikes(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE Quest q SET q.dislikes = q.dislikes - 1, q.score = q.likes - q.dislikes + 1 WHERE q.uuid = :uuid AND q.dislikes > 0")
    int decrementDislikes(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE Quest q SET q.videoCount = q.videoCount + 1 WHERE q.uuid = :uuid")
    int incrementVideoCount(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE Quest q SET q.videoCount = q.videoCount - 1 WHERE q.uuid = :uuid AND q.videoCount > 0")
    int decrementVideoCount(@Param("uuid") UUID uuid);

    // Active quests ordered by score
    @Query("SELECT q FROM Quest q WHERE q.active = true AND q.deleted = false ORDER BY q.score DESC")
    Page<Quest> findActiveQuestsByScore(Pageable pageable);

    // Active quests ordered by creation date
    @Query("SELECT q FROM Quest q WHERE q.active = true AND q.deleted = false ORDER BY q.createdAt DESC")
    Page<Quest> findActiveQuestsByDate(Pageable pageable);

    // Quests by creator
    Page<Quest> findByCreatorUuidAndDeletedFalse(UUID creatorUuid, Pageable pageable);

    // Soft delete
    @Modifying
    @Query("UPDATE Quest q SET q.deleted = true, q.deletedAt = CURRENT_TIMESTAMP WHERE q.uuid = :uuid")
    int softDelete(@Param("uuid") UUID uuid);

    // Soft delete all quests by creator
    @Modifying
    @Query("UPDATE Quest q SET q.deleted = true, q.deletedAt = CURRENT_TIMESTAMP WHERE q.creatorUuid = :creatorUuid AND q.deleted = false")
    int softDeleteByCreatorUuid(@Param("creatorUuid") UUID creatorUuid);

    // Get top quests ordered by score (likes - dislikes) - efficient alternative to findAll()
    @Query("SELECT q FROM Quest q WHERE q.active = true AND q.deleted = false ORDER BY (q.likes - q.dislikes) DESC")
    Page<Quest> findTopQuestsByScore(Pageable pageable);

    // Count active quests for calculating percentages
    @Query("SELECT COUNT(q) FROM Quest q WHERE q.active = true AND q.deleted = false")
    long countActiveQuests();

    // For backward compatibility
    default Quest findByUuid(UUID uuid) {
        return findByUuidAndDeletedFalse(uuid).orElse(null);
    }
}
