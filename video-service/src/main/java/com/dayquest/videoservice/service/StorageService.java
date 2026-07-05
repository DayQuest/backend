package com.dayquest.videoservice.service;

import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    private final MinioClient minioClient;
    private final String videoBucket;
    private final String thumbnailBucket;

    public StorageService(
            MinioClient minioClient,
            @Value("${minio.videoBucket}") String videoBucket,
            @Value("${minio.thumbnailBucket}") String thumbnailBucket) {
        this.minioClient = minioClient;
        this.videoBucket = videoBucket;
        this.thumbnailBucket = thumbnailBucket;
    }

    public String uploadVideo(MultipartFile file, UUID videoUuid) {
        String fileName = videoUuid.toString() + getFileExtension(file.getOriginalFilename());
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(videoBucket)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            logger.info("Video uploaded successfully: {}", fileName);
            return fileName;
        } catch (Exception e) {
            logger.error("Failed to upload video: {}", fileName, e);
            throw new RuntimeException("Failed to upload video", e);
        }
    }

    public String uploadThumbnail(byte[] thumbnailData, UUID videoUuid) {
        String fileName = videoUuid.toString() + ".jpg";
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(thumbnailData)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(thumbnailBucket)
                    .object(fileName)
                    .stream(inputStream, thumbnailData.length, -1)
                    .contentType("image/jpeg")
                    .build());

            logger.info("Thumbnail uploaded successfully: {}", fileName);
            return fileName;
        } catch (Exception e) {
            logger.error("Failed to upload thumbnail: {}", fileName, e);
            throw new RuntimeException("Failed to upload thumbnail", e);
        }
    }

    public String getVideoUrl(String fileName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(videoBucket)
                    .object(fileName)
                    .method(Method.GET)
                    .expiry(24, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            logger.error("Failed to get video URL: {}", fileName, e);
            throw new RuntimeException("Failed to get video URL", e);
        }
    }

    public String getThumbnailUrl(String fileName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(thumbnailBucket)
                    .object(fileName)
                    .method(Method.GET)
                    .expiry(24, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            logger.error("Failed to get thumbnail URL: {}", fileName, e);
            throw new RuntimeException("Failed to get thumbnail URL", e);
        }
    }

    public byte[] downloadVideo(String fileName) {
        try {
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(videoBucket)
                    .object(fileName)
                    .build());
            return response.readAllBytes();
        } catch (Exception e) {
            logger.error("Failed to download video: {}", fileName, e);
            throw new RuntimeException("Failed to download video", e);
        }
    }

    public void deleteVideo(String fileName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(videoBucket)
                    .object(fileName)
                    .build());
            logger.info("Video deleted: {}", fileName);
        } catch (Exception e) {
            logger.error("Failed to delete video: {}", fileName, e);
        }
    }

    public void deleteThumbnail(String fileName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(thumbnailBucket)
                    .object(fileName)
                    .build());
            logger.info("Thumbnail deleted: {}", fileName);
        } catch (Exception e) {
            logger.error("Failed to delete thumbnail: {}", fileName, e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return ".mp4";
        }
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? ".mp4" : filename.substring(dotIndex);
    }
}

