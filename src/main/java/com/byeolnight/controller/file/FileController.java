package com.byeolnight.controller.file;

import com.byeolnight.dto.file.ModerationResultDto;
import com.byeolnight.dto.file.PresignedUrlResponseDto;
import com.byeolnight.dto.file.ViewUrlResponseDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.service.file.CloudFrontService;
import com.byeolnight.service.file.FileUploadRateLimitService;
import com.byeolnight.service.file.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files")
@Slf4j
@Tag(name = "📁 파일 API", description = "AWS S3 파일 업로드 및 관리 API")
public class FileController {

    private final S3Service s3Service;
    private final CloudFrontService cloudFrontService;
    private final FileUploadRateLimitService rateLimitService;
    private final String cloudFrontDomain;

    public FileController(
            S3Service s3Service,
            CloudFrontService cloudFrontService,
            FileUploadRateLimitService rateLimitService,
            @Value("${cloud.aws.cloudfront.domain}") String cloudFrontDomain) {
        this.s3Service = s3Service;
        this.cloudFrontService = cloudFrontService;
        this.rateLimitService = rateLimitService;
        this.cloudFrontDomain = cloudFrontDomain;
    }

    @Operation(summary = "S3 Presigned URL 생성", description = "파일 업로드를 위한 S3 Presigned URL을 생성합니다.")
    @PostMapping("/presigned-url")
    public ResponseEntity<CommonResponse<PresignedUrlResponseDto>> getPresignedUrl(
            @RequestParam("filename") String filename,
            @RequestParam(value = "contentType", required = false) String contentType,
            HttpServletRequest request) {

        String clientIp = IpUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        log.info("Presigned URL 요청: filename={}, contentType={}, clientIp={}, userAgent={}",
                filename, contentType, clientIp, userAgent);

        if (filename == null || filename.trim().isEmpty()) {
            log.warn("Presigned URL 요청 실패: 파일명 누락 - clientIp={}", clientIp);
            return ResponseEntity.badRequest().body(CommonResponse.error("파일명이 필요합니다."));
        }

        // 파일 확장자 검사
        String extension = "";
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = filename.substring(lastDotIndex + 1).toLowerCase();
        } else {
            log.warn("Presigned URL 요청 실패: 파일 확장자 누락 - filename={}, clientIp={}", filename, clientIp);
            return ResponseEntity.badRequest().body(CommonResponse.error("파일 확장자가 필요합니다."));
        }

        // 이미지 파일 확장자 검사
        if (!extension.matches("jpg|jpeg|png|gif|bmp|webp|svg")) {
            log.warn("Presigned URL 요청 실패: 지원되지 않는 파일 형식 - extension={}, filename={}, clientIp={}",
                    extension, filename, clientIp);
            return ResponseEntity.badRequest().body(CommonResponse.error("지원되지 않는 이미지 형식입니다. 지원 형식: jpg, jpeg, png, gif, bmp, webp, svg"));
        }

        // Presigned URL 생성 제한 확인
        if (!rateLimitService.isPresignedUrlAllowed(clientIp)) {
            log.warn("Presigned URL 요청 실패: Rate Limit 초과 - clientIp={}, filename={}", clientIp, filename);
            return ResponseEntity.status(429).body(CommonResponse.error("Presigned URL 생성 한도를 초과했습니다. 잠시 후 다시 시도해주세요."));
        }

        try {
            log.debug("S3 Presigned URL 생성 시작 - filename={}, contentType={}", filename, contentType);
            PresignedUrlResponseDto result = s3Service.generatePresignedUrl(filename, contentType);
            log.info("Presigned URL 생성 성공 - s3Key={}, clientIp={}", result.s3Key(), clientIp);
            return ResponseEntity.ok(CommonResponse.success(result));
        } catch (Exception e) {
            log.error("Presigned URL 생성 오류 - filename={}, contentType={}, clientIp={}, error={}",
                    filename, contentType, clientIp, e.getMessage(), e);
            return ResponseEntity.badRequest().body(CommonResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "URL 기반 이미지 검열", description = "업로드된 이미지의 URL을 기반으로 검열합니다. 부적절한 이미지는 자동으로 삭제됩니다.")
    @PostMapping("/moderate-url")
    public ResponseEntity<CommonResponse<ModerationResultDto>> moderateUrl(
            @RequestParam("imageUrl") String imageUrl,
            @RequestParam("s3Key") String s3Key,
            HttpServletRequest request) {

        String clientIp = IpUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            log.info("URL 기반 이미지 검열 시작: s3Key={}, imageUrl={}, clientIp={}, userAgent={}",
                    s3Key, imageUrl, clientIp, userAgent);

            // URL 유효성 검사
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                log.warn("이미지 검열 실패: URL 누락 - clientIp={}, s3Key={}", clientIp, s3Key);
                return ResponseEntity.badRequest().body(CommonResponse.error("이미지 URL이 필요합니다."));
            }

            // S3 키 유효성 검사
            if (s3Key == null || s3Key.trim().isEmpty()) {
                log.warn("이미지 검열 실패: S3 키 누락 - clientIp={}, imageUrl={}", clientIp, imageUrl);
                return ResponseEntity.badRequest().body(CommonResponse.error("S3 키가 필요합니다."));
            }

            // 이미지 즉시 검증 (비동기적으로 처리하지 않고 즉시 결과 반환)
            try {
                // CloudFront URL만 허용 (SSRF 방지)
                if (!isCloudFrontUrl(imageUrl)) {
                    throw new SecurityException("허용되지 않는 URL입니다. CloudFront URL만 사용 가능합니다.");
                }

                // 이미지 다운로드
                java.net.URL url = new java.net.URL(imageUrl);
                java.io.InputStream inputStream = url.openStream();
                byte[] imageBytes = inputStream.readAllBytes();
                inputStream.close();

                // 검사
                boolean isSafe = s3Service.validateUploadedImage(imageBytes);
                log.info("이미지 검사 결과: {} -> {}", imageUrl, isSafe ? "안전" : "부적절");

                // 부적절한 경우 삭제
                if (!isSafe) {
                    log.warn("부적절한 이미지 감지: {} - 자동 삭제 시작", s3Key);
                    s3Service.deleteObject(s3Key);
                    log.info("부적절한 이미지 삭제 완료: {}", s3Key);

                    return ResponseEntity.ok(CommonResponse.success(
                            ModerationResultDto.completed(false, "부적절한 이미지가 감지되어 삭제되었습니다.")
                    ));
                }

                // 안전한 이미지인 경우
                return ResponseEntity.ok(CommonResponse.success(
                        ModerationResultDto.completed(true, "이미지 검증이 완료되었습니다.")
                ));

            } catch (Exception e) {
                log.error("이미지 검증 실패: imageUrl={}, s3Key={}, clientIp={}, error={}",
                        imageUrl, s3Key, clientIp, e.getMessage(), e);
                // 오류 발생 시 S3에서 이미지 삭제
                try {
                    s3Service.deleteObject(s3Key);
                    log.info("검증 실패한 이미지 삭제 완료: s3Key={}, clientIp={}", s3Key, clientIp);
                } catch (Exception ex) {
                    log.error("이미지 삭제 실패: s3Key={}, clientIp={}, deleteError={}", s3Key, clientIp, ex.getMessage(), ex);
                }

                return ResponseEntity.ok(CommonResponse.success(
                        ModerationResultDto.error("이미지 검증 중 오류가 발생했습니다.")
                ));
            }
        } catch (Exception e) {
            log.error("URL 기반 이미지 검열 오류: s3Key={}, imageUrl={}, clientIp={}, error={}",
                    s3Key, imageUrl, clientIp, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResponse.error(
                "이미지 검열 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
    
    @Operation(summary = "S3 이미지 삭제", description = "S3에 업로드된 이미지를 삭제합니다.")
    @DeleteMapping("/delete")
    public ResponseEntity<CommonResponse<Void>> deleteImage(
            @RequestParam("s3Key") String s3Key) {
        
        try {
            log.info("S3 이미지 삭제 요청: {}", s3Key);
            s3Service.deleteObject(s3Key);
            return ResponseEntity.ok(CommonResponse.success(null, "이미지가 성공적으로 삭제되었습니다."));
        } catch (Exception e) {
            log.error("S3 이미지 삭제 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResponse.error(
                "이미지 삭제 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 이미지 조회용 CloudFront Signed URL 생성
     */
    @GetMapping("/view-url")
    public ResponseEntity<CommonResponse<ViewUrlResponseDto>> getViewUrl(
            @RequestParam("s3Key") String s3Key) {

        try {
            String signedUrl = cloudFrontService.generateSignedUrl(s3Key, 60); // 1시간
            return ResponseEntity.ok(CommonResponse.success(ViewUrlResponseDto.of(signedUrl, s3Key)));
        } catch (Exception e) {
            log.error("이미지 조회 URL 생성 실패: s3Key={}", s3Key, e);
            return ResponseEntity.status(500).body(CommonResponse.error("이미지 URL 생성에 실패했습니다."));
        }
    }
    
    /**
     * CloudFront URL 검증 (SSRF 방지)
     * 설정된 CloudFront 도메인과 *.cloudfront.net 도메인만 허용
     */
    private boolean isCloudFrontUrl(String imageUrl) {
        if (imageUrl == null) return false;

        try {
            java.net.URI uri = java.net.URI.create(imageUrl);
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase();

            // HTTPS만 허용
            if (!"https".equals(uri.getScheme())) {
                return false;
            }

            // 설정된 CloudFront 도메인 또는 *.cloudfront.net 허용
            return host.equals(cloudFrontDomain.toLowerCase()) ||
                   host.endsWith(".cloudfront.net");

        } catch (Exception e) {
            return false;
        }
    }
}