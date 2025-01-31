package com.dayquest.dayquestbackend.video;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import com.dayquest.dayquestbackend.streak.Streak;
import com.dayquest.dayquestbackend.streak.StreakRepository;
import com.dayquest.dayquestbackend.streak.StreakService;
import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.user.UserRepository;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.transaction.Transactional;
import org.apache.commons.io.FileUtils;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;

@Service
public class VideoService {
    @Value("${minio.rawVideosBucket}")
    private String bucket;

    @Value("${video.processed.path}")
    private String processPath;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private UserRepository userRepository;

    private final ViewedVideoRepository viewedVideoRepository;
    private final VideoRepository videoRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private StreakService streakService;

    @Autowired
    private StreakRepository streakRepository;

    private final TransactionTemplate transactionTemplate;

    @Autowired
    public VideoService(ViewedVideoRepository viewedVideoRepository, VideoRepository videoRepository,
                        PlatformTransactionManager transactionManager) {
        this.viewedVideoRepository = viewedVideoRepository;
        this.videoRepository = videoRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                return ResponseEntity.badRequest().body("Invalid file format.");
            }
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();

            String filePath = UUID.randomUUID().toString() + fileExtension;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(filePath)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build());

            Video video = new Video();
            video.setUuid(UUID.randomUUID());
            video.setUser(user);
            video.setCreatedAt(LocalDateTime.now());
            video.setTitle(title);
            video.setDescription(description);
            video.setFilePath(filePath);
            video.setStatus(Status.PENDING);
            videoRepository.save(video);

            return ResponseEntity.ok("Uploaded");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload video file: " + e.getMessage());
        }
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
    public CompletableFuture<ResponseEntity<String>> deleteVideo(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Video> video = videoRepository.findById(uuid);
            if (video.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(video.get().getFilePath())
                        .build());

                String thumbnailPath = video.get().getFilePath().replace(".mp4", "_thumb.jpg");
                try {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(thumbnailPath)
                            .build());
                } catch (Exception e) {
                }

                videoRepository.deleteById(uuid);
                return ResponseEntity.ok("Deleted");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to delete video file: " + e.getMessage());
            }
        });
    }
}