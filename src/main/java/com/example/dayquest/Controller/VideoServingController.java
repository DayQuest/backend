package com.example.dayquest.Controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/videos")
public class VideoServingController {

    private static final String VIDEO_DIRECTORY = "/root/uploads";

    @GetMapping("/stream/{filename}")
    public ResponseEntity<Resource> streamVideo(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(VIDEO_DIRECTORY).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
