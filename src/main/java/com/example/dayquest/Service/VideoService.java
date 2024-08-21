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
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.VideoSize;

@Service
public class VideoService {
    public int videos;
    private static final String VIDEO_DIRECTORY = "E:\\dayquest\\src\\main\\resources\\uploads\\";
    private static final String VIDEO_URL_PREFIX = "http://192.168.178.58:8080/api/videos/stream/";

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
        File targetFile = new File(VIDEO_DIRECTORY + filename);

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

    private File compressVideo(File source) {
        File target = new File(VIDEO_DIRECTORY + "compressed_" + source.getName());

        // Define the video attributes
        VideoAttributes video = new VideoAttributes();
        video.setCodec("h264");
        video.setBitRate(1200000);
        video.setFrameRate(30); // 30 fps
        video.setSize(new VideoSize(720, 1280));

        // Set encoding attributes
        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setOutputFormat("mp4");
        attrs.setVideoAttributes(video);

        // Encode the video
        Encoder encoder = new Encoder();
        try {
            encoder.encode(new MultimediaObject(source), target, attrs);
        } catch (IllegalArgumentException | EncoderException e) {
            e.printStackTrace();
        }

        return target;
    }

}


