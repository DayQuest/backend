package com.dayquest.userservice.repositories;

import com.dayquest.userservice.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.authorities WHERE u.username = :username AND u.enabled = true")
    User findByUsername(@Param("username") String username);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    User findByEmail(String email);

    Page<User> findUsersByUsernameContainingIgnoreCaseAndEnabledTrue(String query, Pageable pageable);

    Optional<User> findByEmailIgnoreCase(String email);

    User findByVerificationCode(String verificationCode);

    // Batch fetch for feed generation - avoids N+1 queries
    @Query("SELECT u FROM User u WHERE u.uuid IN :ids AND u.enabled = true")
    List<User> findAllByIds(@Param("ids") Collection<UUID> ids);

    // Atomic follower count updates
    @Modifying
    @Query("UPDATE User u SET u.followers = u.followers + 1 WHERE u.uuid = :id")
    int incrementFollowerCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE User u SET u.followers = u.followers - 1 WHERE u.uuid = :id AND u.followers > 0")
    int decrementFollowerCount(@Param("id") UUID id);

    // Update last login
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = CURRENT_TIMESTAMP WHERE u.uuid = :id")
    int updateLastLogin(@Param("id") UUID id);

    // Increment interactions
    @Modifying
    @Query("UPDATE User u SET u.interactions = u.interactions + 1 WHERE u.uuid = :id")
    int incrementInteractions(@Param("id") UUID id);

    // For backward compatibility
    default Page<User> findUsersByUsernameContainingIgnoreCase(String query, Pageable pageable) {
        return findUsersByUsernameContainingIgnoreCaseAndEnabledTrue(query, pageable);
    }

    // Find users with profile pictures in DB (for migration to MinIO)
    @Query("SELECT u FROM User u WHERE u.profilePicture IS NOT NULL AND u.enabled = true")
    Page<User> findUsersWithProfilePicture(Pageable pageable);
}
