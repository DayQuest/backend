package com.dayquest.dayquestbackend.badge;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/badge")
public class BadgeController {
    private final BadgeRepository badgeRepository;
    private final BadgeService badgeService;

    public BadgeController(BadgeRepository badgeRepository, BadgeService badgeService) {
        this.badgeRepository = badgeRepository;
        this.badgeService = badgeService;
    }

    @PostMapping("/create")
    @Async
    public CompletableFuture<Object> createBadge(@RequestParam("name") String name, @RequestParam("description") String description, @RequestParam("file")MultipartFile file, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            if (name == null || description == null || file == null) {
                return ResponseEntity.badRequest().body("Missing parameters");
            }
            if (name.length() < 3 || name.length() > 20) {
                return ResponseEntity.badRequest().body("Name must be between 3 and 20 characters");
            }
            if (description.length() < 3 || description.length() > 100) {
                return ResponseEntity.badRequest().body("Description must be between 3 and 100 characters");
            }
            if(!file.getContentType().equals("image/png") && !file.getContentType().equals("image/jpeg")) {
                return ResponseEntity.badRequest().body("File must be an image");
            }
            if(badgeRepository.findByName(name).isPresent()) {
                return ResponseEntity.badRequest().body("Badge with this name already exists");
            }
            return badgeService.createBadge(name, description, file);
        });
    }

    @GetMapping("/list")
    @Async
    public CompletableFuture<List<Badge>> getPagedBadges(@RequestParam("page") int page, @RequestParam("size") int size) {
        return CompletableFuture.supplyAsync(() -> badgeRepository.findAll(PageRequest.of(page, size)).getContent());
    }
}
