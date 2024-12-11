package com.dayquest.dayquestbackend.video;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ViewedVideoRepository extends JpaRepository<ViewedVideo, ViewedVideoId> {

    @Query("SELECT vv.id.videoId FROM ViewedVideo vv WHERE vv.id.userId = :userId")
    List<UUID> findVideoIdsByUserId(UUID userId);
    boolean existsById(ViewedVideoId id);
}
