package com.example.dayquest.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.dayquest.model.Video;

public interface VideoRepository extends JpaRepository<Video, Long> {
}

