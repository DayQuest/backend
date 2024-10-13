package com.dayquest.dayquestbackend.video;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

public class VideoCompressor {

   CompletableFuture<Void> compressVideo(String inputFile, String outputFile) {
    return CompletableFuture.runAsync(() -> {
      try {
        String[] cmd = {
            "ffmpeg",
            "-i", inputFile,
            "-c:v", "libx265", // Use H.265 (HEVC) codec
            "-preset", "slow", // Slower preset for better compression
            "-crf", "28", // Constant Rate Factor (CRF) - adjusted for HEVC
            "-vf", "scale=-1:720", // Scale to 720p, maintaining aspect ratio
            "-r", "30", // Set frame rate to 24 fps
            "-c:a", "copy", // Copy audio without re-encoding
            "-tag:v", "hvc1", // Add tag for better compatibility
            "-movflags", "+faststart",
            outputFile
        };

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
          System.out.println(line);
        }

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
