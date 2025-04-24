package com.dayquest.dayquestbackend.badge;

import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class BadgeService {
    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private UserRepository userRepository;

    @Async
    public CompletableFuture<ResponseEntity<String>> createBadge(String name, String description, MultipartFile file) {
        return CompletableFuture.supplyAsync(() -> {
            Badge badge = new Badge();
            badge.setName(name);
            badge.setDescription(description);
            try {
                badge.setImage(file.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            badgeRepository.save(badge);
            return ResponseEntity.ok("Badge created successfully");
        });
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> awardBadgeToUser(UUID badgeId, UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Badge> badgeOpt = badgeRepository.findById(badgeId);
            Optional<User> userOpt = userRepository.findById(userId);

            if (badgeOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Badge badge = badgeOpt.get();
            User user = userOpt.get();

            // Add user to badge's user list
            List<UUID> userIds = badge.getUserIds();
            if (userIds == null) {
                userIds = new ArrayList<>();
            }
            if (!userIds.contains(userId)) {
                userIds.add(userId);
                badge.setUserIds(userIds);
                badgeRepository.save(badge);

                // Add badge to user's badge list
                List<UUID> userBadges = user.getBadges();
                if (userBadges == null) {
                    userBadges = new ArrayList<>();
                }
                userBadges.add(badgeId);
                user.setBadges(userBadges);
                userRepository.save(user);

                return ResponseEntity.ok("Badge awarded successfully");
            }

            return ResponseEntity.ok("User already has this badge");
        });
    }

    public Badge saveBadge(Badge badge) {
        return badgeRepository.save(badge);
    }
}
