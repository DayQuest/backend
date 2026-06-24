package com.dayquest.userservice.services;

import io.minio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing profile pictures in MinIO object storage.
 * Provides fallback to database storage if MinIO is unavailable.
 */
@Service
public class ProfilePictureService {

    private static final Logger logger = LoggerFactory.getLogger(ProfilePictureService.class);

    private final MinioClient minioClient;

    @Value("${minio.profilePictureBucket:profile-pictures}")
    private String bucketName;

    @Value("${minio.endpoint:http://minio:9000}")
    private String minioEndpoint;

    @Autowired(required = false)
    public ProfilePictureService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Check if MinIO is available
     */
    public boolean isMinioAvailable() {
        return minioClient != null;
    }

    /**
     * Upload profile picture to MinIO
     *
     * @param userId User UUID
     * @param imageBytes Compressed image bytes
     * @return URL of the uploaded image, or null if upload failed
     */
    @Async
    public CompletableFuture<String> uploadProfilePicture(UUID userId, byte[] imageBytes) {
        if (minioClient == null) {
            logger.debug("MinIO not available, returning null");
            return CompletableFuture.completedFuture(null);
        }

        try {
            String objectName = "user-" + userId.toString() + ".jpg";

            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(bais, imageBytes.length, -1)
                    .contentType("image/jpeg")
                    .build());

            String url = minioEndpoint + "/" + bucketName + "/" + objectName;
            logger.info("Uploaded profile picture for user {} to {}", userId, url);

            return CompletableFuture.completedFuture(url);

        } catch (Exception e) {
            logger.error("Failed to upload profile picture for user {}: {}", userId, e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Get profile picture from MinIO
     *
     * @param userId User UUID
     * @return Image bytes, or null if not found
     */
    public byte[] getProfilePicture(UUID userId) {
        if (minioClient == null) {
            return null;
        }

        try {
            String objectName = "user-" + userId.toString() + ".jpg";

            try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build())) {

                return stream.readAllBytes();
            }

        } catch (Exception e) {
            logger.debug("Profile picture not found in MinIO for user {}", userId);
            return null;
        }
    }

    /**
     * Delete profile picture from MinIO
     *
     * @param userId User UUID
     */
    @Async
    public CompletableFuture<Boolean> deleteProfilePicture(UUID userId) {
        if (minioClient == null) {
            return CompletableFuture.completedFuture(false);
        }

        try {
            String objectName = "user-" + userId.toString() + ".jpg";

            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());

            logger.info("Deleted profile picture for user {}", userId);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            logger.error("Failed to delete profile picture for user {}: {}", userId, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Get the public URL for a user's profile picture
     *
     * @param userId User UUID
     * @return Public URL
     */
    public String getProfilePictureUrl(UUID userId) {
        if (minioClient == null) {
            return null;
        }
        return minioEndpoint + "/" + bucketName + "/user-" + userId.toString() + ".jpg";
    }

    /**
     * Check if profile picture exists in MinIO
     */
    public boolean profilePictureExists(UUID userId) {
        if (minioClient == null) {
            return false;
        }

        try {
            String objectName = "user-" + userId.toString() + ".jpg";
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}


