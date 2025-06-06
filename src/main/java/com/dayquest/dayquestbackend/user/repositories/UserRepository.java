package com.dayquest.dayquestbackend.user.repositories;

import java.util.Optional;
import java.util.UUID;

import com.dayquest.dayquestbackend.user.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.authorities WHERE u.username = :username")
    User findByUsername(@Param("username") String username);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.postedVideos WHERE u.uuid = :uuid")
    User findByIdWithVideos(@Param("uuid") UUID uuid);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    User findByEmail(String email);

    Page<User> findUsersByUsernameContainingIgnoreCase(String query, Pageable pageable);

    Optional<User> findByEmailIgnoreCase(String email);

    User findByVerificationCode(String verificationCode);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.postedVideos WHERE u.username = :username")
    User findByUsernameWithVideos(String username);
}
