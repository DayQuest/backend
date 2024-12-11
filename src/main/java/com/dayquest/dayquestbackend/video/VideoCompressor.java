package com.dayquest.dayquestbackend.video;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
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

      ProcessBuilder probeBuilder = new ProcessBuilder(
              "ffprobe",
              "-v", "quiet",
              "-print_format", "json",
              "-show_streams",
              "-select_streams", "v:0",
              inputFile
      );
      probeBuilder.redirectErrorStream(true);

      Process probeProcess = probeBuilder.start();
      BufferedReader probeReader = new BufferedReader(new InputStreamReader(probeProcess.getInputStream()));
      StringBuilder probeOutput = new StringBuilder();
      String probeLine;
      while ((probeLine = probeReader.readLine()) != null) {
        probeOutput.append(probeLine);
      }
      probeProcess.waitFor();

      int width = 0;
      int height = 0;

      try {
        String jsonOutput = probeOutput.toString();
        width = extractIntFromJson(jsonOutput, "width");
        height = extractIntFromJson(jsonOutput, "height");
      } catch (Exception e) {
        System.err.println("Failed to parse video metadata: " + e.getMessage());
      }

      boolean useX265 = true;
      int attempts = 0;
      int maxAttempts = 2;

      while (attempts < maxAttempts) {
        List<String> compressCommand = new ArrayList<>();
        compressCommand.addAll(Arrays.asList(
                "ffmpeg",
                "-i", inputFile
        ));

        if (useX265) {
          compressCommand.addAll(Arrays.asList(
                  "-c:v", "libx265",
                  "-preset", "medium",
                  "-crf", "26",
                  "-tag:v", "hvc1"
          ));
        } else {
          compressCommand.addAll(Arrays.asList(
                  "-c:v", "libx264",
                  "-preset", "medium",
                  "-crf", "20"
          ));
        }

        compressCommand.addAll(Arrays.asList(
                "-vf", "scale=iw:ih",
                "-r", "30",
                "-c:a", "copy",
                "-movflags", "+faststart",
                outputFile.getPath()
        ));


        ProcessBuilder pb = new ProcessBuilder(compressCommand);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder outputLog = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            outputLog.append(line).append("\n");
          }
        }

        int exitCode = process.waitFor();

        if (exitCode == 0 && outputFile.exists() && outputFile.length() > 0) {
          return;
        }

        if (useX265) {
          System.out.println("x265 compression failed, falling back to x264");
          useX265 = false;
          if (outputFile.exists()) {
            outputFile.delete();
          }
        } else {
          throw new RuntimeException("Video compression failed. Exit code: " + exitCode +
                  "\nOutput: " + outputLog.toString());
        }

        attempts++;
      }

      throw new RuntimeException("Video compression failed after all attempts");

    } catch (Exception e) {
      throw new RuntimeException("Video compression error: " + e.getMessage(), e);
    }
  }

  private int extractIntFromJson(String json, String key) {
    String searchKey = "\"" + key + "\":";
    int index = json.indexOf(searchKey);
    if (index != -1) {
      int start = index + searchKey.length();
      int end = json.indexOf(",", start);
      if (end == -1) end = json.indexOf("}", start);

      String valueStr = json.substring(start, end).trim();
      return Integer.parseInt(valueStr);
    }
    return 0;
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

    Logger.getGlobal().info("Compression done");
  }
}
