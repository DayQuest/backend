package com.dayquest.dayquestbackend.streak;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StreakRepository extends JpaRepository<Streak, UUID> {
    Streak findByUserId(UUID userId);
}
