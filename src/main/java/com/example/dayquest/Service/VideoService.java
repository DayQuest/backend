package com.example.dayquest.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    private static final String VIDEO_DIRECTORY = "uploads";
    private static final String VIDEO_URL_PREFIX = "/api/videos/stream/";
    private final Path rootLocation;

    @Autowired
    private VideoRepository videoRepository;

    public VideoService() {
        this.rootLocation = Paths.get(System.getProperty("user.dir"), VIDEO_DIRECTORY);
        createVideoDirectory();
    }
    public Path getVideoPath(String filename) {
        return this.rootLocation.resolve(filename).normalize();
    }

    private void createVideoDirectory() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Konnte Verzeichnis nicht erstellen: " + rootLocation, e);
        }
    }

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
        String filename = UUID.randomUUID().toString() + ".mp4";
        Path targetPath = this.rootLocation.resolve(filename);

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setFilePath(filename); // Speichern Sie nur den Dateinamen, nicht den ganzen Pfad
        videoRepository.save(video);
        videos++;
        return filename;
    }

    public void deleteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video nicht gefunden"));
        Path filePath = this.rootLocation.resolve(video.getFilePath().replace(VIDEO_URL_PREFIX, ""));
        try {
            Files.deleteIfExists(filePath);
            System.out.println("Datei erfolgreich gelöscht");
        } catch (IOException e) {
            System.out.println("Fehler beim Löschen der Datei: " + e.getMessage());
        }
        videoRepository.delete(video);
        videos--;
    }
}