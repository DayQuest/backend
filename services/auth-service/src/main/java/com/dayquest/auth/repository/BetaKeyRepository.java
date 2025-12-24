package com.dayquest.auth.repository;

import com.dayquest.auth.model.BetaKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BetaKeyRepository extends JpaRepository<BetaKey, UUID> {

    boolean existsByKey(String key);

    Optional<BetaKey> findByKey(String key);
}
