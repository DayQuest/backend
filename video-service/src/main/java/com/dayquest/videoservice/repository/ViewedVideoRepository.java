package com.dayquest.videoservice.repository;

import com.dayquest.videoservice.model.ViewedVideo;
import com.dayquest.videoservice.model.ViewedVideoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ViewedVideoRepository extends JpaRepository<ViewedVideo, ViewedVideoId> {

    boolean existsByUserUuidAndVideoUuid(UUID userUuid, UUID videoUuid);

    void deleteByUserUuid(UUID userUuid);
}

