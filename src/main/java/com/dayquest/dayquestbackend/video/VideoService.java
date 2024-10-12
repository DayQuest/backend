package com.dayquest.dayquestbackend.video;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoService {
    @Value("${video.upload.path}")
    private String uploadPath;

    @Autowired
    private VideoRepository videoRepository;

    public CompletableFuture<Video> upvoteVideo(UUID uuid) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video nicht gefunden"));
        video.setUpvotes(video.getUpvotes() + 1);
        return videoRepository.save(video);
    }

    public Video downvoteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video nicht gefunden"));
        video.setDownvotes(video.getDownvotes() + 1);
        return videoRepository.save(video);
    }

    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }

    @Async
    public CompletableFuture<String> uploadVideoAsync(MultipartFile file, String title, String description) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return uploadVideo(file, title, description);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload video", e);
            }
        });
    }

    public Video getRandomVideo() {
        boolean test = true;

        long count = videoRepository.count();
        if (count == 0) {
            throw new RuntimeException("No videos available");
        }
        long randomId = new Random().nextLong(count);
        return videoRepository.findAll().get((int) randomId);
    }

    public CompletableFuture<String> uploadVideo(MultipartFile file, String title, String description) {
        return CompletableFuture.supplyAsync(() -> {
            String fileName = UUID.randomUUID() + ".mp4";
            Path filePath = Paths.get(uploadPath, fileName);
          try {
            Files.copy(file.getInputStream(), filePath);
          } catch (IOException e) {
            return null;
          }

          Video video = new Video();
            video.setTitle(title);
            video.setDescription(description);
            video.setFilePath(fileName.replace(".mp4", ""));
            videoRepository.save(video);
            return fileName;
        });
    }

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