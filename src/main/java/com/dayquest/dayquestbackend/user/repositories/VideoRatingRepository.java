package com.dayquest.dayquestbackend.user.repositories;

import com.dayquest.dayquestbackend.user.ids.VideoRatingId;
import com.dayquest.dayquestbackend.user.models.User;
import com.dayquest.dayquestbackend.user.models.VideoRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRatingRepository extends JpaRepository<VideoRating, VideoRatingId> {
    List<VideoRating> findByUserAndLikedTrue(User user);

    List<VideoRating> findByUserAndLikedFalse(User user);
}
