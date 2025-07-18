package com.byeolnight.service.file;

import com.byeolnight.domain.repository.comment.CommentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import org.springframework.context.annotation.Lazy;

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
    private final com.byeolnight.domain.repository.post.PostRepository postRepository;
    private final CommentRepository commentRepository;
    
    public S3Service(GoogleVisionService googleVisionService,
                    @Lazy com.byeolnight.domain.repository.post.PostRepository postRepository,
                    @Lazy CommentRepository commentRepository) {
        this.googleVisionService = googleVisionService;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    public Map<String, String> generatePresignedUrl(String originalFilename, String contentTypeParam) {
        ensureBucketPublicReadAccess();
        if (!isValidImageFile(originalFilename)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp, svg, bmp 형식만 허용)");
        }
        try {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            S3Presigner presigner = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();

            String s3Key = generateS3Key(originalFilename);
            String contentType = contentTypeParam != null ? contentTypeParam : getContentType(originalFilename);

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .putObjectRequest(builder -> builder
                            .bucket(bucketName)
                            .key(s3Key)
                            .contentType(contentType)
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
    
    /**
     * 이전 버전과의 호환성을 위한 메서드
     */
    public Map<String, String> generatePresignedUrl(String originalFilename) {
        return generatePresignedUrl(originalFilename, null);
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
            S3Client s3Client = createS3Client();

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
        if (extension.endsWith(".svg")) return "image/svg+xml";
        if (extension.endsWith(".bmp")) return "image/bmp";
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

    /**
     * 이미지 파일을 직접 업로드하고 Google Vision API로 검열
     */
    public Map<String, String> uploadImageWithValidation(org.springframework.web.multipart.MultipartFile file) {
        try {
            // 1. 파일 유효성 검사
            if (!isValidImageFile(file.getOriginalFilename())) {
                throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp, svg, bmp 형식만 허용)");
            }
            
            // 2. 파일 크기 검사 (10MB 제한)
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
            }
            
            // 3. Google Vision API로 이미지 검열
            byte[] imageBytes = file.getBytes();
            boolean isSafe = googleVisionService.isImageSafe(imageBytes);
            
            if (!isSafe) {
                log.warn("부적절한 이미지 업로드 시도 차단: {}", file.getOriginalFilename());
                throw new IllegalArgumentException("부적절한 콘텐츠가 포함된 이미지입니다. 업로드가 거부되었습니다.");
            }
            
            // 4. S3에 업로드
            String s3Key = generateS3Key(file.getOriginalFilename());
            String contentType = getContentType(file.getOriginalFilename());
            
            S3Client s3Client = createS3Client();
            
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();
            
            s3Client.putObject(putRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(imageBytes));
            
            String permanentUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName, region, s3Key);
            
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
    
    /**
     * URL로 이미지를 다운로드하여 검증하고, 부적절한 경우 자동 삭제
     * @param imageUrl 검증할 이미지 URL
     * @return 이미지가 안전한지 여부
     */
    public boolean validateImageByUrl(String imageUrl) {
        try {
            if (!imageUrl.contains(bucketName)) {
                throw new IllegalArgumentException("유효하지 않은 이미지 URL입니다.");
            }
            
            // URL에서 S3 키 추출
            String s3Key = extractS3KeyFromUrl(imageUrl);
            if (s3Key == null) {
                log.warn("이미지 URL에서 S3 키를 추출할 수 없습니다: {}", imageUrl);
                return false;
            }
            
            // 이미지 다운로드
            java.net.URL url = new java.net.URL(imageUrl);
            java.io.InputStream inputStream = url.openStream();
            byte[] imageBytes = inputStream.readAllBytes();
            inputStream.close();
            
            // Google Vision API로 검증
            boolean isSafe = googleVisionService.isImageSafe(imageBytes);
            log.info("URL 이미지 검증 결과: {} -> {}", imageUrl, isSafe ? "안전" : "부적절");
            
            // 부적절한 이미지인 경우 자동 삭제
            if (!isSafe) {
                log.warn("부적절한 이미지 감지: {} - 자동 삭제 시작", s3Key);
                deleteObject(s3Key);
                log.info("부적절한 이미지 삭제 완료: {}", s3Key);
            }
            
            return isSafe;
            
        } catch (Exception e) {
            log.error("URL 이미지 검증 실패: {}", imageUrl, e);
            return false;
        }
    }
    
    /**
     * 이미지 URL에서 S3 키 추출
     */
    private String extractS3KeyFromUrl(String imageUrl) {
        try {
            // https://bucket-name.s3.region.amazonaws.com/uploads/filename.jpg 형식에서 추출
            String pattern = "https://.*\\.amazonaws\\.com/(.*)";
            java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = r.matcher(imageUrl);
            
            if (m.find()) {
                return m.group(1); // uploads/filename.jpg 부분 반환
            }
            return null;
        } catch (Exception e) {
            log.error("S3 키 추출 실패: {}", imageUrl, e);
            return null;
        }
    }

    private boolean isValidImageFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) return false;
        String extension = filename.toLowerCase();
        return extension.endsWith(".jpg") || extension.endsWith(".jpeg") ||
                extension.endsWith(".png") || extension.endsWith(".gif") ||
                extension.endsWith(".webp") || extension.endsWith(".svg") ||
                extension.endsWith(".bmp");
    }

    public void ensureBucketPublicReadAccess() {
        log.info("S3 버킷 공개 읽기 권한 확인: {}", bucketName);
        log.info("영구 URL 형식: https://{}.s3.{}.amazonaws.com/uploads/[filename]", bucketName, region);
        setupLifecyclePolicy();
    }

    private void setupLifecyclePolicy() {
        try {
            S3Client s3Client = createS3Client();

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
        } catch (S3Exception e) {
            if (e.statusCode() == 403 && e.getMessage().contains("PutLifecycleConfiguration")) {
                log.debug("S3 Lifecycle 정책 권한 없음 (관리자 대시보드에서 수동 정리 가능)");
            } else {
                log.warn("⚠️ S3 Lifecycle 정책 설정 실패: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.warn("⚠️ S3 Lifecycle 정책 설정 실패: {}", e.getMessage());
        }
    }

    public int cleanupOrphanImages() {
        try {
            // 실제 버킷 리전 찾기
            String actualRegion = findBucketRegion();
            S3Client s3Client = createS3Client(actualRegion);

            // S3에서 모든 uploads/ 파일 조회
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix("uploads/")
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
            List<S3Object> objects = response.contents();
            log.info("S3에서 총 {}개의 파일 발견", objects.size());

            // 7일 이상 된 파일만 필터링
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
            List<S3Object> oldObjects = objects.stream()
                    .filter(obj -> obj.lastModified().isBefore(cutoffDate.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                    .collect(Collectors.toList());
            
            log.info("7일 이상 된 파일: {}개", oldObjects.size());

            // 실제 고아 파일 검증 (DB에서 사용 중인지 확인)
            List<S3Object> orphanObjects = oldObjects.stream()
                    .filter(this::isOrphanFile)
                    .collect(Collectors.toList());
            
            log.info("실제 고아 파일: {}개", orphanObjects.size());

            int deletedCount = 0;
            for (S3Object obj : orphanObjects) {
                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(obj.key())
                            .build());
                    deletedCount++;
                    log.info("🗑️ 고아 이미지 삭제: {} (크기: {}KB, 수정일: {})", 
                        obj.key(), 
                        obj.size() / 1024,
                        obj.lastModified());
                } catch (Exception e) {
                    log.error("삭제 실패: {}", obj.key(), e);
                }
            }
            log.info("🧹 고아 이미지 정리 완료: {}개 삭제 (총 용량 절약: {}MB)", 
                deletedCount, 
                orphanObjects.stream().mapToLong(S3Object::size).sum() / (1024 * 1024));
            return deletedCount;
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            if (e.statusCode() == 301) {
                log.error("S3 리전 오류 - 버킷: {}, 설정된 리전: {}, 오류: 올바른 엔드포인트를 사용해야 합니다.", bucketName, region);
                log.info("해결 방법: 1) 버킷이 올바른 리전에 있는지 확인 2) AWS 자격 증명 확인");
            } else {
                log.error("S3 연결 오류 - 버킷: {}, 리전: {}, 상태코드: {}, 오류: {}", 
                    bucketName, region, e.statusCode(), e.getMessage());
            }
            return 0;
        } catch (RuntimeException e) {
            log.error("S3 설정 오류: {}", e.getMessage());
            return 0;
        } catch (Exception e) {
            log.error("고아 이미지 정리 실패", e);
            return 0;
        }
    }

    public int getOrphanImageCount() {
        try {
            // 실제 버킷 리전 찾기
            String actualRegion = findBucketRegion();
            S3Client s3Client = createS3Client(actualRegion);

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix("uploads/")
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
            List<S3Object> objects = response.contents();

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
            long orphanCount = objects.stream()
                    .filter(obj -> obj.lastModified().isBefore(cutoffDate.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                    .filter(this::isOrphanFile)
                    .count();

            log.info("실제 고아 이미지 개수: {}개 (전체 파일: {}개, 7일 이상: {}개)", 
                orphanCount, 
                objects.size(),
                objects.stream().filter(obj -> obj.lastModified().isBefore(cutoffDate.atZone(java.time.ZoneId.systemDefault()).toInstant())).count());
            return (int) orphanCount;
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            if (e.statusCode() == 301) {
                log.error("S3 리전 오류 - 버킷: {}, 설정된 리전: {}, 오류: 올바른 엔드포인트를 사용해야 합니다.", bucketName, region);
                log.info("해결 방법: 1) 버킷이 올바른 리전에 있는지 확인 2) AWS 자격 증명 확인");
            } else {
                log.error("S3 연결 오류 - 버킷: {}, 리전: {}, 상태코드: {}, 오류: {}", 
                    bucketName, region, e.statusCode(), e.getMessage());
            }
            return 0;
        } catch (RuntimeException e) {
            log.error("S3 설정 오류: {}", e.getMessage());
            return 0;
        } catch (Exception e) {
            log.error("고아 이미지 개수 조회 실패", e);
            return 0;
        }
    }

    /**
     * S3 클라이언트 생성 (공통 메서드)
     */
    private S3Client createS3Client() {
        return createS3Client(region);
    }
    
    /**
     * 지정된 리전으로 S3 클라이언트 생성
     */
    private S3Client createS3Client(String targetRegion) {
        try {
            // AWS 자격 증명 검증
            if (accessKey == null || accessKey.trim().isEmpty() || 
                secretKey == null || secretKey.trim().isEmpty()) {
                log.warn("AWS S3 자격 증명이 설정되지 않았습니다.");
                throw new RuntimeException("AWS S3 자격 증명이 설정되지 않았습니다.");
            }
            
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            return S3Client.builder()
                    .region(Region.of(targetRegion))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .forcePathStyle(true)
                    .build();
        } catch (Exception e) {
            log.error("S3 클라이언트 생성 실패 - 버킷: {}, 리전: {}, 오류: {}", bucketName, targetRegion, e.getMessage());
            throw new RuntimeException("S3 연결에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 버킷의 실제 리전 찾기
     */
    private String findBucketRegion() {
        String[] commonRegions = {
            "us-east-1", "us-west-2", "ap-northeast-2", "eu-west-1", "ap-southeast-1"
        };
        
        for (String testRegion : commonRegions) {
            try {
                S3Client testClient = createS3Client(testRegion);
                testClient.headBucket(builder -> builder.bucket(bucketName));
                log.info("버킷 리전 발견: {} -> {}", bucketName, testRegion);
                return testRegion;
            } catch (Exception e) {
                log.debug("리전 {} 테스트 실패: {}", testRegion, e.getMessage());
            }
        }
        
        return region; // 기본 리전 반환
    }

    /**
     * 파일이 실제로 고아 파일인지 확인
     * DB에서 해당 파일을 사용하고 있는지 검사
     */
    private boolean isOrphanFile(S3Object s3Object) {
        try {
            String s3Key = s3Object.key();
            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
            
            // 1. 게시글 내용에서 해당 파일 URL이 사용되고 있는지 확인
            boolean usedInPosts = postRepository.existsByContentContaining(fileUrl) || 
                                postRepository.existsByContentContaining(s3Key);
            
            if (usedInPosts) {
                log.debug("파일이 게시글에서 사용 중: {}", s3Key);
                return false; // 사용 중이므로 고아 파일이 아님
            }
            
            // 2. 댓글 내용에서 해당 파일 URL이 사용되고 있는지 확인
            boolean usedInComments = commentRepository.existsByContentContaining(fileUrl) ||
                                   commentRepository.existsByContentContaining(s3Key);
            
            if (usedInComments) {
                log.debug("파일이 댓글에서 사용 중: {}", s3Key);
                return false; // 사용 중이므로 고아 파일이 아님
            }
            
            log.debug("고아 파일 확인: {} -> 사용되지 않는 파일", s3Key);
            return true; // DB에서 사용되지 않는 고아 파일
        } catch (Exception e) {
            log.warn("고아 파일 검증 중 오류: {}", s3Object.key(), e);
            return false; // 오류 시 안전하게 삭제하지 않음
        }
    }
    
    /**
     * S3 연결 상태 확인
     */
    public Map<String, Object> getS3Status() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // 기본 설정 정보
            status.put("bucketName", bucketName);
            status.put("configuredRegion", region);
            status.put("accessKeyConfigured", accessKey != null && !accessKey.trim().isEmpty());
            status.put("secretKeyConfigured", secretKey != null && !secretKey.trim().isEmpty());
            
            // 자동 리전 감지 시도
            String actualRegion = findBucketRegion();
            status.put("actualRegion", actualRegion);
            status.put("regionMatch", region.equals(actualRegion));
            
            // 실제 리전으로 S3 연결 테스트
            S3Client s3Client = createS3Client(actualRegion);
            
            // 버킷 존재 여부 확인
            try {
                s3Client.headBucket(builder -> builder.bucket(bucketName));
                status.put("bucketExists", true);
                status.put("connectionStatus", "SUCCESS");
                
                // 파일 개수 조회 테스트
                ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix("uploads/")
                        .maxKeys(1)
                        .build();
                        
                ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
                status.put("canListObjects", true);
                status.put("totalObjects", response.keyCount());
                
                // 리전 불일치 경고
                if (!region.equals(actualRegion)) {
                    status.put("warning", "설정된 리전(" + region + ")과 실제 버킷 리전(" + actualRegion + ")이 다릅니다.");
                    status.put("suggestion", ".env 파일에서 CLOUD_AWS_REGION=" + actualRegion + "로 수정하세요.");
                }
                
            } catch (Exception e) {
                status.put("bucketExists", false);
                status.put("connectionStatus", "BUCKET_ERROR");
                status.put("error", e.getMessage());
            }
            
        } catch (Exception e) {
            status.put("connectionStatus", "CONNECTION_ERROR");
            status.put("error", e.getMessage());
            
            if (e.getMessage().contains("301")) {
                status.put("suggestion", "버킷이 다른 리전에 있습니다. 자동 감지를 시도해보세요.");
            } else if (e.getMessage().contains("403")) {
                status.put("suggestion", "AWS 자격 증명이 잘못되었거나 권한이 부족합니다.");
            } else if (e.getMessage().contains("404")) {
                status.put("suggestion", "버킷이 존재하지 않습니다. 버킷 이름을 확인해주세요.");
            }
        }
        
        return status;
    }
}
