package com.dayquest.dayquestbackend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.authorities WHERE u.username = :username")
    User findByUsername(@Param("username") String username);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.postedVideos v LEFT JOIN FETCH v.user WHERE u.username = :username")
    User findByUsernameWithVideos(@PathVariable String username);
    User findByEmail(String email);
    List<User> findAllByDailyQuestUuid(UUID dailyQuestUuid);
    Optional<User> findByEmailIgnoreCase(String email);
    User findByVerificationCode(String verificationCode);
}
