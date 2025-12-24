package com.dayquest.user.service;

import com.dayquest.common.exception.BadRequestException;
import com.dayquest.user.model.Follow;
import com.dayquest.user.model.User;
import com.dayquest.user.repository.FollowRepository;
import com.dayquest.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    public boolean isFollowing(UUID followerId, UUID followedId) {
        return followRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }

    @Transactional
    public void followUser(UUID followerId, UUID followedId) {
        if (followerId.equals(followedId)) {
            throw new BadRequestException("Cannot follow yourself");
        }

        if (isFollowing(followerId, followedId)) {
            throw new BadRequestException("Already following this user");
        }

        Follow follow = new Follow(followerId, followedId);
        followRepository.save(follow);

        // Update follower counts
        userRepository.findById(followedId).ifPresent(user -> {
            user.setFollowers(user.getFollowers() + 1);
            userRepository.save(user);
        });

        userRepository.findById(followerId).ifPresent(user -> {
            user.setFollowing(user.getFollowing() + 1);
            userRepository.save(user);
        });
    }

    @Transactional
    public void unfollowUser(UUID followerId, UUID followedId) {
        if (!isFollowing(followerId, followedId)) {
            throw new BadRequestException("Not following this user");
        }

        followRepository.deleteByFollowerIdAndFollowedId(followerId, followedId);

        // Update follower counts
        userRepository.findById(followedId).ifPresent(user -> {
            user.setFollowers(Math.max(0, user.getFollowers() - 1));
            userRepository.save(user);
        });

        userRepository.findById(followerId).ifPresent(user -> {
            user.setFollowing(Math.max(0, user.getFollowing() - 1));
            userRepository.save(user);
        });
    }

    public Page<UUID> getFollowers(UUID userId, int page, int size) {
        return followRepository.findFollowersByFollowedId(userId, PageRequest.of(page, size));
    }

    public Page<UUID> getFollowing(UUID userId, int page, int size) {
        return followRepository.findFollowedByFollowerId(userId, PageRequest.of(page, size));
    }

    public long getFollowersCount(UUID userId) {
        return followRepository.countByFollowedId(userId);
    }

    public long getFollowingCount(UUID userId) {
        return followRepository.countByFollowerId(userId);
    }
}
