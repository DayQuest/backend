package com.dayquest.dayquestbackend.video;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.checkerframework.checker.units.qual.A;
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

    @Autowired
    private VideoCompressor videoCompressor;


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
            return ResponseEntity.ok(video.get());
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
            return ResponseEntity.ok(video.get());
        });
    }

    @Async
    public CompletableFuture<Video> getRandomVideo() {
        return CompletableFuture.supplyAsync(() -> {
            long count = videoRepository.count();
            if (count == 0) {
                throw new RuntimeException("No videos available");
            }
            long randomId = new Random().nextLong(count);
            return videoRepository.findAll().get((int) randomId);
        });
    }

    @Async
    public CompletableFuture<String> uploadVideo(MultipartFile file, String title, String description) {
        return CompletableFuture.supplyAsync(() -> {
            String fileName = UUID.randomUUID() + ".mp4";
            Path filePath = Paths.get(uploadPath, fileName);
          try {
            Files.copy(file.getInputStream(), filePath);
          } catch (IOException e) {
            return null;
          }

          videoCompressor.compressVideo(filePath.toString(), fileName);
          videoCompressor.removeUnprocessed(filePath.toString());

          Video video = new Video();
            video.setTitle(title);
            video.setDescription(description);
            video.setFilePath(fileName.replace(".mp4", ""));
            videoRepository.save(video);
            return fileName;
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