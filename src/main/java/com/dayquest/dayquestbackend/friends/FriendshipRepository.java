package com.dayquest.dayquestbackend.friends;

import com.dayquest.dayquestbackend.user.User;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    Friendship findByUserAndFriend(User user, User friend);

    List<Friendship> findByUserAndStatus(User user, FriendRequestStatus status);

    List<Friendship> findByUser(User user);
}
