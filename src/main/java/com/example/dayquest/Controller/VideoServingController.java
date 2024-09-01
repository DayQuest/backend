package com.example.dayquest.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.dayquest.Service.VideoService;
import com.example.dayquest.model.Video;

import java.util.Base64;

@RestController
@RequestMapping("/api/videos")
public class VideoServingController {

    private final VideoService videoService;

    @Autowired
    public VideoServingController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/stream/{videoId}")
    public ResponseEntity<byte[]> streamVideo(@PathVariable String videoId) {
        try {
            // Use the videoService instance to call getVideoById
            Video video = videoService.getVideoById(videoId);
            if (video != null && video.getVideo64() != null) {
                byte[] videoBytes = Base64.getDecoder().decode(video.getVideo64());

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("video/mp4"));
                headers.setContentDispositionFormData("attachment", video.getTitle() + ".mp4");
                headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

                return new ResponseEntity<>(videoBytes, headers, HttpStatus.OK);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}