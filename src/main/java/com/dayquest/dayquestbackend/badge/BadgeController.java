package com.dayquest.dayquestbackend.badge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/badges")
@CrossOrigin(origins = "*")
public class BadgeController {
    private final BadgeRepository badgeRepository;
    private final BadgeService badgeService;

    @Autowired
    public BadgeController(BadgeRepository badgeRepository, BadgeService badgeService) {
        this.badgeRepository = badgeRepository;
        this.badgeService = badgeService;
    }

    @Value("${cdn.base.url}")
    private String cdnBaseUrl;

    @Value("${cdn.upload.path}")
    private String cdnUploadPath;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Async
    public CompletableFuture<ResponseEntity<Object>> createBadge(@RequestParam("name") String name, @RequestParam("description") String description, @RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
            if (name == null || description == null || file == null) {
                return ResponseEntity.badRequest().body("Missing parameters");
            }
            if (name.length() < 3 || name.length() > 20) {
                return ResponseEntity.unprocessableEntity().body("Name must be between 3 and 20 characters");
            }
            if (description.length() < 3 || description.length() > 100) {
                return ResponseEntity.unprocessableEntity().body("Description must be between 3 and 100 characters");
            }
            if (badgeRepository.findByName(name).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Badge with this name already exists");
            }
            try {
                // Generate UUID for the badge
                UUID badgeId = UUID.randomUUID();
                
                // Get file extension from original filename or content type
                String extension = getFileExtension(file);
                
                // Save image to CDN directory
                String fileName = badgeId.toString() + extension;
                Path uploadPath = Paths.get(cdnUploadPath, "badges", fileName);
                Files.createDirectories(uploadPath.getParent());
                Files.write(uploadPath, file.getBytes());

                // Create badge with CDN URL
                Badge badge = new Badge();
                badge.setId(badgeId);
                badge.setName(name);
                badge.setDescription(description);
                badge.setImageUrl(cdnBaseUrl + "/badges/" + fileName);
                badge.setUserIds(List.of());

                return ResponseEntity.ok(badgeService.saveBadge(badge));
            } catch (IOException e) {
                return ResponseEntity.internalServerError().build();
            }
        });
    }

    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // Fallback to content type
        String contentType = file.getContentType();
        if (contentType != null) {
            switch (contentType) {
                case "image/jpeg":
                    return ".jpg";
                case "image/png":
                    return ".png";
                default:
                    return ".jpg"; // default to jpg
            }
        }
        
        return ".jpg"; // final fallback
    }

    @DeleteMapping
    @Async
    public CompletableFuture<Object> deleteBadge(@RequestParam("id") UUID id, @RequestHeader("Authorization") String token) {
        return CompletableFuture.supplyAsync(() -> {
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
    public CompletableFuture<Object> getBadge(@PathVariable UUID uuid) {
        return CompletableFuture.supplyAsync(() -> badgeRepository.findById(uuid).orElse(null));
    }
}
