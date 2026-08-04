package com.dayquest.socialservice.repository;

import com.dayquest.socialservice.model.Hashtag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, UUID> {

    Optional<Hashtag> findByName(String name);

    @Query("SELECT h FROM Hashtag h ORDER BY h.usageCount DESC")
    Page<Hashtag> findTrendingHashtags(Pageable pageable);

    Page<Hashtag> findByNameContainingIgnoreCase(String name, Pageable pageable);
}

