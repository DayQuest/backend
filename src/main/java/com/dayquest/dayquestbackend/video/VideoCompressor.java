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

  @Value("${ffmpeg.path}")
  private String ffmpegPath;

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
        ffmpegPath,
        "-i", inputFile,
        "-c:v", "libx265",
        "-preset", "slow",
        "-crf", "28",
        "-vf", "scale=-1:720",
        "-r", "30",
        "-c:a", "copy",
        "-tag:v", "hvc1",
        "-movflags", "+faststart",
        outputFile.getPath()
    };
  }

  public void removeUnprocessed(String path) {
    File file = new File(path);
    if (!file.delete()) {
      Logger.getGlobal().info("Unable to delete unprocessed file: " + path);
    }
  }


  @PostConstruct
  public void compressAllUnprocessed() {
    Logger.getGlobal().info("Starting compression of unprocessed videos");
    File unprocessedDir = new File(unprocessedPath);
    File[] files = unprocessedDir.listFiles();
    if (files == null) {
      Logger.getGlobal().info("No unprocessed videos found");
      return;
    }

    for (File file : files) {
      compressVideo(file.getPath(), file.getName());
      removeUnprocessed(file.getPath());
    }

    Logger.getLogger("video compressor").info("Compression done");
  }
}
