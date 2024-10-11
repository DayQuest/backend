package com.example.dayquest.beta;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KeyRepository extends JpaRepository<BetaKey, Long> {
    boolean existsByKey(String key);
}
