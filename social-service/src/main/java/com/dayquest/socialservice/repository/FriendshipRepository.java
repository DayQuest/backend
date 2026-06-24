package com.dayquest.socialservice.repository;

import com.dayquest.socialservice.model.Friendship;
import com.dayquest.socialservice.model.FriendshipStatus;
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
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.userUuid = :userUuid OR f.friendUuid = :userUuid) AND f.status = :status")
    Page<Friendship> findFriendsByUserUuidAndStatus(
            @Param("userUuid") UUID userUuid,
            @Param("status") FriendshipStatus status,
            Pageable pageable);

    @Query("SELECT f FROM Friendship f WHERE " +
           "((f.userUuid = :user1 AND f.friendUuid = :user2) OR " +
           "(f.userUuid = :user2 AND f.friendUuid = :user1))")
    Optional<Friendship> findFriendshipBetween(
            @Param("user1") UUID user1,
            @Param("user2") UUID user2);

    Page<Friendship> findByFriendUuidAndStatus(UUID friendUuid, FriendshipStatus status, Pageable pageable);

    @Query("SELECT COUNT(f) FROM Friendship f WHERE " +
           "(f.userUuid = :userUuid OR f.friendUuid = :userUuid) AND f.status = 'ACCEPTED'")
    long countFriendsByUserUuid(@Param("userUuid") UUID userUuid);
}

