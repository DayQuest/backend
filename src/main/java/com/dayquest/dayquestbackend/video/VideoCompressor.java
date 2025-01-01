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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Service
public class VideoCompressor {

  private static final Logger logger = Logger.getLogger(VideoCompressor.class.getName());

  @Value("${video.processed.path}")
  private String processPath;

  @Value("${video.upload.path}")
  private String unprocessedPath;

  @Value("${ffmpeg.path}")
  private String ffmpegPath;

  @PostConstruct
  public void init() {
    validateFFmpegInstallation();
    compressAllUnprocessed();
  }

  void compressVideo(String inputFile, String outputFileName) {
    validateInputs(inputFile, outputFileName);

    try {
      String extension = outputFileName.substring(outputFileName.lastIndexOf('.'));
      File tempFile = File.createTempFile("compress_", extension);
      File finalOutputFile = new File(processPath, outputFileName);

      try {
        VideoMetadata metadata = getVideoMetadata(inputFile);

        boolean useX265 = true;
        int attempts = 0;
        int maxAttempts = 2;

        while (attempts < maxAttempts) {
          if (compressVideoAttempt(inputFile, tempFile, useX265, metadata)) {
            if (finalOutputFile.exists()) {
              finalOutputFile.delete();
            }
            if (!tempFile.renameTo(finalOutputFile)) {
              Files.copy(tempFile.toPath(), finalOutputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return;
          }

          if (useX265) {
            logger.info("x265 compression failed, falling back to x264");
            useX265 = false;
          } else {
            throw new RuntimeException("Video compression failed after x264 attempt");
          }

          attempts++;
        }

        throw new RuntimeException("Video compression failed after all attempts");

      } finally {
        if (tempFile.exists()) {
          tempFile.delete();
        }
      }

    } catch (Exception e) {
      throw new RuntimeException("Video compression error: " + e.getMessage(), e);
    }
  }

  private void validateFFmpegInstallation() {
    try {
      ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
      Process process = pb.start();
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new RuntimeException("FFmpeg is not properly installed");
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to verify FFmpeg installation: " + e.getMessage());
    }
  }

  private void validateInputs(String inputFile, String outputFileName) {
    if (inputFile == null || outputFileName == null) {
      throw new IllegalArgumentException("Input file and output file name cannot be null");
    }

    File input = new File(inputFile);
    if (!input.exists()) {
      throw new IllegalArgumentException("Input file does not exist: " + inputFile);
    }

    File outputDir = new File(processPath);
    if (!outputDir.exists() || !outputDir.canWrite()) {
      throw new IllegalArgumentException("Output directory is not writable: " + processPath);
    }
  }

  private VideoMetadata getVideoMetadata(String inputFile) throws Exception {
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

    String jsonOutput = probeOutput.toString();
    return new VideoMetadata(
            extractIntFromJson(jsonOutput, "width"),
            extractIntFromJson(jsonOutput, "height")
    );
  }

  private boolean isInputHEVC(String inputFile) throws Exception {
    ProcessBuilder probeBuilder = new ProcessBuilder(
            "ffprobe",
            "-v", "quiet",
            "-select_streams", "v:0",
            "-show_entries", "stream=codec_name",
            "-of", "default=noprint_wrappers=1:nokey=1",
            inputFile
    );

    Process probeProcess = probeBuilder.start();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(probeProcess.getInputStream()))) {
      String codec = reader.readLine();
      return "hevc".equals(codec);
    }
  }

  private boolean compressVideoAttempt(String inputFile, File outputFile, boolean useX265, VideoMetadata metadata) throws Exception {
    List<String> compressCommand = new ArrayList<>();
    compressCommand.addAll(Arrays.asList(
            "ffmpeg",
            "-y",
            "-i", inputFile
    ));

    boolean isInputHEVC = isInputHEVC(inputFile);
    long inputBitrate = getInputBitrate(inputFile);

    if (isInputHEVC && inputBitrate < 500_000) {
      compressCommand.addAll(Arrays.asList(
              "-c:v", "copy",
              "-c:a", "copy"
      ));
    } else if (useX265) {
      compressCommand.addAll(Arrays.asList(
              "-c:v", "libx265",
              "-preset", "medium",
              "-crf", "28",
              "-tag:v", "hvc1",
              "-c:a", "aac",
              "-b:a", "128k",
              "-ar", "48000"
      ));
    } else {
      compressCommand.addAll(Arrays.asList(
              "-c:v", "libx264",
              "-preset", "medium",
              "-crf", "23",
              "-c:a", "aac",
              "-b:a", "128k",
              "-ar", "48000"
      ));
    }

    compressCommand.addAll(Arrays.asList(
            "-movflags", "+faststart",
            outputFile.getPath()
    ));

    logger.info("Executing FFmpeg command: " + String.join(" ", compressCommand));

    ProcessBuilder pb = new ProcessBuilder(compressCommand);
    pb.redirectErrorStream(true);
    Process process = pb.start();

    StringBuilder outputLog = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains("time=")) {
          logProgress(line);
        }
        outputLog.append(line).append("\n");
      }
    }

    int exitCode = process.waitFor();
    boolean success = exitCode == 0 && outputFile.exists() && outputFile.length() > 0;

    if (!success) {
      logger.severe("FFmpeg failed with exit code: " + exitCode + "\nOutput log: " + outputLog.toString());
    }

    return success;
  }

  private long getInputBitrate(String inputFile) throws Exception {
    ProcessBuilder probeBuilder = new ProcessBuilder(
            "ffprobe",
            "-v", "quiet",
            "-select_streams", "v:0",
            "-show_entries", "stream=bit_rate",
            "-of", "default=noprint_wrappers=1:nokey=1",
            inputFile
    );

    Process probeProcess = probeBuilder.start();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(probeProcess.getInputStream()))) {
      String bitrate = reader.readLine();
      return bitrate != null && !bitrate.isEmpty() ? Long.parseLong(bitrate) : 0;
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private void logProgress(String line) {
    try {
      String time = line.split("time=")[1].split(" ")[0];
      logger.info("Compression progress: " + time);
    } catch (Exception e) {
        logger.warning("Failed to parse FFmpeg progress: " + e.getMessage());
    }
  }

  private void logError(String message, String outputLog) {
    logger.severe(message + "\nFFmpeg output log:\n" + outputLog);
  }

  private int extractIntFromJson(String json, String key) {
    try {
      String searchKey = "\"" + key + "\":";
      int index = json.indexOf(searchKey);
      if (index != -1) {
        int start = index + searchKey.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) return 0;

        String valueStr = json.substring(start, end).trim();
        return Integer.parseInt(valueStr);
      }
    } catch (Exception e) {
      logger.warning("Failed to extract " + key + " from JSON: " + e.getMessage());
    }
    return 0;
  }

  public void removeUnprocessed(String path) {
    File file = new File(path);
    if (!file.delete()) {
      logger.warning("Unable to delete unprocessed file: " + path);
    }
  }

  private void compressAllUnprocessed() {
    logger.info("Starting compression of unprocessed videos");
    File unprocessedDir = new File(unprocessedPath);
    File[] files = unprocessedDir.listFiles();
    if (files == null) {
      logger.info("No unprocessed videos found");
      return;
    }

    for (File file : files) {
      try {
        compressVideo(file.getPath(), file.getName());
        removeUnprocessed(file.getPath());
      } catch (Exception e) {
        logger.severe("Failed to process file " + file.getName() + ": " + e.getMessage());
      }
    }

    logger.info("Compression process completed");
  }

  private static class VideoMetadata {
    final int width;
    final int height;

    VideoMetadata(int width, int height) {
      this.width = width;
      this.height = height;
    }
  }
}