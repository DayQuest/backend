package com.dayquest.videoservice.repository;

import com.dayquest.videoservice.model.Video;
import com.dayquest.videoservice.model.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID>, JpaSpecificationExecutor<Video> {

    Page<Video> findByStatusAndDeletedFalse(VideoStatus status, Pageable pageable);

    Page<Video> findByUserUuidAndDeletedFalse(UUID userUuid, Pageable pageable);

    Page<Video> findByQuestUuidAndDeletedFalse(UUID questUuid, Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.status = 'ACTIVE' AND v.deleted = false AND v.uuid NOT IN " +
           "(SELECT vv.videoUuid FROM ViewedVideo vv WHERE vv.userUuid = :userUuid) " +
           "ORDER BY RANDOM()")
    List<Video> findUnviewedVideosByUserUuid(@Param("userUuid") UUID userUuid, Pageable pageable);

    @Query(value = "SELECT * FROM videos WHERE status = 'ACTIVE' AND deleted = false ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Video> findRandomVideo();

    @Query("SELECT v FROM Video v WHERE v.status = 'ACTIVE' AND v.deleted = false ORDER BY v.createdAt DESC")
    Page<Video> findLatestVideos(Pageable pageable);

    @Query("SELECT v FROM Video v WHERE v.status = 'ACTIVE' AND v.deleted = false ORDER BY v.score DESC")
    Page<Video> findTrendingVideos(Pageable pageable);

    List<Video> findByUserUuidAndStatusAndDeletedFalse(UUID userUuid, VideoStatus status);

    long countByUserUuidAndDeletedFalse(UUID userUuid);

    // Atomic counter updates to prevent race conditions
    @Modifying
    @Query("UPDATE Video v SET v.views = v.views + 1 WHERE v.uuid = :id")
    int incrementViews(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Video v SET v.upVotes = v.upVotes + 1, v.score = v.upVotes + 1 - v.downVotes WHERE v.uuid = :id")
    int incrementUpVotes(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Video v SET v.upVotes = v.upVotes - 1, v.score = v.upVotes - 1 - v.downVotes WHERE v.uuid = :id AND v.upVotes > 0")
    int decrementUpVotes(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Video v SET v.downVotes = v.downVotes + 1, v.score = v.upVotes - v.downVotes - 1 WHERE v.uuid = :id")
    int incrementDownVotes(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Video v SET v.downVotes = v.downVotes - 1, v.score = v.upVotes - v.downVotes + 1 WHERE v.uuid = :id AND v.downVotes > 0")
    int decrementDownVotes(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Video v SET v.comments = v.comments + 1 WHERE v.uuid = :id")
    int incrementCommentCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Video v SET v.comments = v.comments - 1 WHERE v.uuid = :id AND v.comments > 0")
    int decrementCommentCount(@Param("id") UUID id);

    // Soft delete
    @Modifying
    @Query("UPDATE Video v SET v.deleted = true, v.deletedAt = CURRENT_TIMESTAMP WHERE v.uuid = :id")
    int softDelete(@Param("id") UUID id);

    // For backward compatibility
    default Page<Video> findByStatus(VideoStatus status, Pageable pageable) {
        return findByStatusAndDeletedFalse(status, pageable);
    }

    default Page<Video> findByUserUuid(UUID userUuid, Pageable pageable) {
        return findByUserUuidAndDeletedFalse(userUuid, pageable);
    }

    default Page<Video> findByQuestUuid(UUID questUuid, Pageable pageable) {
        return findByQuestUuidAndDeletedFalse(questUuid, pageable);
    }

    default long countByUserUuid(UUID userUuid) {
        return countByUserUuidAndDeletedFalse(userUuid);
    }
}

