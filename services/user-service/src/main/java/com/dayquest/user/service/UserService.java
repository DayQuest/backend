package com.dayquest.user.service;

import com.dayquest.common.exception.BadRequestException;
import com.dayquest.common.exception.ResourceNotFoundException;
import com.dayquest.user.dto.CreateUserDTO;
import com.dayquest.user.dto.ProfileDTO;
import com.dayquest.user.dto.UpdateUserDTO;
import com.dayquest.user.model.User;
import com.dayquest.user.model.UserStatus;
import com.dayquest.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String DEFAULT_PROFILE_PICTURE_URL = "https://static.vecteezy.com/system/resources/thumbnails/003/337/584/small/default-avatar-photo-placeholder-profile-icon-vector.jpg";
    private static final String PROFILE_PICTURE_BASE_URL = "https://api.dayquest.de/api/users/profilepicture/";

    private final UserRepository userRepository;
    private final FollowService followService;

    public UserService(UserRepository userRepository, FollowService followService) {
        this.userRepository = userRepository;
        this.followService = followService;
    }

    @Cacheable(value = "users", key = "#uuid")
    public User getUserById(UUID uuid) {
        return userRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + uuid));
    }

    @Cacheable(value = "users", key = "#username")
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    public ProfileDTO getProfileById(UUID uuid, UUID requesterId) {
        User user = getUserById(uuid);
        boolean isFollowing = requesterId != null && followService.isFollowing(requesterId, uuid);
        return createProfileDTO(user, isFollowing);
    }

    public ProfileDTO getProfileByUsername(String username, UUID requesterId) {
        User user = getUserByUsername(username);
        boolean isFollowing = requesterId != null && followService.isFollowing(requesterId, user.getUuid());
        return createProfileDTO(user, isFollowing);
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public User createUser(CreateUserDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = new User(dto.getUsername(), dto.getEmail());
        user.setEnabled(false);
        user.setLeftRerolls(3);
        return userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#uuid")
    public User updateUser(UUID uuid, UpdateUserDTO dto) {
        User user = getUserById(uuid);

        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new BadRequestException("Username already exists");
            }
            user.setUsername(dto.getUsername());
        }

        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new BadRequestException("Email already exists");
            }
            user.setEmail(dto.getEmail());
        }

        return userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#uuid")
    public void deleteUser(UUID uuid) {
        if (!userRepository.existsById(uuid)) {
            throw new ResourceNotFoundException("User not found with id: " + uuid);
        }
        userRepository.deleteById(uuid);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#uuid")
    public User updateProfilePicture(UUID uuid, byte[] imageBytes) {
        User user = getUserById(uuid);
        user.setProfilePicture(imageBytes);
        return userRepository.save(user);
    }

    public byte[] getProfilePicture(String username) {
        User user = getUserByUsername(username);
        return user.getProfilePicture();
    }

    public Page<ProfileDTO> searchUsers(String query, int page, int size, UUID requesterId) {
        Page<User> users = userRepository.findByUsernameContainingIgnoreCase(query, PageRequest.of(page, size));
        return users.map(user -> {
            boolean isFollowing = requesterId != null && followService.isFollowing(requesterId, user.getUuid());
            return createProfileDTO(user, isFollowing);
        });
    }

    @Transactional
    @CacheEvict(value = "users", key = "#uuid")
    public void updateLastLogin(UUID uuid) {
        User user = getUserById(uuid);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#uuid")
    public void setBanStatus(UUID uuid, UserStatus status, String adminComment) {
        User user = getUserById(uuid);
        user.setStatus(status);
        user.setAdminComment(adminComment);
        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#uuid")
    public void addBadge(UUID uuid, UUID badgeId) {
        User user = getUserById(uuid);
        if (!user.getBadges().contains(badgeId)) {
            user.getBadges().add(badgeId);
            userRepository.save(user);
        }
    }

    @Transactional
    @CacheEvict(value = "users", key = "#uuid")
    public void removeBadge(UUID uuid, UUID badgeId) {
        User user = getUserById(uuid);
        user.getBadges().remove(badgeId);
        userRepository.save(user);
    }

    public List<UUID> getUserBadges(UUID uuid) {
        User user = getUserById(uuid);
        return user.getBadges();
    }

    @Transactional
    @CacheEvict(value = "users", key = "#uuid")
    public void setDailyQuest(UUID uuid, UUID questId) {
        User user = getUserById(uuid);
        if (user.getDailyQuestId() != null) {
            user.addDoneQuest(user.getDailyQuestId());
        }
        user.setDailyQuestId(questId);
        userRepository.save(user);
    }

    @Transactional
    public int getRerolls(UUID uuid) {
        User user = getUserById(uuid);
        if (user.getLastReroll() == null || user.getLastReroll().plusDays(1).isBefore(LocalDateTime.now())) {
            return 3;
        }
        return user.getLeftRerolls();
    }

    @Transactional
    @CacheEvict(value = "users", key = "#uuid")
    public void decrementRerolls(UUID uuid) {
        User user = getUserById(uuid);
        if (user.getLastReroll() == null || user.getLastReroll().plusDays(1).isBefore(LocalDateTime.now())) {
            user.setLeftRerolls(2);
        } else {
            user.setLeftRerolls(user.getLeftRerolls() - 1);
        }
        user.setLastReroll(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#uuid")
    public void enableUser(UUID uuid) {
        User user = getUserById(uuid);
        user.setEnabled(true);
        userRepository.save(user);
    }

    public void increaseInteractions(UUID uuid) {
        User user = getUserById(uuid);
        user.increaseInteractions();
        userRepository.save(user);
    }

    private ProfileDTO createProfileDTO(User user, boolean isFollowing) {
        String profilePictureUrl = user.getProfilePicture() != null 
                ? PROFILE_PICTURE_BASE_URL + user.getUsername() 
                : DEFAULT_PROFILE_PICTURE_URL;

        return new ProfileDTO(
                user.getUuid(),
                user.getUsername(),
                profilePictureUrl,
                user.getFollowers(),
                user.getFollowing(),
                isFollowing,
                user.getBadges(),
                user.getDailyQuestId(),
                user.getStatus() == UserStatus.BANNED
        );
    }
}
