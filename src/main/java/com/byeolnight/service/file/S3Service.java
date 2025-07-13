package com.byeolnight.service.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final GoogleVisionService googleVisionService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    public Map<String, String> generatePresignedUrl(String originalFilename) {
        ensureBucketPublicReadAccess();
        if (!isValidImageFile(originalFilename)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, png, gif, webp만 허용)");
        }
        try {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            S3Presigner presigner = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();

            String s3Key = generateS3Key(originalFilename);
            String contentType = getContentType(originalFilename);

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .putObjectRequest(builder -> builder
                            .bucket(bucketName)
                            .key(s3Key)
                            .contentType(contentType) // 명시적 Content-Type 설정
                            .acl(ObjectCannedACL.PUBLIC_READ)
                    )
                    .build();

            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            String permanentUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName, region, s3Key);

            Map<String, String> result = new HashMap<>();
            result.put("uploadUrl", presignedUrl);
            result.put("url", permanentUrl);
            result.put("s3Key", s3Key);
            result.put("originalName", originalFilename);
            result.put("contentType", contentType); // Content-Type 정보 추가

            log.info("Presigned URL 생성 완료: {} (영구 URL: {}, Content-Type: {})", s3Key, permanentUrl, contentType);
            return result;

        } catch (Exception e) {
            log.error("Presigned URL 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("파일 업로드 URL 생성에 실패했습니다.", e);
        }
    }

    private String generateS3Key(String originalFilename) {
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }
        return "uploads/" + UUID.randomUUID().toString() + extension;
    }

    public void deleteObject(String s3Key) {
        try {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            S3Client s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();

            s3Client.deleteObject(builder -> builder
                    .bucket(bucketName)
                    .key(s3Key)
            );
            log.info("S3 객체 삭제 완료: {}", s3Key);
        } catch (Exception e) {
            log.error("S3 객체 삭제 실패: {}", s3Key, e);
        }
    }

    public String generatePresignedUploadUrl(String filename) {
        try {
            Map<String, String> result = generatePresignedUrl(filename);
            return result.get("url");
        } catch (Exception e) {
            log.error("Presigned Upload URL 생성 실패: {}", filename, e);
            throw new RuntimeException("파일 업로드 URL 생성에 실패했습니다.", e);
        }
    }

    private String getContentType(String filename) {
        String extension = filename.toLowerCase();
        if (extension.endsWith(".jpg") || extension.endsWith(".jpeg")) return "image/jpeg";
        if (extension.endsWith(".png")) return "image/png";
        if (extension.endsWith(".gif")) return "image/gif";
        if (extension.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    public boolean validateUploadedImage(byte[] imageBytes) {
        try {
            boolean isSafe = googleVisionService.isImageSafe(imageBytes);
            log.info("이미지 검열 결과: {}", isSafe ? "안전" : "부적절");
            return isSafe;
        } catch (Exception e) {
            log.error("이미지 검열 중 오류 발생", e);
            return true;
        }
    }

    private boolean isValidImageFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) return false;
        String extension = filename.toLowerCase();
        return extension.endsWith(".jpg") || extension.endsWith(".jpeg") ||
                extension.endsWith(".png") || extension.endsWith(".gif") ||
                extension.endsWith(".webp");
    }

    public void ensureBucketPublicReadAccess() {
        log.info("S3 버킷 공개 읽기 권한 확인: {}", bucketName);
        log.info("영구 URL 형식: https://{}.s3.{}.amazonaws.com/uploads/[filename]", bucketName, region);
        setupLifecyclePolicy();
    }

    private void setupLifecyclePolicy() {
        try {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            S3Client s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();

            LifecycleRule rule = LifecycleRule.builder()
                    .id("cleanup-orphan-images")
                    .status(ExpirationStatus.ENABLED)
                    .filter(LifecycleRuleFilter.builder().prefix("uploads/").build())
                    .expiration(LifecycleExpiration.builder().days(7).build())
                    .build();

            PutBucketLifecycleConfigurationRequest request = PutBucketLifecycleConfigurationRequest.builder()
                    .bucket(bucketName)
                    .lifecycleConfiguration(BucketLifecycleConfiguration.builder().rules(rule).build())
                    .build();

            s3Client.putBucketLifecycleConfiguration(request);
            log.info("✅ S3 Lifecycle 정책 설정 완료: uploads/ 폴더 7일 후 자동 삭제");
        } catch (Exception e) {
            log.warn("⚠️ S3 Lifecycle 정책 설정 실패 (수동 설정 필요): {}", e.getMessage());
        }
    }

    public int cleanupOrphanImages() {
        try {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            S3Client s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix("uploads/")
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
            List<S3Object> objects = response.contents();

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
            List<S3Object> oldObjects = objects.stream()
                    .filter(obj -> obj.lastModified().isBefore(cutoffDate.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                    .collect(Collectors.toList());

            int deletedCount = 0;
            for (S3Object obj : oldObjects) {
                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(obj.key())
                            .build());
                    deletedCount++;
                    log.info("🗑️ 고아 이미지 삭제: {}", obj.key());
                } catch (Exception e) {
                    log.error("삭제 실패: {}", obj.key(), e);
                }
            }
            log.info("🧹 고아 이미지 정리 완료: {}개 삭제", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("고아 이미지 정리 실패", e);
            return 0;
        }
    }

    public int getOrphanImageCount() {
        try {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            S3Client s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix("uploads/")
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
            List<S3Object> objects = response.contents();

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
            long oldObjectCount = objects.stream()
                    .filter(obj -> obj.lastModified().isBefore(cutoffDate.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                    .count();

            log.info("고아 이미지 개수 조회: {}개", oldObjectCount);
            return (int) oldObjectCount;
        } catch (Exception e) {
            log.error("고아 이미지 개수 조회 실패", e);
            return 0;
        }
    }
}
