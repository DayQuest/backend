package com.dayquest.dayquestbackend.video;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.dayquest.dayquestbackend.streak.Streak;
import com.dayquest.dayquestbackend.streak.StreakRepository;
import com.dayquest.dayquestbackend.streak.StreakService;
import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.user.UserRepository;
import jakarta.transaction.Transactional;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    @Value("${video.upload.path}")
    private String uploadPath;


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
    public VideoService(ViewedVideoRepository viewedVideoRepository, VideoRepository videoRepository, PlatformTransactionManager transactionManager) {
        this.viewedVideoRepository = viewedVideoRepository;
        this.videoRepository = videoRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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

            return videoRepository.findRandomVideo().orElseThrow(() -> new RuntimeException("Failed to find random video"));
        });
    }


    @Async
    @Transactional
    public CompletableFuture<String> uploadVideo(MultipartFile file, String title, String description, User user) {
        return CompletableFuture.supplyAsync(() -> {
            String fileName = UUID.randomUUID() + ".mp4";
            Path filePath = Paths.get(uploadPath, fileName);

            try {
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                videoCompressor.compressVideo(filePath.toString(), fileName);
                videoCompressor.removeUnprocessed(filePath.toString());

                String processedFilePath = filePath.toString().replace("unprocessed", "processed");

                if (!Files.exists(Paths.get(processedFilePath))) {
                    throw new RuntimeException("Processed video file not found: " + processedFilePath);
                }

                return transactionTemplate.execute(status -> {
                    User managedUser = userRepository.findById(user.getUuid())
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    Video video = new Video();
                    video.setTitle(title);
                    video.setThumbnail(generateThumbnail(processedFilePath));
                    video.setDescription(description);
                    video.setFilePath(fileName.replace(".mp4", ""));
                    video.setUser(managedUser);
                    video.setQuestUuid(managedUser.getDailyQuest().getUuid());

                    managedUser.addPostedVideo(video);

                    userRepository.save(managedUser);

                    Streak streak = streakRepository.findByUserId(managedUser.getUuid());
                    if (streak == null) {
                        streakService.createStreak(managedUser.getUuid());
                    } else {
                        streakService.updateStreak(managedUser.getUuid());
                    }
                    return fileName;
                });

            } catch (Exception e) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException ignored) {}
                throw new RuntimeException("Failed to process video: " + e.getMessage(), e);
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

                return outputStream.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate thumbnail: " + e.getMessage(), e);
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
                Files.deleteIfExists(Paths.get(uploadPath, video.get().getFilePath()));
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete video file");
            }

            videoRepository.deleteById(uuid);
            return ResponseEntity.ok("Deleted");
        });
    }
}