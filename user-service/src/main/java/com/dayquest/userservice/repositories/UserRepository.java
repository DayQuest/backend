package com.dayquest.userservice.repositories;

import com.dayquest.userservice.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.authorities WHERE u.username = :username")
    User findByUsername(@Param("username") String username);


    Optional<User> findByPasswordResetToken(String passwordResetToken);

    User findByEmail(String email);

    Page<User> findUsersByUsernameContainingIgnoreCase(String query, Pageable pageable);

    Optional<User> findByEmailIgnoreCase(String email);

    User findByVerificationCode(String verificationCode);
}
