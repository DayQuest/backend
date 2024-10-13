package com.dayquest.dayquestbackend.video;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VideoCompressor {

  @Value("${video.processed.path}")
  private String processPath;

  @Value("${video.upload.path}")
  private String unprocessedPath;

  void compressVideo(String inputFile, String outputFileName) {
    try {
      File outputFile = new File(processPath, outputFileName);

      ProcessBuilder pb = new ProcessBuilder(getCompressCommand(inputFile, outputFile));
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
  }


  private String[] getCompressCommand(String inputFile, File outputFile) {
    return new String[]{
        "ffmpeg",
        "-i", inputFile,
        "-c:v", "libx265",
        "-preset", "slow", // Slower preset for better compression
        "-crf", "28", // Constant Rate Factor (CRF) - adjusted for HEVC
        "-vf", "scale=-1:720", // Scale to 720p, maintaining aspect ratio
        "-r", "30", // Set frame rate to 24 fps
        "-c:a", "copy", // Copy audio without re-encoding
        "-tag:v", "hvc1", // Add tag for better compatibility
        "-movflags", "+faststart",
        outputFile.getPath()
    };
  }

  public void removeUnprocessed(String path) {
    File file = new File(path);
    if (!file.delete()) {
      Logger.getLogger("Video Compressor").warning("Unable to delete unprocessed file: " + path);
    }
  }


  @PostConstruct
  public void compressAllUnprocessed() {
    File unprocessedDir = new File(unprocessedPath);
    File[] files = unprocessedDir.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      compressVideo(file.getPath(), file.getName());
      removeUnprocessed(file.getPath());
    }
  }
}
