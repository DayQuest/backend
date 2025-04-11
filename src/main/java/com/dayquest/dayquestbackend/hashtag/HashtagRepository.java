package com.dayquest.dayquestbackend.hashtag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HashtagRepository extends JpaRepository<Hashtag, UUID> {
    Hashtag findByHashtag(String hashtag);

    Page<Hashtag> findHashtagsByHashtagContainingIgnoreCase(String query, Pageable pageable);
}
