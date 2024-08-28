package com.example.dayquest.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.example.dayquest.Repository.VideoRepository;
import com.example.dayquest.model.Video;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ws.schild.jave.*;


@Service
public class VideoService {
    public int videos;
    private static final String VIDEO_DIRECTORY = "/root/uploads";
    private static final String VIDEO_URL_PREFIX = "http://77.90.21.53:8090/api/videos/stream/";

    @Autowired
    private VideoRepository videoRepository;

    public Video upvoteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setUpvotes(video.getUpvotes() + 1);
        return videoRepository.save(video);
    }

    public Video downvoteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        video.setDownvotes(video.getDownvotes() + 1);
        return videoRepository.save(video);
    }

    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }

    public String uploadVideo(MultipartFile file, String title, String description) throws IOException {
        String filename = UUID.randomUUID().toString() + ".mp4";
        File targetFile = new File(VIDEO_DIRECTORY + "/" + filename);

        file.transferTo(targetFile);

        File compressedFile = compressVideo(targetFile);
        targetFile.delete();

        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setFilePath(VIDEO_URL_PREFIX + compressedFile.getName());
        videoRepository.save(video);
        videos++;
        return compressedFile.getName();
    }

    public void deleteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        String filePath = VIDEO_DIRECTORY + video.getFilePath().replace(VIDEO_URL_PREFIX, "");
        File file = new File(filePath);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("File deleted successfully");
            } else {
                System.out.println("Failed to delete the file");
            }
        }
        videoRepository.delete(video);
        videos--;
    }
    public File compressVideo(File source) {
        File target = new File(VIDEO_DIRECTORY + "compressed_" + source.getName());

        return target;
    }

}


