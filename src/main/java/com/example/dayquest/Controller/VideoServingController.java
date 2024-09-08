package com.example.dayquest.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import com.example.dayquest.Service.VideoService;
import com.example.dayquest.model.Video;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/videos")
public class VideoServingController {

    private final VideoService videoService;

    @Autowired
    public VideoServingController(VideoService videoService) {
        this.videoService = videoService;
    }
}
