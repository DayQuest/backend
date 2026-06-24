package com.dayquest.videoservice.listener;

import com.dayquest.common.events.UserDeletedEvent;
import com.dayquest.common.messaging.RabbitMQConstants;
import com.dayquest.videoservice.model.Video;
import com.dayquest.videoservice.model.VideoStatus;
import com.dayquest.videoservice.repository.VideoRepository;
import com.dayquest.videoservice.repository.ViewedVideoRepository;
import com.dayquest.videoservice.repository.VideoRatingRepository;
import com.dayquest.videoservice.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Listens for user deleted events and cleans up all user-related video data.
 */
@Component
public class UserDeletedEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserDeletedEventListener.class);

    private final VideoRepository videoRepository;
    private final ViewedVideoRepository viewedVideoRepository;
    private final VideoRatingRepository videoRatingRepository;
    private final StorageService storageService;

    public UserDeletedEventListener(VideoRepository videoRepository,
                                    ViewedVideoRepository viewedVideoRepository,
                                    VideoRatingRepository videoRatingRepository,
                                    StorageService storageService) {
        this.videoRepository = videoRepository;
        this.viewedVideoRepository = viewedVideoRepository;
        this.videoRatingRepository = videoRatingRepository;
        this.storageService = storageService;
    }

    @RabbitListener(queues = RabbitMQConstants.USER_DELETED_VIDEO_QUEUE)
    @Transactional
    public void handleUserDeletedEvent(UserDeletedEvent event) {
        logger.info("Received UserDeletedEvent for user: {} ({})", event.getUsername(), event.getUserId());

        try {
            // 1. Delete all videos uploaded by this user
            List<Video> userVideos = videoRepository.findByUserUuidAndStatusAndDeletedFalse(
                    event.getUserId(), VideoStatus.ACTIVE);

            for (Video video : userVideos) {
                // Delete video and thumbnail files from storage
                if (video.getFilePath() != null) {
                    storageService.deleteVideo(video.getFilePath());
                }
                if (video.getThumbnailPath() != null) {
                    storageService.deleteThumbnail(video.getThumbnailPath());
                }
                // Mark video as deleted
                video.setDeleted(true);
                video.setStatus(VideoStatus.DELETED);
                videoRepository.save(video);
            }
            logger.info("Deleted {} videos for user: {}", userVideos.size(), event.getUserId());

            // 2. Delete all video ratings (likes/dislikes) by this user
            videoRatingRepository.deleteByUserUuid(event.getUserId());
            logger.info("Deleted video ratings for user: {}", event.getUserId());

            // 3. Delete viewed video records for this user
            viewedVideoRepository.deleteByUserUuid(event.getUserId());
            logger.info("Deleted viewed video records for user: {}", event.getUserId());

            logger.info("Successfully cleaned up all video data for user: {} ({})",
                    event.getUsername(), event.getUserId());

        } catch (Exception e) {
            logger.error("Error cleaning up video data for user: {} - {}", event.getUserId(), e.getMessage(), e);
            throw e; // Re-throw to trigger retry/DLQ
        }
    }
}


