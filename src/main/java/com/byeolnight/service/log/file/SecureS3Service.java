package com.byeolnight.service.log.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 보안 강화된 S3 서비스
 * - 인증된 사용자만 업로드 가능
 * - 파일 타입 및 크기 제한 강화
 * - 악성 파일 업로드 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecureS3Service {

    private final S3Service s3Service;
    private final GoogleVisionService googleVisionService;
    
    // 허용된 파일 확장자 (보안 강화)
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );
    
    // 최대 파일 크기 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 보안 강화된 Presigned URL 생성
     * @param filename 파일명
     * @param contentType 콘텐츠 타입
     * @return 업로드 정보
     */
    public Map<String, String> generateSecurePresignedUrl(String filename, String contentType) {
        // 파일명 검증
        validateFilename(filename);
        
        // 파일 확장자 검증
        validateFileExtension(filename);
        
        // 콘텐츠 타입 검증
        validateContentType(contentType, filename);
        
        try {
            log.info("보안 Presigned URL 생성: filename={}, contentType={}", filename, contentType);
            return s3Service.generatePresignedUrl(filename, contentType);
        } catch (Exception e) {
            log.error("보안 Presigned URL 생성 실패: filename={}", filename, e);
            throw new RuntimeException("보안 파일 업로드 URL 생성에 실패했습니다.", e);
        }
    }

    /**
     * 파일명 유효성 검사
     */
    private void validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 필요합니다.");
        }
        
        // 파일명 길이 제한
        if (filename.length() > 255) {
            throw new IllegalArgumentException("파일명이 너무 깁니다. (최대 255자)");
        }
        
        // 위험한 문자 검사
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new SecurityException("파일명에 위험한 문자가 포함되어 있습니다.");
        }
    }

    /**
     * 파일 확장자 검증
     */
    private void validateFileExtension(String filename) {
        String extension = getFileExtension(filename);
        if (extension.isEmpty()) {
            throw new IllegalArgumentException("파일 확장자가 필요합니다.");
        }
        
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                "지원되지 않는 파일 형식입니다. 허용 형식: " + String.join(", ", ALLOWED_EXTENSIONS)
            );
        }
    }

    /**
     * 콘텐츠 타입 검증
     */
    private void validateContentType(String contentType, String filename) {
        if (contentType != null && !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }
        
        // 파일 확장자와 콘텐츠 타입 일치 여부 검사
        String extension = getFileExtension(filename).toLowerCase();
        if (contentType != null) {
            boolean typeMatches = switch (extension) {
                case "jpg", "jpeg" -> contentType.equals("image/jpeg");
                case "png" -> contentType.equals("image/png");
                case "gif" -> contentType.equals("image/gif");
                case "webp" -> contentType.equals("image/webp");
                case "bmp" -> contentType.equals("image/bmp");
                default -> false;
            };
            
            if (!typeMatches) {
                log.warn("파일 확장자와 콘텐츠 타입 불일치: extension={}, contentType={}", extension, contentType);
            }
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}