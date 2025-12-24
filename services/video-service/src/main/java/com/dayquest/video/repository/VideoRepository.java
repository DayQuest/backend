package com.dayquest.video.repository;

import com.dayquest.video.model.Video;
import com.dayquest.video.model.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    Page<Video> findByStatusAndSecurityLevelNot(VideoStatus status, 
            com.dayquest.video.model.SecurityLevel securityLevel, Pageable pageable);

    Page<Video> findByUserUuid(UUID userUuid, Pageable pageable);

    Page<Video> findByQuestUuid(UUID questUuid, Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.status = :status ORDER BY v.createdAt DESC")
    List<Video> findRecentVideos(@Param("status") VideoStatus status, Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.status = :status ORDER BY (v.upVotes - v.downVotes) DESC")
    List<Video> findTopRatedVideos(@Param("status") VideoStatus status, Pageable pageable);

    Page<Video> findByTitleContainingIgnoreCaseAndStatus(String title, VideoStatus status, Pageable pageable);

    long countByUserUuid(UUID userUuid);

    long countByQuestUuid(UUID questUuid);
}
