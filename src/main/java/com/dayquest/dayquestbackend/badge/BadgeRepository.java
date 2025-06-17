package com.dayquest.dayquestbackend.badge;

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
    @Query("SELECT b.id FROM Badge b WHERE :uuid MEMBER OF b.userIds")
    List<UUID> findBadgeIdsByUserId(@Param("uuid") UUID uuid);
}
