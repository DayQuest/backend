package com.dayquest.dayquestbackend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    User findByUsername(String username);
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.postedVideos v LEFT JOIN FETCH v.user WHERE u.username = :username")
    User findByUsernameWithVideos(@PathVariable String username);
}
