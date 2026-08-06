package com.byeolnight.service.file;

import com.byeolnight.entity.file.File;
import com.byeolnight.entity.file.FileStatus;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.infrastructure.config.SecurityProperties;
import com.byeolnight.repository.file.FileRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.dto.file.S3StatusDto;
import com.byeolnight.dto.file.UploadedImageResponseDto;
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
import software.amazon.awssdk.core.sync.RequestBody;

import com.byeolnight.infrastructure.exception.FileProcessingException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AWS S3 이미지 저장 및 관리 서비스.
 * 안전성 검열을 통과한 이미지 바이트만 공개 경로에 저장한다.
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
     * 이미지 검열이 끝난 뒤에만 S3에 업로드하여 검열 전 공개 가능성을 차단한다.
     */
    public UploadedImageResponseDto uploadModeratedImage(
            String originalFilename,
            byte[] imageBytes
    ) {
        if (!validateUploadedImage(imageBytes)) {
            throw new IllegalArgumentException("부적절하거나 검열할 수 없는 이미지입니다.");
        }

        String s3Key = generateS3Key(originalFilename);
        String storedContentType = getContentType(originalFilename);
        String permanentUrl = String.format("https://%s/%s", cloudFrontDomain, s3Key);
        boolean uploaded = false;

        try (S3Client s3Client = createS3Client()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(s3Key)
                    .contentType(storedContentType)
                    .contentLength((long) imageBytes.length)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));
            uploaded = true;

            File pendingFile = File.createPending(originalFilename, s3Key, permanentUrl);
            fileRepository.saveAndFlush(pendingFile);
            log.info("검열 완료 이미지 업로드: s3Key={}, size={}", s3Key, imageBytes.length);

            return UploadedImageResponseDto.of(
                    permanentUrl,
                    s3Key,
                    originalFilename,
                    storedContentType
            );
        } catch (Exception e) {
            if (uploaded) {
                deleteObject(s3Key);
            }
            log.error("검열 완료 이미지 업로드 실패: s3Key={}", s3Key, e);
            throw new FileProcessingException("검열 완료 이미지를 저장하지 못했습니다.", e);
        }
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
            return false;
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
    S3Client createS3Client() {
        try {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(getAccessKey(), getSecretKey());
            return S3Client.builder()
                    .region(Region.of(getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
        } catch (Exception e) {
            log.error("S3 클라이언트 생성 실패", e);
            throw new FileProcessingException("S3 연결에 실패했습니다: " + e.getMessage(), e);
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

}
