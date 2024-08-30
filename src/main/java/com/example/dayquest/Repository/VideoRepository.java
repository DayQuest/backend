package com.example.dayquest.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.dayquest.model.Video;

import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {
    Optional<Video> findByFilePath(String filePath);
}

