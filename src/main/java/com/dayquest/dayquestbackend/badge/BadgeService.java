package com.dayquest.dayquestbackend.badge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Service
public class BadgeService {
    @Autowired
    private BadgeRepository badgeRepository;

    @Async
    public CompletableFuture<ResponseEntity<String>> createBadge(String name, String description, MultipartFile file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Badge badge = new Badge();
                badge.setName(name);
                badge.setDescription(description);
                badge.setImage(file.getBytes());
                if (badge.getUserIds() == null) {
                    badge.setUserIds(new ArrayList<>());
                }
                badgeRepository.save(badge);
                return ResponseEntity.ok("Badge created successfully");
            } catch (Exception e) {
                throw new RuntimeException("Failed to create badge", e);
            }
        });
    }
}
