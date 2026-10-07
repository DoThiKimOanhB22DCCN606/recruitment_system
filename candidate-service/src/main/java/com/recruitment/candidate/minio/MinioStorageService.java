package com.recruitment.candidate.minio;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Wraps the MinIO Java SDK: bucket provisioning, object upload/delete, and
 * presigned URL generation for temporary, permission-scoped file access.
 * Used by CV storage (bucket: candidate-cvs).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.candidate-cvs}")
    private String cvBucket;

    @Value("${minio.presigned-url.expiry-seconds:600}")
    private int presignedUrlExpirySeconds;

    @PostConstruct
    public void ensureBucketsExist() {
        try {
            createBucketIfMissing(cvBucket);
        } catch (Exception e) {
            log.error("Failed to initialize MinIO buckets", e);
        }
    }

    private void createBucketIfMissing(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Created MinIO bucket: {}", bucket);
        }
    }

    /** Uploads a candidate CV. Returns the generated object key. */
    public String uploadCv(Long candidateId, MultipartFile file) {
        String objectKey = "candidates/%d/cvs/%s-%s".formatted(
                candidateId, System.currentTimeMillis(), sanitize(file.getOriginalFilename()));
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(cvBucket)
                    .object(objectKey)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload CV to MinIO: " + e.getMessage(), e);
        }
    }

    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(cvBucket).object(objectKey).build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete object from MinIO: " + e.getMessage(), e);
        }
    }

    /** Generates a time-limited presigned GET URL so clients can download the CV directly from MinIO. */
    public String generatePresignedDownloadUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(cvBucket)
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
