package com.dayquest.contentservice.repository;

import com.dayquest.contentservice.model.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, UUID> {

    Optional<Badge> findByName(String name);

    @Query("SELECT b FROM Badge b WHERE :userUuid MEMBER OF b.userUuids")
    List<Badge> findByUserUuid(@Param("userUuid") UUID userUuid);
}

