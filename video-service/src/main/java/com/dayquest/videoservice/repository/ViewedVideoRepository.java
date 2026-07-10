package com.dayquest.videoservice.repository;

import com.dayquest.videoservice.model.ViewedVideo;
import com.dayquest.videoservice.model.ViewedVideoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ViewedVideoRepository extends JpaRepository<ViewedVideo, ViewedVideoId> {

    boolean existsByUserUuidAndVideoUuid(UUID userUuid, UUID videoUuid);

    /** Removes all view records for a video — called during video deletion cascade. */
    void deleteByVideoUuid(UUID videoUuid);

    void deleteByUserUuid(UUID userUuid);
}