package com.dayquest.dayquestbackend.user.repositories;

import com.dayquest.dayquestbackend.user.ids.VideoRatingId;
import com.dayquest.dayquestbackend.user.models.User;
import com.dayquest.dayquestbackend.user.models.VideoRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoRatingRepository extends JpaRepository<VideoRating, VideoRatingId> {
    @Query("SELECT vr.video.uuid FROM VideoRating vr WHERE vr.user.uuid = :userId AND vr.liked = :liked")
    List<UUID> findVideoIdsByUserAndLiked(@Param("userId") UUID userId, @Param("liked") boolean liked);
}
