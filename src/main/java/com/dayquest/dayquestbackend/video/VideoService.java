package com.dayquest.dayquestbackend.video;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.dayquest.dayquestbackend.storage.service.VideoStorageService;
import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.video.models.Video;
import com.dayquest.dayquestbackend.video.repository.VideoRepository;
import com.dayquest.dayquestbackend.video.repository.ViewedVideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoService {
    private static final Logger logger = Logger.getLogger(VideoService.class.getName());

    private final VideoRepository videoRepository;
    private final ViewedVideoRepository viewedVideoRepository;

    @Autowired
    private VideoStorageService videoStorageService;

    @Autowired
    public VideoService(ViewedVideoRepository viewedVideoRepository, VideoRepository videoRepository,
                       PlatformTransactionManager transactionManager) {
        this.videoRepository = videoRepository;
        this.viewedVideoRepository = viewedVideoRepository;
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Async
    public CompletableFuture<ResponseEntity<Video>> likeVideo(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<Video> video = videoRepository.findById(uuid);
                if (video.isEmpty()) {
                    logger.info("Video not found: " + uuid);
                    return ResponseEntity.notFound().build();
                }

                video.get().setUpVotes(video.get().getUpVotes() + 1);
                videoRepository.save(video.get());
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error liking video: " + uuid, e);
                return ResponseEntity.internalServerError().build();
            }
        });
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> uploadVideo(MultipartFile file, String title, String description, User user, List<String> hashtags) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                videoStorageService.uploadVideo(file, title, description, user, hashtags);
                return ResponseEntity.ok("Uploaded");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error uploading video", e);
                return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
            }
        });
    }

    @Async
    public CompletableFuture<ResponseEntity<Video>> dislikeVideo(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<Video> video = videoRepository.findById(uuid);
                if (video.isEmpty()) {
                    logger.info("Video not found: " + uuid);
                    return ResponseEntity.notFound().build();
                }

                video.get().setDownVotes(video.get().getDownVotes() + 1);
                videoRepository.save(video.get());
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error disliking video: " + uuid, e);
                return ResponseEntity.internalServerError().build();
            }
        });
    }

    @Async
    public CompletableFuture<CompletableFuture<ResponseEntity<String>>> deleteVideo(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return videoStorageService.deleteVideo(uuid);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error deleting video: " + uuid, e);
                return CompletableFuture.completedFuture(ResponseEntity.internalServerError().body("Deletion failed: " + e.getMessage()));
            }
        });
    }
}