package com.byeolnight.service.file;

import com.byeolnight.dto.file.UploadedImageResponseDto;
import com.byeolnight.infrastructure.exception.FileProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 공개 저장 전에 파일 형식 검증과 이미지 검열을 강제하는 업로드 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecureS3Service {

    private final S3Service s3Service;
    
    // 허용된 파일 확장자 (보안 강화)
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );
    
    // 최대 파일 크기 (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 이미지 바이트를 서버에서 먼저 검열한 뒤 안전한 파일만 S3에 공개한다.
     */
    public UploadedImageResponseDto uploadModeratedImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지 파일이 필요합니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
        }

        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        validateFilename(filename);
        validateFileExtension(filename);
        validateContentType(contentType, filename);

        try {
            return s3Service.uploadModeratedImage(filename, file.getBytes());
        } catch (java.io.IOException e) {
            throw new FileProcessingException("업로드 파일을 읽을 수 없습니다.", e);
        }
    }

    /**
     * 파일명 유효성 검사
     * 
     * 검증 항목:
     * - null/빈 문자열 체크
     * - 길이 제한 (255자)
     * - 경로 조작 공격 방지 (.., /, \ 문자 차단)
     * 
     * @param filename 검증할 파일명
     * @throws IllegalArgumentException 파일명이 null이거나 너무 긴 경우
     * @throws SecurityException 위험한 문자가 포함된 경우
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
     * 
     * 허용된 확장자만 통과:
     * - jpg, jpeg, png, gif, webp, bmp
     * 
     * @param filename 검증할 파일명
     * @throws IllegalArgumentException 확장자가 없거나 허용되지 않는 경우
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
     * 
     * 검증 항목:
     * - image/* 형식만 허용
     * - 파일 확장자와 콘텐츠 타입 일치성 확인
     * 
     * @param contentType 검증할 콘텐츠 타입
     * @param filename 파일명 (확장자 추출용)
     * @throws IllegalArgumentException 이미지가 아니거나 타입 불일치
     */
    private void validateContentType(String contentType, String filename) {
        if (contentType == null || contentType.isBlank() || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }
        
        // 파일 확장자와 콘텐츠 타입 일치 여부 검사
        String extension = getFileExtension(filename).toLowerCase();
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
            throw new IllegalArgumentException("파일 확장자와 콘텐츠 타입이 일치하지 않습니다.");
        }
    }

    /**
     * 파일 확장자 추출
     * 
     * @param filename 파일명
     * @return 확장자 (점 제외, 소문자 변환 안 함)
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}
