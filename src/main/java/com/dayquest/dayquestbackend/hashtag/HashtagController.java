package com.dayquest.dayquestbackend.hashtag;

import com.dayquest.dayquestbackend.video.dto.VideoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/hashtag")
public class HashtagController {
    private final HashtagService hashtagService;
    private final HashtagRepository hashtagRepository;

    public HashtagController(HashtagService hashtagService, HashtagRepository hashtagRepository) {
        this.hashtagService = hashtagService;
        this.hashtagRepository = hashtagRepository;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<String>> createHashtag(@RequestBody CreateHashtagDTO hashtagDTO) {
        return CompletableFuture.supplyAsync(() -> {
            if (hashtagDTO.getHashtag().length() > 20) {
                return ResponseEntity.badRequest().body("Hashtag is too long");
            }

            if (hashtagDTO.getHashtag().length() < 1) {
                return ResponseEntity.badRequest().body("Hashtag is too short");
            }

            if (hashtagDTO.getHashtag().contains(" ")) {
                return ResponseEntity.badRequest().body("Hashtag cannot contain spaces");
            }

            if (hashtagRepository.findByHashtag(hashtagDTO.getHashtag()) != null) {
                return ResponseEntity.badRequest().body("Hashtag already exists");
            }

            Hashtag hashtag = hashtagService.createHashtag(hashtagDTO.getHashtag()).join();
            return ResponseEntity.ok(hashtag.getHashtag());
        });
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<List<Hashtag>>> getHashtag(@RequestParam String query, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return CompletableFuture.supplyAsync(() -> {
            return ResponseEntity.ok(hashtagService.searchPaginatedHashtags(page, size, query).join());
        });
    }

    @GetMapping("/{hashtag}/videos")
    public CompletableFuture<ResponseEntity<List<VideoDTO>>> getVideosWithHashtag(@PathVariable String hashtag, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return CompletableFuture.supplyAsync(() -> {
            return ResponseEntity.ok(hashtagService.getPaginatedVideosWithHashtag(page, size, hashtag).join());
        });
    }
}
