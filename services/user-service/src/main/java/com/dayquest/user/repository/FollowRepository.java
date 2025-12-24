package com.dayquest.user.repository;

import com.dayquest.user.model.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    Optional<Follow> findByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    @Query("SELECT f.followedId FROM Follow f WHERE f.followerId = :userId")
    Page<UUID> findFollowedByFollowerId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT f.followerId FROM Follow f WHERE f.followedId = :userId")
    Page<UUID> findFollowersByFollowedId(@Param("userId") UUID userId, Pageable pageable);

    long countByFollowerId(UUID followerId);

    long countByFollowedId(UUID followedId);

    void deleteByFollowerIdAndFollowedId(UUID followerId, UUID followedId);
}
