package com.byeolnight.service.file;

import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.infrastructure.config.SecurityProperties;
import com.byeolnight.repository.post.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
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
public class S3Service {

    private final GoogleVisionService googleVisionService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final SecurityProperties securityProperties;
    
    @Autowired
    public S3Service(GoogleVisionService googleVisionService,
                    @Lazy PostRepository postRepository,
                    @Lazy CommentRepository commentRepository,
                    SecurityProperties securityProperties) {
        this.googleVisionService = googleVisionService;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.securityProperties = securityProperties;
    }

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    
    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;
    
    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;
    
    @Value("${cloud.aws.region.static}")
    private String region;
    
    private String getBucketName() {
        return bucketName;
    }
    
    private String getAccessKey() {
        return accessKey;
    }
    
    private String getSecretKey() {
        return secretKey;
    }
    
    private String getRegion() {
        return region;
    }

    public Map<String, String> generatePresignedUrl(String originalFilename, String contentTypeParam) {
        if (!isValidImageFile(originalFilename)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp, svg, bmp 형식만 허용)");
        }
        try {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(getAccessKey(), getSecretKey());
            S3Presigner presigner = S3Presigner.builder()
                    .region(Region.of(getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();

            String s3Key = generateS3Key(originalFilename);
            String contentType = contentTypeParam != null ? contentTypeParam : getContentType(originalFilename);

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .putObjectRequest(builder -> builder
                            .bucket(getBucketName())
                            .key(s3Key)
                            .contentType(contentType)
                    )
                    .build();

            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            String permanentUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    getBucketName(), getRegion(), s3Key);

            Map<String, String> result = new HashMap<>();
            result.put("uploadUrl", presignedUrl);
            result.put("url", permanentUrl);
            result.put("s3Key", s3Key);
            result.put("originalName", originalFilename);
            result.put("contentType", contentType);

            log.info("Presigned URL 생성 완료: {} (영구 URL: {}, Content-Type: {})", s3Key, permanentUrl, contentType);
            return result;

        } catch (Exception e) {
            log.error("Presigned URL 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("파일 업로드 URL 생성에 실패했습니다.", e);
        }
    }
    
    public Map<String, String> generatePresignedUrl(String originalFilename) {
        return generatePresignedUrl(originalFilename, null);
    }

    public void deleteObject(String s3Key) {
        try {
            S3Client s3Client = createS3Client();
            s3Client.deleteObject(builder -> builder
                    .bucket(getBucketName())
                    .key(s3Key)
            );
            log.info("S3 객체 삭제 완료: {}", s3Key);
        } catch (Exception e) {
            log.error("S3 객체 삭제 실패: {}", s3Key, e);
        }
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

    public Map<String, String> uploadImageWithValidation(org.springframework.web.multipart.MultipartFile file) {
        try {
            if (!isValidImageFile(file.getOriginalFilename())) {
                throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp, svg, bmp 형식만 허용)");
            }
            
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
            }
            
            byte[] imageBytes = file.getBytes();
            boolean isSafe = googleVisionService.isImageSafe(imageBytes);
            
            if (!isSafe) {
                log.warn("부적절한 이미지 업로드 시도 차단: {}", file.getOriginalFilename());
                throw new IllegalArgumentException("부적절한 콘텐츠가 포함된 이미지입니다. 업로드가 거부되었습니다.");
            }
            
            String s3Key = generateS3Key(file.getOriginalFilename());
            String contentType = getContentType(file.getOriginalFilename());
            
            S3Client s3Client = createS3Client();
            
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(s3Key)
                    .contentType(contentType)
                    .build();
            
            s3Client.putObject(putRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(imageBytes));
            
            String permanentUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    getBucketName(), getRegion(), s3Key);
            
            Map<String, String> result = new HashMap<>();
            result.put("url", permanentUrl);
            result.put("s3Key", s3Key);
            result.put("originalName", file.getOriginalFilename());
            result.put("contentType", contentType);
            result.put("size", String.valueOf(file.getSize()));
            result.put("validated", "true");
            
            log.info("✅ 이미지 검열 통과 및 업로드 완료: {} -> {}", file.getOriginalFilename(), s3Key);
            return result;
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("이미지 업로드 및 검열 실패: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
        }
    }

    public void checkImageInBackground(String imageUrl) {
        // 백그라운드 검사는 간단하게 로그만 남김
        log.info("이미지 백그라운드 검사: {}", imageUrl);
    }

    public int getOrphanImageCount() {
        try {
            S3Client s3Client = createS3Client();
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(getBucketName())
                    .prefix("uploads/")
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
            List<S3Object> objects = response.contents();

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
            long orphanCount = objects.stream()
                    .filter(obj -> obj.lastModified().isBefore(cutoffDate.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                    .filter(this::isOrphanFile)
                    .count();

            return (int) orphanCount;
        } catch (Exception e) {
            log.error("고아 이미지 개수 조회 실패", e);
            return 0;
        }
    }

    public int cleanupOrphanImages() {
        try {
            S3Client s3Client = createS3Client();
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(getBucketName())
                    .prefix("uploads/")
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
            List<S3Object> objects = response.contents();

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
            List<S3Object> orphanObjects = objects.stream()
                    .filter(obj -> obj.lastModified().isBefore(cutoffDate.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                    .filter(this::isOrphanFile)
                    .collect(Collectors.toList());

            int deletedCount = 0;
            for (S3Object obj : orphanObjects) {
                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(getBucketName())
                            .key(obj.key())
                            .build());
                    deletedCount++;
                    log.info("🗑️ 고아 이미지 삭제: {}", obj.key());
                } catch (Exception e) {
                    log.error("삭제 실패: {}", obj.key(), e);
                }
            }
            return deletedCount;
        } catch (Exception e) {
            log.error("고아 이미지 정리 실패", e);
            return 0;
        }
    }

    public Map<String, Object> getS3Status() {
        Map<String, Object> status = new HashMap<>();
        try {
            status.put("bucketName", getBucketName());
            status.put("region", getRegion());
            status.put("accessKeyConfigured", getAccessKey() != null && !getAccessKey().trim().isEmpty());
            status.put("secretKeyConfigured", getSecretKey() != null && !getSecretKey().trim().isEmpty());
            status.put("connectionStatus", "OK");
        } catch (Exception e) {
            status.put("connectionStatus", "ERROR");
            status.put("error", e.getMessage());
        }
        return status;
    }

    private S3Client createS3Client() {
        try {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(getAccessKey(), getSecretKey());
            return S3Client.builder()
                    .region(Region.of(getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
        } catch (Exception e) {
            log.error("S3 클라이언트 생성 실패", e);
            throw new RuntimeException("S3 연결에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private boolean isOrphanFile(S3Object s3Object) {
        try {
            String s3Key = s3Object.key();
            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", getBucketName(), getRegion(), s3Key);
            
            boolean usedInPosts = postRepository.existsByContentContaining(fileUrl) || 
                                postRepository.existsByContentContaining(s3Key);
            
            if (usedInPosts) {
                return false;
            }
            
            boolean usedInComments = commentRepository.existsByContentContaining(fileUrl) ||
                                   commentRepository.existsByContentContaining(s3Key);
            
            return !usedInComments;
        } catch (Exception e) {
            log.warn("고아 파일 검증 중 오류: {}", s3Object.key(), e);
            return false;
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

    private String getContentType(String filename) {
        String extension = filename.toLowerCase();
        if (extension.endsWith(".jpg") || extension.endsWith(".jpeg")) return "image/jpeg";
        if (extension.endsWith(".png")) return "image/png";
        if (extension.endsWith(".gif")) return "image/gif";
        if (extension.endsWith(".webp")) return "image/webp";
        if (extension.endsWith(".svg")) return "image/svg+xml";
        if (extension.endsWith(".bmp")) return "image/bmp";
        return "application/octet-stream";
    }

    private boolean isValidImageFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) return false;
        String extension = filename.toLowerCase();
        return extension.endsWith(".jpg") || extension.endsWith(".jpeg") ||
                extension.endsWith(".png") || extension.endsWith(".gif") ||
                extension.endsWith(".webp") || extension.endsWith(".svg") ||
                extension.endsWith(".bmp");
    }
}