package com.dayquest.userservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO Configuration for profile picture storage.
 * Migrates profile pictures from PostgreSQL BLOB to object storage.
 */
@Configuration
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true", matchIfMissing = true)
public class MinioConfig {

    private static final Logger logger = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.accessKey:minioadmin}")
    private String accessKey;

    @Value("${minio.secretKey:minioadmin}")
    private String secretKey;

    @Value("${minio.profilePictureBucket:profile-pictures}")
    private String profilePictureBucket;

    @Bean
    public MinioClient minioClient() {
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            initializeBucket(client, profilePictureBucket);

            logger.info("MinIO client initialized successfully for endpoint: {}", endpoint);
            return client;
        } catch (Exception e) {
            logger.warn("Failed to initialize MinIO client: {}. Profile pictures will use database fallback.", e.getMessage());
            return null;
        }
    }

    private void initializeBucket(MinioClient client, String bucketName) {
        try {
            boolean bucketExists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());

            if (!bucketExists) {
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                logger.info("Created bucket: {}", bucketName);

                // Set public read policy for profile pictures
                String policy = """
                    {
                        "Version": "2012-10-17",
                        "Statement": [
                            {
                                "Effect": "Allow",
                                "Principal": "*",
                                "Action": ["s3:GetObject"],
                                "Resource": ["arn:aws:s3:::%s/*"]
                            }
                        ]
                    }
                    """.formatted(bucketName);

                client.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policy)
                        .build());
                logger.info("Set public read policy for bucket: {}", bucketName);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize bucket: {}", bucketName, e);
        }
    }
}

