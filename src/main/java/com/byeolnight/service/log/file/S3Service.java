package com.byeolnight.service.log.file;

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
    
    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudFrontDomain;
    
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

            // CloudFront URL 사용 (S3 직접 접근 차단으로 인한 AccessDenied 방지)
            String permanentUrl = String.format("https://%s/%s", cloudFrontDomain, s3Key);

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
            
            // CloudFront URL 사용 (S3 직접 접근 차단으로 인한 AccessDenied 방지)
            String permanentUrl = String.format("https://%s/%s", cloudFrontDomain, s3Key);
            
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
        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                log.warn("S3 ListBucket 권한 부족 - IAM 정책 확인 필요: {}", e.getMessage());
                return -1; // 권한 부족을 나타내는 특별한 값
            }
            log.error("고아 이미지 개수 조회 실패", e);
            return 0;
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
            // 기본 설정 정보
            status.put("bucketName", getBucketName());
            status.put("configuredRegion", getRegion());
            
            // 자격 증명 확인
            boolean accessKeyConfigured = getAccessKey() != null && !getAccessKey().trim().isEmpty();
            boolean secretKeyConfigured = getSecretKey() != null && !getSecretKey().trim().isEmpty();
            
            if (!accessKeyConfigured || !secretKeyConfigured) {
                status.put("connectionStatus", "ERROR");
                status.put("bucketExists", false);
                status.put("regionMatch", false);
                status.put("error", "AWS 자격 증명이 설정되지 않았습니다.");
                status.put("suggestion", "application.yml에서 AWS Access Key와 Secret Key를 확인해주세요.");
                return status;
            }
            
            // S3 클라이언트로 실제 연결 테스트
            S3Client s3Client = createS3Client();
            
            // 버킷 존재 여부 확인
            try {
                HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                        .bucket(getBucketName())
                        .build();
                s3Client.headBucket(headBucketRequest);
                
                status.put("connectionStatus", "SUCCESS");
                status.put("bucketExists", true);
                
                // 버킷의 실제 리전 확인
                try {
                    GetBucketLocationRequest locationRequest = GetBucketLocationRequest.builder()
                            .bucket(getBucketName())
                            .build();
                    GetBucketLocationResponse locationResponse = s3Client.getBucketLocation(locationRequest);
                    
                    String actualRegion = locationResponse.locationConstraintAsString();
                    // us-east-1의 경우 null이 반환될 수 있음
                    if (actualRegion == null || actualRegion.isEmpty()) {
                        actualRegion = "us-east-1";
                    }
                    
                    status.put("actualRegion", actualRegion);
                    status.put("regionMatch", actualRegion.equals(getRegion()));
                    
                    if (!actualRegion.equals(getRegion())) {
                        status.put("warning", String.format("설정된 리전(%s)과 실제 버킷 리전(%s)이 다릅니다.", getRegion(), actualRegion));
                        status.put("suggestion", "application.yml의 cloud.aws.region.static 설정을 " + actualRegion + "으로 변경해주세요.");
                    }
                    
                } catch (S3Exception regionError) {
                    if (regionError.statusCode() == 403) {
                        log.info("s3:GetBucketLocation 권한 없음 - 설정된 리전 사용: {}", getRegion());
                        status.put("actualRegion", "권한 없음 (설정값 사용)");
                        status.put("regionMatch", true);
                        status.put("info", "리전 조회 권한이 없어 설정된 리전을 사용합니다.");
                    } else {
                        log.warn("버킷 리전 조회 실패: {}", regionError.getMessage());
                        status.put("actualRegion", "조회 실패");
                        status.put("regionMatch", false);
                        status.put("warning", "버킷 리전을 확인할 수 없습니다.");
                    }
                } catch (Exception regionError) {
                    log.warn("버킷 리전 조회 실패: {}", regionError.getMessage());
                    status.put("actualRegion", "조회 실패");
                    status.put("regionMatch", false);
                    status.put("warning", "버킷 리전을 확인할 수 없습니다.");
                }
                
            } catch (NoSuchBucketException e) {
                status.put("connectionStatus", "ERROR");
                status.put("bucketExists", false);
                status.put("regionMatch", false);
                status.put("error", "지정된 S3 버킷이 존재하지 않습니다.");
                status.put("suggestion", "AWS 콘솔에서 " + getBucketName() + " 버킷을 생성하거나 올바른 버킷명을 설정해주세요.");
                
            } catch (S3Exception e) {
                status.put("connectionStatus", "ERROR");
                status.put("bucketExists", false);
                status.put("regionMatch", false);
                
                if (e.statusCode() == 403) {
                    status.put("error", "S3 버킷에 대한 접근 권한이 없습니다.");
                    status.put("suggestion", "IAM 정책에서 s3:HeadBucket, s3:GetBucketLocation 권한을 확인해주세요.");
                } else {
                    status.put("error", "S3 연결 오류: " + e.getMessage());
                    status.put("suggestion", "AWS 자격 증명과 리전 설정을 확인해주세요.");
                }
            }
            
        } catch (Exception e) {
            log.error("S3 상태 확인 중 오류 발생", e);
            status.put("connectionStatus", "ERROR");
            status.put("bucketExists", false);
            status.put("regionMatch", false);
            status.put("error", "S3 상태 확인 실패: " + e.getMessage());
            status.put("suggestion", "AWS 설정을 확인하고 네트워크 연결을 점검해주세요.");
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
            // CloudFront URL 사용 (S3 직접 URL과 함께 검사)
            String fileUrl = String.format("https://%s/%s", cloudFrontDomain, s3Key);
            String s3DirectUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", getBucketName(), getRegion(), s3Key);
            
            // CloudFront URL, S3 직접 URL, S3 키 모두 검사
            boolean usedInPosts = postRepository.existsByContentContaining(fileUrl) || 
                                postRepository.existsByContentContaining(s3DirectUrl) ||
                                postRepository.existsByContentContaining(s3Key);
            
            if (usedInPosts) {
                return false;
            }
            
            boolean usedInComments = commentRepository.existsByContentContaining(fileUrl) ||
                                   commentRepository.existsByContentContaining(s3DirectUrl) ||
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