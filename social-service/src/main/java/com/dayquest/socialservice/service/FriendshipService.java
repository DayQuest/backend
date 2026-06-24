package com.dayquest.socialservice.service;

import com.dayquest.socialservice.dto.FriendshipDTO;
import com.dayquest.socialservice.model.Friendship;
import com.dayquest.socialservice.model.FriendshipStatus;
import com.dayquest.socialservice.repository.FriendshipRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;

    public FriendshipService(FriendshipRepository friendshipRepository) {
        this.friendshipRepository = friendshipRepository;
    }

    @Transactional
    public Friendship sendFriendRequest(UUID userUuid, UUID friendUuid) {
        Optional<Friendship> existingFriendship = friendshipRepository.findFriendshipBetween(userUuid, friendUuid);
        if (existingFriendship.isPresent()) {
            return existingFriendship.get();
        }

        Friendship friendship = new Friendship();
        friendship.setUserUuid(userUuid);
        friendship.setFriendUuid(friendUuid);
        friendship.setStatus(FriendshipStatus.PENDING);
        return friendshipRepository.save(friendship);
    }

    @Transactional
    public boolean acceptFriendRequest(UUID friendshipId, UUID userUuid) {
        Optional<Friendship> friendshipOpt = friendshipRepository.findById(friendshipId);
        if (friendshipOpt.isEmpty()) {
            return false;
        }

        Friendship friendship = friendshipOpt.get();
        if (!friendship.getFriendUuid().equals(userUuid)) {
            return false;
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
        return true;
    }

    @Transactional
    public boolean declineFriendRequest(UUID friendshipId, UUID userUuid) {
        Optional<Friendship> friendshipOpt = friendshipRepository.findById(friendshipId);
        if (friendshipOpt.isEmpty()) {
            return false;
        }

        Friendship friendship = friendshipOpt.get();
        if (!friendship.getFriendUuid().equals(userUuid)) {
            return false;
        }

        friendship.setStatus(FriendshipStatus.DECLINED);
        friendshipRepository.save(friendship);
        return true;
    }

    @Transactional
    public boolean removeFriend(UUID userUuid, UUID friendUuid) {
        Optional<Friendship> friendshipOpt = friendshipRepository.findFriendshipBetween(userUuid, friendUuid);
        if (friendshipOpt.isEmpty()) {
            return false;
        }

        friendshipRepository.delete(friendshipOpt.get());
        return true;
    }

    @Transactional
    public boolean blockUser(UUID userUuid, UUID blockedUuid) {
        Optional<Friendship> existingFriendship = friendshipRepository.findFriendshipBetween(userUuid, blockedUuid);

        Friendship friendship;
        if (existingFriendship.isPresent()) {
            friendship = existingFriendship.get();
        } else {
            friendship = new Friendship();
            friendship.setUserUuid(userUuid);
            friendship.setFriendUuid(blockedUuid);
        }

        friendship.setStatus(FriendshipStatus.BLOCKED);
        friendshipRepository.save(friendship);
        return true;
    }

    public Page<Friendship> getFriends(UUID userUuid, Pageable pageable) {
        return friendshipRepository.findFriendsByUserUuidAndStatus(userUuid, FriendshipStatus.ACCEPTED, pageable);
    }

    public Page<Friendship> getPendingRequests(UUID userUuid, Pageable pageable) {
        return friendshipRepository.findByFriendUuidAndStatus(userUuid, FriendshipStatus.PENDING, pageable);
    }

    public Optional<Friendship> getFriendshipStatus(UUID userUuid, UUID friendUuid) {
        return friendshipRepository.findFriendshipBetween(userUuid, friendUuid);
    }

    public long getFriendCount(UUID userUuid) {
        return friendshipRepository.countFriendsByUserUuid(userUuid);
    }

    public FriendshipDTO toDTO(Friendship friendship, UUID currentUserUuid) {
        FriendshipDTO dto = new FriendshipDTO();
        dto.setId(friendship.getId());
        dto.setUserUuid(friendship.getUserUuid());
        dto.setFriendUuid(friendship.getFriendUuid());
        dto.setStatus(friendship.getStatus());
        dto.setCreatedAt(friendship.getCreatedAt());

        if (friendship.getUserUuid().equals(currentUserUuid)) {
            dto.setOtherUserUuid(friendship.getFriendUuid());
        } else {
            dto.setOtherUserUuid(friendship.getUserUuid());
        }

        return dto;
    }
}

