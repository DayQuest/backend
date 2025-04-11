package com.dayquest.dayquestbackend.video.repository;

import java.util.List;
import java.util.UUID;

import com.dayquest.dayquestbackend.video.models.Video;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface  VideoRepository extends JpaRepository<Video, UUID> {
    @Fetch(FetchMode.JOIN)
    Optional<Video> findById(UUID uuid);

    @Query(value = "SELECT * FROM video ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Video> findRandomVideo();

    @Query(value = "SELECT * " +
            "FROM video " +
            "WHERE uuid NOT IN (" +
            "  SELECT video_id " +
            "  FROM viewed_video " +
            "  WHERE user_id = :userId" +
            ")" +
            "ORDER BY RAND()" +
            "LIMIT 10;", nativeQuery = true)
    List<Video> findUnviewedVideosByUserId(@Param("userId") UUID userId);

    Page<Video> findVideosByHashtagsContainsIgnoreCase(String query, Pageable pageable);
}

