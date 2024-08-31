package com.example.dayquest.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.example.dayquest.Repository.VideoRepository;
import com.example.dayquest.model.Video;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoService {
    public int videos;

    @Autowired
    private static VideoRepository videoRepository;

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

    public String uploadVideo(MultipartFile file, String title, String description) throws IOException {
        String videoId = UUID.randomUUID().toString();

        String base64Video = Base64.getEncoder().encodeToString(file.getBytes());

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setVideo64(base64Video);
        video.setFilePath(videoId); // Wir verwenden filePath als eindeutige ID
        videoRepository.save(video);
        videos++;
        return videoId;
    }

    public void deleteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video nicht gefunden"));
        videoRepository.delete(video);
        videos--;
    }

    public Video getVideo(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video nicht gefunden"));
    }
    public static Video getVideoById(String videoId) {
        return videoRepository.findByFilePath(videoId)
                .orElseThrow(() -> new RuntimeException("Video nicht gefunden"));
    }
}