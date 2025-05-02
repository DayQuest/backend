package com.dayquest.dayquestbackend.user.repositories;

import com.dayquest.dayquestbackend.user.ids.QuestRatingId;
import com.dayquest.dayquestbackend.user.models.QuestRating;
import com.dayquest.dayquestbackend.user.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestRatingRepository extends JpaRepository<QuestRating, QuestRatingId> {
    List<QuestRating> findByUserAndLikedTrue(User user);

    List<QuestRating> findByUserAndLikedFalse(User user);
}
