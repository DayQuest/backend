package com.example.dayquest.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.example.dayquest.video.VideoRepository;
import com.example.dayquest.video.Video;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoService {
    @Value("${video.upload.path}")
    private String uploadPath;

    @Autowired
    private VideoRepository videoRepository;

    public Video upvoteVideo(Long id) {
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
        long count = videoRepository.count();
        if (count == 0) {
            throw new RuntimeException("No videos available");
        }
        long randomId = new Random().nextLong(count);
        return videoRepository.findAll().get((int) randomId);
    }

    public String uploadVideo(MultipartFile file, String title, String description) throws IOException {
        String fileName = UUID.randomUUID().toString() + ".mp4";
        Path filePath = Paths.get(uploadPath, fileName);
        Files.copy(file.getInputStream(), filePath);

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setFilePath(fileName.replace(".mp4", ""));
        videoRepository.save(video);
        return fileName;
    }

    public void deleteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video nicht gefunden"));
        try {
            Files.deleteIfExists(Paths.get(uploadPath, video.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete video file", e);
        }
        videoRepository.delete(video);
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