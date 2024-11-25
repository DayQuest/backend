package com.dayquest.dayquestbackend.video;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
    private VideoRepository videoRepository;

    @Autowired
    private VideoCompressor videoCompressor;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final TransactionTemplate transactionTemplate;

    @Autowired
    public VideoService(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    //TODO: Remove code dupe in down and upvote
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


    //Unused
    public Resource loadVideoAsResource(String fileName) {
        try {
            Path filePath = Paths.get(uploadPath).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + fileName);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read file: " + fileName, e);
        }
    }
}