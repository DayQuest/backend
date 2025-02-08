package com.dayquest.dayquestbackend.video;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import com.dayquest.dayquestbackend.storage.Service.VideoStorageService;
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

    private final VideoRepository videoRepository;

    @Autowired
    private VideoStorageService videoStorageService;

    @Autowired
    public VideoService(ViewedVideoRepository viewedVideoRepository, VideoRepository videoRepository,
                        PlatformTransactionManager transactionManager) {
        this.videoRepository = videoRepository;
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Async
    public CompletableFuture<ResponseEntity<Video>> likeVideo(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Video> video = videoRepository.findById(uuid);
            if (video.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            video.get().setUpVotes(video.get().getUpVotes() + 1);
            videoRepository.save(video.get());
            return ResponseEntity.ok().build();
        });
    }

    @Async
    public CompletableFuture<ResponseEntity<String>> uploadVideo(MultipartFile file, String title, String description, User user) {
        return CompletableFuture.supplyAsync(() -> {
            videoStorageService.uploadVideo(file, title, description, user);
            return ResponseEntity.ok("Uploaded");
        });
    }


    @Async
    public CompletableFuture<ResponseEntity<Video>> dislikeVideo(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Video> video = videoRepository.findById(uuid);
            if (video.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            video.get().setDownVotes(video.get().getDownVotes() + 1);
            videoRepository.save(video.get());
            return ResponseEntity.ok().build();
        });
    }

    @Async
    public CompletableFuture<CompletableFuture<ResponseEntity<String>>> deleteVideo(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            return videoStorageService.deleteVideo(uuid);
        });
    }
}