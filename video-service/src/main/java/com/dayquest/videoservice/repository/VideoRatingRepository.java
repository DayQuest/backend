package com.dayquest.videoservice.repository;

import com.dayquest.videoservice.model.VideoRating;
import com.dayquest.videoservice.model.VideoRatingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoRatingRepository extends JpaRepository<VideoRating, VideoRatingId> {

    Optional<VideoRating> findByUserUuidAndVideoUuid(UUID userUuid, UUID videoUuid);

    void deleteByUserUuidAndVideoUuid(UUID userUuid, UUID videoUuid);

    void deleteByVideoUuid(UUID videoUuid);

    void deleteByUserUuid(UUID userUuid);

    long countByVideoUuidAndIsUpvote(UUID videoUuid, boolean isUpvote);
}