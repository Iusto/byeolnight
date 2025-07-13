package com.byeolnight.service.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AWS S3 파일 업로드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {
    
    @PostConstruct
    public void init() {
        ensureBucketPublicReadAccess();
    }
    
    private final GoogleVisionService googleVisionService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    /**
     * S3 Presigned URL 생성 (이미지 검증 포함)
     */
    public Map<String, String> generatePresignedUrl(String originalFilename) {
        // 기본적인 파일 형식 검증
        if (!isValidImageFile(originalFilename)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, png, gif, webp만 허용)");
        }
        try {
            // AWS 자격 증명 설정
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            
            // S3 Presigner 생성
            S3Presigner presigner = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();

            // 고유한 파일명 생성
            String s3Key = generateS3Key(originalFilename);

            // Presigned URL 요청 생성 (업로드용)
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10)) // 업로드용 10분
                    .putObjectRequest(builder -> builder
                            .bucket(bucketName)
                            .key(s3Key)
                            .contentType(getContentType(originalFilename))
                    )
                    .build();

            // Presigned URL 생성 (업로드용)
            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();
            
            // 영구 접근 URL 생성 (조회용)
            String permanentUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", 
                    bucketName, region, s3Key);

            // 결과 반환
            Map<String, String> result = new HashMap<>();
            result.put("uploadUrl", presignedUrl);  // 업로드용 URL
            result.put("url", permanentUrl);       // 영구 접근 URL
            result.put("s3Key", s3Key);
            result.put("originalName", originalFilename);

            log.info("Presigned URL 생성 완료: {} (영구 URL: {})", s3Key, permanentUrl);
            return result;

        } catch (Exception e) {
            log.error("Presigned URL 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("파일 업로드 URL 생성에 실패했습니다.", e);
        }
    }

    /**
     * S3 키 생성 (고유한 파일명)
     */
    private String generateS3Key(String originalFilename) {
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }
        
        return "uploads/" + UUID.randomUUID().toString() + extension;
    }

    /**
     * S3 객체 삭제
     */
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

    /**
     * Presigned Upload URL 생성 (기존 메서드와 호환성을 위해)
     */
    public String generatePresignedUploadUrl(String filename) {
        try {
            Map<String, String> result = generatePresignedUrl(filename);
            return result.get("url");
        } catch (Exception e) {
            log.error("Presigned Upload URL 생성 실패: {}", filename, e);
            throw new RuntimeException("파일 업로드 URL 생성에 실패했습니다.", e);
        }
    }

    /**
     * 파일 확장자에 따른 Content-Type 결정
     */
    private String getContentType(String filename) {
        String extension = filename.toLowerCase();
        if (extension.endsWith(".jpg") || extension.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (extension.endsWith(".png")) {
            return "image/png";
        } else if (extension.endsWith(".gif")) {
            return "image/gif";
        } else if (extension.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
    
    /**
     * 업로드된 이미지 검열 (Google Vision API)
     */
    public boolean validateUploadedImage(byte[] imageBytes) {
        try {
            boolean isSafe = googleVisionService.isImageSafe(imageBytes);
            log.info("이미지 검열 결과: {}", isSafe ? "안전" : "부적절");
            return isSafe;
        } catch (Exception e) {
            log.error("이미지 검열 중 오류 발생", e);
            return true; // 오류 시 기본적으로 허용
        }
    }
    
    /**
     * 이미지 파일 형식 검증
     */
    private boolean isValidImageFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        
        String extension = filename.toLowerCase();
        return extension.endsWith(".jpg") || 
               extension.endsWith(".jpeg") || 
               extension.endsWith(".png") || 
               extension.endsWith(".gif") || 
               extension.endsWith(".webp");
    }
    
    /**
     * S3 버킷 공개 읽기 권한 확인
     */
    public void ensureBucketPublicReadAccess() {
        log.info("S3 버킷 공개 읽기 권한 확인: {}", bucketName);
        log.info("영구 URL 형식: https://{}.s3.{}.amazonaws.com/uploads/[filename]", bucketName, region);
        
        // AWS 콘솔에서 다음 버킷 정책을 설정해야 합니다:
        // {
        //   "Version": "2012-10-17",
        //   "Statement": [
        //     {
        //       "Sid": "PublicReadGetObject",
        //       "Effect": "Allow",
        //       "Principal": "*",
        //       "Action": "s3:GetObject",
        //       "Resource": "arn:aws:s3:::YOUR_BUCKET_NAME/uploads/*"
        //     }
        //   ]
        // }
    }
}