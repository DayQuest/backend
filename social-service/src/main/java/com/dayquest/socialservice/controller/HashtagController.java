package com.dayquest.socialservice.controller;

import com.dayquest.socialservice.model.Hashtag;
import com.dayquest.socialservice.service.HashtagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


//TODO: Rethink and rewrite hashtag system
@RestController
@RequestMapping("/hashtags")
@Tag(name = "Hashtags", description = "Hashtag management endpoints")
public class HashtagController {

    private final HashtagService hashtagService;

    public HashtagController(HashtagService hashtagService) {
        this.hashtagService = hashtagService;
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending hashtags")
    public ResponseEntity<Page<Hashtag>> getTrendingHashtags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(hashtagService.getTrendingHashtags(PageRequest.of(page, size)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search hashtags")
    public ResponseEntity<Page<Hashtag>> searchHashtags(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(hashtagService.searchHashtags(query, PageRequest.of(page, size)));
    }

    @GetMapping("/{name}")
    @Operation(summary = "Get hashtag by name")
    public ResponseEntity<Hashtag> getHashtag(@PathVariable String name) {
        return hashtagService.getHashtag(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create or get hashtags")
    public ResponseEntity<List<Hashtag>> createHashtags(@RequestBody List<String> names) {
        return ResponseEntity.ok(hashtagService.processHashtags(names));
    }
}

