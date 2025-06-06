package com.dayquest.dayquestbackend.badge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/badge")
public class BadgeController {
    private final BadgeRepository badgeRepository;
    private final BadgeService badgeService;

    @Value("${api.secret}")
    private String apiSecret;

    public BadgeController(BadgeRepository badgeRepository, BadgeService badgeService) {
        this.badgeRepository = badgeRepository;
        this.badgeService = badgeService;
    }

    @PostMapping
    @Async
    public CompletableFuture<Object> createBadge(@RequestParam("name") String name, @RequestParam("description") String description, @RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String secret) {
        return CompletableFuture.supplyAsync(() -> {
            if (name == null || description == null || file == null) {
                return ResponseEntity.badRequest().body("Missing parameters");
            }
            if (secret == null || !secret.equals(apiSecret)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }
            if (name.length() < 3 || name.length() > 20) {
                return ResponseEntity.unprocessableEntity().body("Name must be between 3 and 20 characters");
            }
            if (description.length() < 3 || description.length() > 100) {
                return ResponseEntity.unprocessableEntity().body("Description must be between 3 and 100 characters");
            }
            if (!file.getContentType().equals("image/png") && !file.getContentType().equals("image/jpeg")) {
                return ResponseEntity.unprocessableEntity().body("File must be an image");
            }
            if (badgeRepository.findByName(name).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Badge with this name already exists");
            }
            return badgeService.createBadge(name, description, file);
        });
    }

    @DeleteMapping
    @Async
    public CompletableFuture<Object> deleteBadge(@RequestParam("id") UUID id, @RequestHeader("Authorization") String secret) {
        return CompletableFuture.supplyAsync(() -> {
            if (secret == null || !secret.equals(apiSecret)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }
            if (id == null) {
                return ResponseEntity.badRequest().body("Missing parameters");
            }
            if (badgeRepository.findById(id).isEmpty()) {
                return ResponseEntity.notFound();
            }
            badgeRepository.deleteById(id);
            return ResponseEntity.ok().build();
        });
    }

    @GetMapping("/list")
    @Async
    public CompletableFuture<List<Badge>> getPagedBadges(@RequestParam("page") int page, @RequestParam("size") int size) {
        return CompletableFuture.supplyAsync(() -> badgeRepository.findAll(PageRequest.of(page, size)).getContent());
    }

    @GetMapping("/{uuid}/users")
    @Async
    public CompletableFuture<List<UUID>> getUsersWithBadge(@PathVariable UUID uuid) {
        return CompletableFuture.supplyAsync(() -> badgeRepository.findById(uuid).map(Badge::getUserIds).orElse(null));
    }

    @GetMapping("/{uuid}")
    @Async
    public CompletableFuture<ResponseEntity<ByteArrayResource>> getBadge(@PathVariable UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Badge badge = badgeRepository.findById(uuid).orElse(null);
            if (badge == null) {
                return ResponseEntity.notFound().build();
            }
            byte[] imageData = badge.getImage();
            if (imageData == null || imageData.length == 0) {
                return ResponseEntity.notFound().build();
            }
            ByteArrayResource resource = new ByteArrayResource(imageData) {
                @Override
                public String getFilename() {
                    return badge.getName() + ".png";
                }
            };
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentDispositionFormData("inline", resource.getFilename());


            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        });
    }
}
