package com.dayquest.dayquestbackend.video;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos")
public class VideoServingController {

    private final VideoService videoService;

    @Autowired
    public VideoServingController(VideoService videoService) {
        this.videoService = videoService;
    }
}
