package com.dayquest.dayquestbackend.user.repositories;

import com.dayquest.dayquestbackend.user.ids.QuestRatingId;
import com.dayquest.dayquestbackend.user.models.QuestRating;
import com.dayquest.dayquestbackend.user.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestRatingRepository extends JpaRepository<QuestRating, QuestRatingId> {


    @Query("SELECT qr.quest.uuid FROM QuestRating qr WHERE qr.user.uuid = :userId AND qr.liked = :liked")
    List<UUID> findQuestIdsByUserAndLiked(@Param("userId") UUID userId, @Param("liked") boolean liked);
}
