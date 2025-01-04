package com.dayquest.dayquestbackend.badge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
public class BadgeService {
    @Autowired
    private BadgeRepository badgeRepository;

    @Async
    public CompletableFuture<ResponseEntity<String>> createBadge(String name, String description, MultipartFile file){
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
}
