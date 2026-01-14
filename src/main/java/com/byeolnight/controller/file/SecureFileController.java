package com.byeolnight.controller.file;

import com.byeolnight.dto.file.PresignedUrlResponseDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.file.CloudFrontService;
import com.byeolnight.service.file.SecureS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * 보안 강화된 파일 컨트롤러
 * - 업로드: S3 Presigned URL (인증 필요)
 * - 조회: CloudFront Signed URL (SSRF 방지)
 */
@RestController
@RequestMapping("/api/secure-files")
@RequiredArgsConstructor
@Slf4j
public class SecureFileController {

    private final SecureS3Service secureS3Service;
    private final CloudFrontService cloudFrontService;
    
    // CloudFront 도메인만 허용 (SSRF 방지)
    private static final Set<String> ALLOWED_DOMAINS = Set.of(
        "d1234567890.cloudfront.net" // 실제 CloudFront 도메인으로 변경
    );

    /**
     * 인증된 사용자만 업로드 URL 생성 가능
     */
    @PostMapping("/upload-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonResponse<PresignedUrlResponseDto>> getUploadUrl(
            @RequestParam("filename") String filename,
            @RequestParam(value = "contentType", required = false) String contentType) {

        try {
            PresignedUrlResponseDto result = secureS3Service.generateSecurePresignedUrl(filename, contentType);
            return ResponseEntity.ok(CommonResponse.success(result));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(CommonResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(CommonResponse.error(e.getMessage()));
        }
    }

    /**
     * 이미지 조회용 CloudFront Signed URL 생성
     */
    @GetMapping("/view-url")
    public ResponseEntity<CommonResponse<Map<String, String>>> getViewUrl(
            @RequestParam("s3Key") String s3Key,
            @RequestParam(value = "expires", defaultValue = "60") int expirationMinutes) {
        
        try {
            // S3 키 유효성 검사
            if (s3Key == null || s3Key.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(CommonResponse.error("S3 키가 필요합니다."));
            }
            
            // 만료 시간 제한 (최대 24시간)
            if (expirationMinutes > 1440) {
                expirationMinutes = 1440;
            }
            
            String signedUrl = cloudFrontService.generateSignedUrl(s3Key, expirationMinutes);
            
            Map<String, String> result = Map.of(
                "viewUrl", signedUrl,
                "s3Key", s3Key,
                "expiresInMinutes", String.valueOf(expirationMinutes)
            );
            
            return ResponseEntity.ok(CommonResponse.success(result));
            
        } catch (Exception e) {
            log.error("이미지 조회 URL 생성 실패: s3Key={}", s3Key, e);
            return ResponseEntity.status(500).body(CommonResponse.error("이미지 URL 생성에 실패했습니다."));
        }
    }

    /**
     * URL 검증 (SSRF 방지)
     */
    @PostMapping("/validate-url")
    public ResponseEntity<CommonResponse<Map<String, Boolean>>> validateUrl(
            @RequestParam("imageUrl") String imageUrl) {
        
        try {
            // CloudFront 도메인만 허용
            boolean isValid = isCloudFrontUrl(imageUrl);
            
            Map<String, Boolean> result = Map.of(
                "isValid", isValid,
                "isCloudFront", isValid
            );
            
            return ResponseEntity.ok(CommonResponse.success(result));
            
        } catch (Exception e) {
            log.error("URL 검증 실패: imageUrl={}", imageUrl, e);
            return ResponseEntity.ok(CommonResponse.success(Map.of("isValid", false)));
        }
    }

    /**
     * CloudFront URL 검증 (SSRF 완전 차단)
     */
    private boolean isCloudFrontUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return false;
        }
        
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            
            // HTTPS만 허용
            if (!"https".equalsIgnoreCase(url.getProtocol())) {
                return false;
            }
            
            // CloudFront 도메인만 허용
            String host = url.getHost().toLowerCase();
            return ALLOWED_DOMAINS.contains(host);
            
        } catch (Exception e) {
            return false;
        }
    }
}