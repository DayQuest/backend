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
    @Value("${minio.bucket}")
    private String bucket;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private VideoCompressor videoCompressor;

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

    public Page<Video> getAllVideos(Pageable pageable) {
        return videoRepository.findAll(pageable);
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
    public CompletableFuture<Video> getRandomVideo() {
        return CompletableFuture.supplyAsync(() -> {
            long count = videoRepository.count();
            if (count == 0) {
                return null;
            }
            return videoRepository.findRandomVideo()
                    .orElseThrow(() -> new RuntimeException("Failed to find random video"));
        });
    }

    @Async
    @Transactional
    public CompletableFuture<String> uploadVideo(MultipartFile file, String title, String description, User user) {
        return CompletableFuture.supplyAsync(() -> {
            String fileName = UUID.randomUUID().toString() + ".mp4";
            File tempFile = null;
            File processedTempFile = null;

            try {
                tempFile = File.createTempFile("unprocessed_", ".mp4");
                file.transferTo(tempFile);

                processedTempFile = File.createTempFile("processed_", ".mp4");
                videoCompressor.compressVideo(tempFile.getAbsolutePath(), processedTempFile.getAbsolutePath());

                String processedFileName = "videos/" + fileName;
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(processedFileName)
                        .stream(new FileInputStream(processedTempFile), processedTempFile.length(), -1)
                        .contentType("video/mp4")
                        .build());

                File finalProcessedTempFile = processedTempFile;
                return transactionTemplate.execute(status -> {
                    User managedUser = userRepository.findById(user.getUuid())
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    Video video = new Video();
                    video.setTitle(title);
                    video.setThumbnail(generateThumbnail(finalProcessedTempFile.getAbsolutePath()));
                    video.setDescription(description);
                    video.setFilePath(processedFileName);
                    video.setUser(managedUser);
                    video.setQuestUuid(managedUser.getDailyQuest().getUuid());
                    video.setCreatedAt(LocalDateTime.now());

                    managedUser.addPostedVideo(video);
                    userRepository.save(managedUser);

                    Streak streak = streakRepository.findByUserId(managedUser.getUuid());
                    if (streak == null) {
                        streakService.createStreak(managedUser.getUuid());
                    } else {
                        streakService.updateStreak(managedUser.getUuid());
                    }

                    return processedFileName;
                });

            } catch (Exception e) {
                throw new RuntimeException("Failed to process video: " + e.getMessage(), e);
            } finally {
                if (tempFile != null) {
                    tempFile.delete();
                }
                if (processedTempFile != null) {
                    processedTempFile.delete();
                }
            }
        });
    }

    public CompletableFuture<List<Video>> getUnviewedVideos(UUID userId) {
        return CompletableFuture.supplyAsync(() -> videoRepository.findUnviewedVideosByUserId(userId));
    }

    public void markVideoAsViewed(UUID userId, UUID videoId) {
        viewedVideoRepository.save(new ViewedVideo(new ViewedVideoId(userId, videoId)));
    }

    private byte[] generateThumbnail(String videoPath) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath)) {
            grabber.setFormat("mp4");
            grabber.start();
            grabber.setTimestamp(1000000);
            Frame frame = grabber.grabImage();

            if (frame == null) {
                throw new RuntimeException("Failed to grab video frame");
            }

            try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
                BufferedImage bufferedImage = converter.getBufferedImage(frame);
                if (bufferedImage == null) {
                    throw new RuntimeException("Failed to convert frame to BufferedImage");
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                if (!ImageIO.write(bufferedImage, "jpg", outputStream)) {
                    throw new RuntimeException("Failed to write BufferedImage to JPG");
                }

                byte[] thumbnailBytes = outputStream.toByteArray();

                String thumbnailFileName = videoPath.replace(".mp4", "_thumb.jpg");
                thumbnailFileName = "thumbnails/" + UUID.randomUUID().toString() + ".jpg";

                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(thumbnailFileName)
                        .stream(new ByteArrayInputStream(thumbnailBytes), thumbnailBytes.length, -1)
                        .contentType("image/jpeg")
                        .build());

                return thumbnailBytes;
            }
        } catch (IOException | ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new RuntimeException("Failed to generate thumbnail: " + e.getMessage(), e);
        }
    }

    public byte[] getVideo(String fileName) {
        try {
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .build());
            return response.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get video from MinIO: " + e.getMessage(), e);
        }
    }

    public String getVideoUrl(String fileName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .method(Method.GET)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate video URL: " + e.getMessage(), e);
        }
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