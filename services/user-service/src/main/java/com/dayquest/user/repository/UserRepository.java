package com.dayquest.user.repository;

import com.dayquest.user.model.User;
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

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Page<User> findByUsernameContainingIgnoreCase(String query, Pageable pageable);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u.uuid FROM User u WHERE u.username = :username")
    Optional<UUID> findUuidByUsername(@Param("username") String username);
}
