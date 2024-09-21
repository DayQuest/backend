package com.example.dayquest.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.dayquest.model.Video;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {
    Optional<Video> findByFilePath(String filePath);
    @Query(value = "SELECT * FROM video ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Video findRandomVideo();
}

