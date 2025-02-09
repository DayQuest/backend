package com.dayquest.dayquestbackend.quest;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestRepository extends JpaRepository<Quest, UUID> {
    Quest findByUuid(UUID uuid);

    @Modifying
    @Query("UPDATE Quest q SET q.likes = q.likes + 1 WHERE q.uuid = :uuid")
    int incrementLikes(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE Quest q SET q.dislikes = q.dislikes + 1 WHERE q.uuid = :uuid")
    int incrementDislikes(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE Quest q SET q.likes = q.likes - 1 WHERE q.uuid = :uuid")
    int decrementLikes(@Param("uuid") UUID uuid);

    @Modifying
    @Query("UPDATE Quest q SET q.dislikes = q.dislikes - 1 WHERE q.uuid = :uuid")
    int decrementDislikes(@Param("uuid") UUID uuid);
}
