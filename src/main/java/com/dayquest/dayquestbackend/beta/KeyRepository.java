package com.dayquest.dayquestbackend.beta;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KeyRepository extends JpaRepository<BetaKey, Long> {
    boolean existsByKey(String key);
}
