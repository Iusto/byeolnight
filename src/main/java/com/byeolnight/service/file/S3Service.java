package com.byeolnight.service.file;

import com.byeolnight.entity.file.File;
import com.byeolnight.entity.file.FileStatus;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.infrastructure.config.SecurityProperties;
import com.byeolnight.repository.file.FileRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.dto.file.S3StatusDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import com.byeolnight.dto.file.PresignedUrlResponseDto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AWS S3 파일 업로드/관리 핵심 서비스
 * 
 * 아키텍처:
 * - 업로드: Presigned S3 URL (클라이언트 → S3 직접)
 * - 조회: CloudFront URL (클라이언트 → CloudFront → S3)
 * 
 * 주요 기능:
 * - Presigned URL 생성 (10분 유효)
 * - Google Vision API 이미지 검열
 * - 고아 파일 자동 정리
 * - S3 연결 상태 모니터링
 * 
 * @author byeolnight
 * @since 1.0
 */
@Slf4j
@Service
public class S3Service {

    private final GoogleVisionService googleVisionService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final FileRepository fileRepository;
    private final SecurityProperties securityProperties;

    @Autowired
    public S3Service(GoogleVisionService googleVisionService,
                    @Lazy PostRepository postRepository,
                    @Lazy CommentRepository commentRepository,
                    @Lazy FileRepository fileRepository,
                    SecurityProperties securityProperties) {
        this.googleVisionService = googleVisionService;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.fileRepository = fileRepository;
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

    /**
     * S3 Presigned URL 생성 (클라이언트 직접 업로드용)
     *
     * 플로우:
     * 1. 파일 확장자 검증 (jpg, png, gif 등)
     * 2. S3 Presigned URL 생성 (10분 유효)
     * 3. File 테이블에 PENDING 상태로 저장
     * 4. CloudFront URL 반환 (조회용)
     *
     * @param originalFilename 원본 파일명
     * @param contentTypeParam 콘텐츠 타입 (선택적)
     * @return uploadUrl(업로드용), url(조회용), s3Key 등 포함
     */
    @Transactional
    public PresignedUrlResponseDto generatePresignedUrl(String originalFilename, String contentTypeParam) {
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

            // File 테이블에 PENDING 상태로 저장 (고아 파일 추적용)
            File pendingFile = File.createPending(originalFilename, s3Key, permanentUrl);
            fileRepository.save(pendingFile);
            log.debug("PENDING 상태 파일 저장: s3Key={}", s3Key);

            log.info("Presigned URL 생성 완료: {} (영구 URL: {}, Content-Type: {})", s3Key, permanentUrl, contentType);
            return PresignedUrlResponseDto.of(presignedUrl, permanentUrl, s3Key, originalFilename, contentType);

        } catch (Exception e) {
            log.error("Presigned URL 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("파일 업로드 URL 생성에 실패했습니다.", e);
        }
    }

    public PresignedUrlResponseDto generatePresignedUrl(String originalFilename) {
        return generatePresignedUrl(originalFilename, null);
    }

    /**
     * S3 객체 삭제
     * 
     * @param s3Key S3 객체 키
     */
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

    /**
     * 업로드된 이미지 검열 (Google Vision API)
     * 
     * @param imageBytes 이미지 바이트 데이터
     * @return true: 안전한 이미지, false: 부적절한 이미지
     */
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
     * 고아 이미지 개수 조회 (File 테이블 기반)
     *
     * 고아 이미지: PENDING 상태이고 7일 이상 경과한 파일
     *
     * @return 고아 이미지 개수
     */
    public int getOrphanImageCount() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
            long orphanCount = fileRepository.countByStatusAndCreatedAtBefore(FileStatus.PENDING, cutoffDate);
            return (int) orphanCount;
        } catch (Exception e) {
            log.error("고아 이미지 개수 조회 실패", e);
            return 0;
        }
    }

    /**
     * 고아 이미지 자동 정리 (File 테이블 기반)
     *
     * PENDING 상태이고 7일 이상 경과한 파일을 S3에서 삭제하고 DB에서도 제거
     *
     * @return 삭제된 이미지 개수
     */
    @Transactional
    public int cleanupOrphanImages() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
            List<File> orphanFiles = fileRepository.findByStatusAndCreatedAtBefore(FileStatus.PENDING, cutoffDate);

            if (orphanFiles.isEmpty()) {
                log.info("정리할 고아 파일이 없습니다.");
                return 0;
            }

            S3Client s3Client = createS3Client();
            int deletedCount = 0;

            for (File orphanFile : orphanFiles) {
                try {
                    // S3에서 파일 삭제
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(getBucketName())
                            .key(orphanFile.getS3Key())
                            .build());

                    // DB에서 파일 레코드 삭제
                    fileRepository.delete(orphanFile);

                    deletedCount++;
                    log.info("🗑️ 고아 이미지 삭제: {} (id={})", orphanFile.getS3Key(), orphanFile.getId());
                } catch (Exception e) {
                    log.error("삭제 실패: {} (id={})", orphanFile.getS3Key(), orphanFile.getId(), e);
                }
            }

            log.info("고아 이미지 정리 완료: {}개 삭제", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("고아 이미지 정리 실패", e);
            return 0;
        }
    }

    /**
     * 파일 상태를 CONFIRMED로 변경
     *
     * @param s3Key S3 키
     */
    @Transactional
    public void confirmFile(String s3Key) {
        fileRepository.findByS3Key(s3Key).ifPresent(file -> {
            file.confirm();
            log.debug("파일 상태 CONFIRMED로 변경: s3Key={}", s3Key);
        });
    }

    /**
     * URL로 파일 상태를 CONFIRMED로 변경
     *
     * @param url 파일 URL
     */
    @Transactional
    public void confirmFileByUrl(String url) {
        fileRepository.findByUrl(url).ifPresent(file -> {
            file.confirm();
            log.debug("파일 상태 CONFIRMED로 변경: url={}", url);
        });
    }

    /**
     * S3 연결 상태 및 설정 검증
     * 
     * 검증 항목:
     * - AWS 자격 증명 설정 여부
     * - 버킷 존재 여부
     * - 리전 설정 일치 여부
     * - 권한 확인
     * 
     * @return S3 상태 정보
     */
    public S3StatusDto getS3Status() {
        S3StatusDto.S3StatusDtoBuilder statusBuilder = S3StatusDto.builder();
        
        try {
            // 기본 설정 정보
            statusBuilder.bucketName(getBucketName()).configuredRegion(getRegion());
            
            // 자격 증명 확인
            boolean accessKeyConfigured = getAccessKey() != null && !getAccessKey().trim().isEmpty();
            boolean secretKeyConfigured = getSecretKey() != null && !getSecretKey().trim().isEmpty();
            
            if (!accessKeyConfigured || !secretKeyConfigured) {
                return statusBuilder
                        .connectionStatus(S3StatusDto.ConnectionStatus.ERROR)
                        .bucketExists(false)
                        .regionMatch(false)
                        .error("AWS 자격 증명이 설정되지 않았습니다.")
                        .suggestion("application.yml에서 AWS Access Key와 Secret Key를 확인해주세요.")
                        .build();
            }
            
            // S3 클라이언트로 실제 연결 테스트
            S3Client s3Client = createS3Client();
            
            // 버킷 존재 여부 확인
            try {
                HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                        .bucket(getBucketName())
                        .build();
                s3Client.headBucket(headBucketRequest);
                
                statusBuilder.connectionStatus(S3StatusDto.ConnectionStatus.SUCCESS).bucketExists(true);
                
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
                    
                    boolean regionMatch = actualRegion.equals(getRegion());
                    statusBuilder.actualRegion(actualRegion).regionMatch(regionMatch);
                    
                    if (!regionMatch) {
                        statusBuilder.warning(String.format("설정된 리전(%s)과 실제 버킷 리전(%s)이 다릅니다.", getRegion(), actualRegion))
                                     .suggestion("application.yml의 cloud.aws.region.static 설정을 " + actualRegion + "으로 변경해주세요.");
                    }
                    
                } catch (S3Exception regionError) {
                    if (regionError.statusCode() == 403) {
                        log.info("s3:GetBucketLocation 권한 없음 - 설정된 리전 사용: {}", getRegion());
                        statusBuilder.actualRegion("권한 없음 (설정값 사용)")
                                     .regionMatch(true)
                                     .info("리전 조회 권한이 없어 설정된 리전을 사용합니다.");
                    } else {
                        log.warn("버킷 리전 조회 실패: {}", regionError.getMessage());
                        statusBuilder.actualRegion("조회 실패").regionMatch(false).warning("버킷 리전을 확인할 수 없습니다.");
                    }
                } catch (Exception regionError) {
                    log.warn("버킷 리전 조회 실패: {}", regionError.getMessage());
                    statusBuilder.actualRegion("조회 실패").regionMatch(false).warning("버킷 리전을 확인할 수 없습니다.");
                }
                
            } catch (NoSuchBucketException e) {
                statusBuilder.connectionStatus(S3StatusDto.ConnectionStatus.ERROR)
                             .bucketExists(false)
                             .regionMatch(false)
                             .error("지정된 S3 버킷이 존재하지 않습니다.")
                             .suggestion("AWS 콘솔에서 " + getBucketName() + " 버킷을 생성하거나 올바른 버킷명을 설정해주세요.");
                
            } catch (S3Exception e) {
                statusBuilder.connectionStatus(S3StatusDto.ConnectionStatus.ERROR)
                             .bucketExists(false)
                             .regionMatch(false);
                
                if (e.statusCode() == 403) {
                    statusBuilder.error("S3 버킷에 대한 접근 권한이 없습니다.")
                                 .suggestion("IAM 정책에서 s3:HeadBucket, s3:GetBucketLocation 권한을 확인해주세요.");
                } else {
                    statusBuilder.error("S3 연결 오류: " + e.getMessage())
                                 .suggestion("AWS 자격 증명과 리전 설정을 확인해주세요.");
                }
            }
            
        } catch (Exception e) {
            log.error("S3 상태 확인 중 오류 발생", e);
            statusBuilder.connectionStatus(S3StatusDto.ConnectionStatus.ERROR)
                         .bucketExists(false)
                         .regionMatch(false)
                         .error("S3 상태 확인 실패: " + e.getMessage())
                         .suggestion("AWS 설정을 확인하고 네트워크 연결을 점검해주세요.");
        }
        
        return statusBuilder.build();
    }

    /**
     * S3 클라이언트 생성
     * 
     * @return 설정된 자격 증명과 리전으로 생성된 S3 클라이언트
     */
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