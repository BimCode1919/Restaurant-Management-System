package com.restaurant.qrorder.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE)
public class MinioService {

    final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    String bucketName;

    @Value("${minio.endpoint}")
    String endpoint;

    @Value("${minio.presigned-url-expiry}")
    int presignedUrlExpiry;

    /**
     * Generate a presigned PUT URL for frontend to upload directly to MinIO.
     *
     * @param originalFileName original file name from client (e.g., "photo.jpg")
     * @param contentType      MIME type (e.g., "image/jpeg")
     * @return array of [presignedPutUrl, publicUrl]
     */
    public String[] generatePresignedPutUrl(String originalFileName, String contentType) {
        String objectName = buildObjectName(originalFileName);

        try {
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(presignedUrlExpiry, TimeUnit.MINUTES)
                            .build()
            );

            String publicUrl = endpoint + "/" + bucketName + "/" + objectName;

            log.debug("Generated presigned PUT URL for object: {}", objectName);
            return new String[]{presignedUrl, publicUrl};

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for object {}: {}", objectName, e.getMessage());
            throw new RuntimeException("Failed to generate presigned upload URL", e);
        }
    }

    private String buildObjectName(String originalFileName) {
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);
        }
        return "images/" + UUID.randomUUID() + extension;
    }
}
