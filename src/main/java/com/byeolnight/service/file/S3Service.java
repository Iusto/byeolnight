package com.byeolnight.service.file;

import com.byeolnight.entity.file.File;
import com.byeolnight.repository.file.FileRepository;
import com.byeolnight.dto.file.UploadedImageResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;

import com.byeolnight.infrastructure.exception.FileProcessingException;

import java.util.UUID;

/**
 * AWS S3 이미지 저장 및 관리 서비스.
 * 안전성 검열을 통과한 이미지 바이트만 공개 경로에 저장한다.
 */
@Slf4j
@Service
public class S3Service {

    private final GoogleVisionService googleVisionService;
    private final FileRepository fileRepository;

    @Autowired
    public S3Service(GoogleVisionService googleVisionService,
                    FileRepository fileRepository) {
        this.googleVisionService = googleVisionService;
        this.fileRepository = fileRepository;
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

    String getBucketName() {
        return bucketName;
    }

    String getAccessKey() {
        return accessKey;
    }

    String getSecretKey() {
        return secretKey;
    }

    String getRegion() {
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
            deleteObjectOrThrow(s3Key);
        } catch (Exception exception) {
            log.error("S3 ?? ?? ??: {}", s3Key, exception);
        }
    }

    /** ?? ???? ?? ?? ??? ??? ????? ??? ??? ????. */
    public void deleteObjectOrThrow(String s3Key) {
        try (S3Client s3Client = createS3Client()) {
            s3Client.deleteObject(builder -> builder.bucket(getBucketName()).key(s3Key));
            log.info("S3 ?? ?? ??: {}", s3Key);
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
/**
     * 고아 이미지 자동 정리 (File 테이블 기반)
     *
     * PENDING 상태이고 7일 이상 경과한 파일을 S3에서 삭제하고 DB에서도 제거
     *
     * @return 삭제된 이미지 개수
     */
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
