package com.dayquest.quest.repository;

import com.dayquest.quest.model.Quest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestRepository extends JpaRepository<Quest, UUID> {

    Page<Quest> findByActiveTrue(Pageable pageable);

    @Query("SELECT q FROM Quest q WHERE q.active = true ORDER BY (q.likes - q.dislikes) DESC")
    List<Quest> findTopRatedQuests(Pageable pageable);

    @Query("SELECT q FROM Quest q WHERE q.active = true ORDER BY (q.likes - q.dislikes) DESC")
    List<Quest> findTop30PercentByRating();

    Page<Quest> findByCreatorUuid(UUID creatorUuid, Pageable pageable);

    Page<Quest> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    long countByActiveTrue();
}
