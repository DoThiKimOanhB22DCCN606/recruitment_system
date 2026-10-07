package com.recruitment.company.minio;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Wraps the MinIO Java SDK for the company-logos bucket: bucket provisioning,
 * upload/replace/delete of logos, and presigned URL generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.company-logos}")
    private String logoBucket;

    @Value("${minio.presigned-url.expiry-seconds:600}")
    private int presignedUrlExpirySeconds;

    @PostConstruct
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(logoBucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(logoBucket).build());
                log.info("Created MinIO bucket: {}", logoBucket);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket '{}'", logoBucket, e);
        }
    }

    /** Uploads (or replaces) a company's logo. Returns the object key. */
    public String uploadLogo(Long companyId, MultipartFile file) {
        String objectKey = "companies/%d/logo/%s-%s".formatted(
                companyId, System.currentTimeMillis(), sanitize(file.getOriginalFilename()));
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(logoBucket)
                    .object(objectKey)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload logo to MinIO: " + e.getMessage(), e);
        }
    }

    public void deleteObject(String objectKey) {
        if (objectKey == null) return;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(logoBucket).object(objectKey).build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete object from MinIO: " + e.getMessage(), e);
        }
    }

    public String generatePresignedDownloadUrl(String objectKey) {
        if (objectKey == null) return null;
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(logoBucket)
                    .object(objectKey)
                    .expiry(presignedUrlExpirySeconds, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage(), e);
        }
    }

    private String sanitize(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
