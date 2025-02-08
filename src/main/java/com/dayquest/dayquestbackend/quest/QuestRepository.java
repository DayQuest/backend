package com.dayquest.dayquestbackend.quest;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestRepository extends JpaRepository<Quest, UUID> {
    Quest findByUuid(UUID uuid);
}
