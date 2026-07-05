package com.dayquest.socialservice.service;

import com.dayquest.socialservice.model.Hashtag;
import com.dayquest.socialservice.repository.HashtagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class HashtagService {

    private final HashtagRepository hashtagRepository;

    public HashtagService(HashtagRepository hashtagRepository) {
        this.hashtagRepository = hashtagRepository;
    }

    @Transactional
    public Hashtag createOrGetHashtag(String name) {
        String normalizedName = normalizeHashtag(name);

        Optional<Hashtag> existingHashtag = hashtagRepository.findByName(normalizedName);
        if (existingHashtag.isPresent()) {
            Hashtag hashtag = existingHashtag.get();
            hashtag.incrementUsage();
            return hashtagRepository.save(hashtag);
        }

        Hashtag hashtag = new Hashtag();
        hashtag.setName(normalizedName);
        hashtag.setUsageCount(1);
        return hashtagRepository.save(hashtag);
    }

    @Transactional
    public List<Hashtag> processHashtags(List<String> hashtagNames) {
        List<Hashtag> hashtags = new ArrayList<>();
        for (String name : hashtagNames) {
            hashtags.add(createOrGetHashtag(name));
        }
        return hashtags;
    }

    public Page<Hashtag> getTrendingHashtags(Pageable pageable) {
        return hashtagRepository.findTrendingHashtags(pageable);
    }

    public Page<Hashtag> searchHashtags(String query, Pageable pageable) {
        return hashtagRepository.findByNameContainingIgnoreCase(query, pageable);
    }

    public Optional<Hashtag> getHashtag(String name) {
        return hashtagRepository.findByName(normalizeHashtag(name));
    }

    private String normalizeHashtag(String name) {
        if (name == null) {
            return "";
        }
        String normalized = name.toLowerCase().trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return normalized.replaceAll("[^a-z0-9]", "");
    }
}

