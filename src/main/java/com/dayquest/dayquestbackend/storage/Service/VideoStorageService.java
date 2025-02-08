package com.dayquest.dayquestbackend.storage.Service;

import com.dayquest.dayquestbackend.hashtag.Hashtag;
import com.dayquest.dayquestbackend.hashtag.HashtagRepository;
import com.dayquest.dayquestbackend.hashtag.HashtagService;
import com.dayquest.dayquestbackend.user.User;
import com.dayquest.dayquestbackend.video.states.Status;
import com.dayquest.dayquestbackend.video.models.Video;
import com.dayquest.dayquestbackend.video.repository.VideoRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class VideoStorageService {
    @Value("${minio.rawVideosBucket}")
    private String bucket;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private HashtagRepository hashtagRepository;
    @Autowired
    private HashtagService hashtagService;

    @Async
    public CompletableFuture<ResponseEntity<String>> uploadVideo(MultipartFile file, String title, String description, User user, List<String> hashtags) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null || !originalFilename.contains(".")) {
                    return ResponseEntity.badRequest().body("Invalid file format.");
                }
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();

                String filePath = UUID.randomUUID().toString() + fileExtension;
                List<Hashtag> videoHashtags = new ArrayList<>();
                for (String hashtag : hashtags) {
                    Hashtag foundHashtag = hashtagRepository.findByHashtag(hashtag);
                    if (foundHashtag != null) {
                        videoHashtags.add(foundHashtag);
                    } else {
                        hashtagService.createHashtag(hashtag);
                        videoHashtags.add(hashtagRepository.findByHashtag(hashtag));
                    }
                }

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
                video.setHashtags(videoHashtags);
                videoRepository.save(video);

                return ResponseEntity.ok("Uploaded");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to upload video file: " + e.getMessage());
            }
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
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to delete thumbnail file: " + e.getMessage());
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
