package com.dayquest.contentservice.service;

import com.dayquest.contentservice.model.Badge;
import com.dayquest.contentservice.repository.BadgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BadgeService {

    private final BadgeRepository badgeRepository;

    public BadgeService(BadgeRepository badgeRepository) {
        this.badgeRepository = badgeRepository;
    }

    @Transactional
    public Badge createBadge(String name, String description, String iconUrl) {
        Badge badge = new Badge();
        badge.setName(name);
        badge.setDescription(description);
        badge.setIconUrl(iconUrl);
        return badgeRepository.save(badge);
    }

    public List<Badge> getAllBadges() {
        return badgeRepository.findAll();
    }

    public Optional<Badge> getBadge(UUID badgeId) {
        return badgeRepository.findById(badgeId);
    }

    public Optional<Badge> getBadgeByName(String name) {
        return badgeRepository.findByName(name);
    }

    public List<Badge> getUserBadges(UUID userUuid) {
        return badgeRepository.findByUserUuid(userUuid);
    }

    @Transactional
    public boolean awardBadge(UUID badgeId, UUID userUuid) {
        Optional<Badge> badgeOpt = badgeRepository.findById(badgeId);
        if (badgeOpt.isEmpty()) {
            return false;
        }

        Badge badge = badgeOpt.get();
        badge.addUser(userUuid);
        badgeRepository.save(badge);
        return true;
    }

    @Transactional
    public boolean awardBadgeByName(String badgeName, UUID userUuid) {
        Optional<Badge> badgeOpt = badgeRepository.findByName(badgeName);
        if (badgeOpt.isEmpty()) {
            return false;
        }

        Badge badge = badgeOpt.get();
        badge.addUser(userUuid);
        badgeRepository.save(badge);
        return true;
    }
}

