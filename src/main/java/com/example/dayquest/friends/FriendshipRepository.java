package com.example.dayquest.friends;

import com.example.dayquest.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    Friendship findByUserAndFriend(User user, User friend);
    List<Friendship> findByUserAndStatus(User user, String status);
    List<Friendship> findByFriendAndStatus(User friend, String status);
}
