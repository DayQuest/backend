package com.dayquest.dayquestbackend.video;

import java.util.UUID;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface  VideoRepository extends JpaRepository<Video, UUID> {
    @Fetch(FetchMode.JOIN)
    Optional<Video> findById(UUID uuid);

    @Query(value = "SELECT * FROM video ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Video> findRandomVideo();
}

