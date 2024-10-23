package com.dayquest.dayquestbackend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    User findByUsername(String username);
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.postedVideos WHERE u.username = :username")
    User findByUsernameWithVideos(@PathVariable String username);
}
