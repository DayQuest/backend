package com.dayquest.dayquestbackend.video;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface  VideoRepository extends JpaRepository<Video, UUID> {
    @Query(value = "SELECT * FROM video ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Video> findRandomVideo();
}

