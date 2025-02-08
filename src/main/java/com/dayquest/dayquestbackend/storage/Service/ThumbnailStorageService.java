package com.dayquest.dayquestbackend.storage.Service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class ThumbnailStorageService {
    @Value("${minio.thumbnailBucket}")
    private String bucket;

    @Autowired
    private MinioClient minioClient;

    public byte[] getThumbnail(String key) {
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(key + ".jpg")
                        .build()
        )) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
