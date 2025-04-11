package com.dayquest.dayquestbackend.beta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeyRepository extends JpaRepository<BetaKey, Long> {
    boolean existsByKey(String betaKey);

    BetaKey findByKey(String betaKey);

    BetaKey findByAppUsername(String username);
}
