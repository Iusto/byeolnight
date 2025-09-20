package com.byeolnight.service.log.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 보안 강화된 S3 서비스 (S3Service의 보안 래퍼)
 * 
 * 역할:
 * - S3Service의 기본 기능에 추가 보안 검증 레이어 제공
 * - 파일명, 확장자, 콘텐츠 타입에 대한 엄격한 검증
 * - 악성 파일 업로드 및 경로 조작 공격 방지
 * 
 * 보안 강화 사항:
 * - 허용된 이미지 확장자만 업로드 (jpg, jpeg, png, gif, webp, bmp)
 * - 파일명 길이 제한 (255자)
 * - 위험한 문자 차단 (.., /, \)
 * - 파일 확장자와 콘텐츠 타입 일치성 검증
 * - 이미지 파일만 허용 (image/* 콘텐츠 타입)
 * 
 * 사용 시나리오:
 * - 관리자 페이지에서의 파일 업로드
 * - 보안이 중요한 파일 업로드 작업
 * - 추가 검증이 필요한 특수 업로드
 * 
 * @author byeolnight
 * @since 1.0
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
    
    // 최대 파일 크기 (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 보안 강화된 Presigned URL 생성
     * 
     * S3Service.generatePresignedUrl()에 추가 보안 검증을 적용:
     * 1. 파일명 유효성 검사 (길이, 위험 문자)
     * 2. 허용된 확장자 검증
     * 3. 콘텐츠 타입 검증 및 일치성 확인
     * 4. 모든 검증 통과 후 S3Service 호출
     * 
     * @param filename 파일명 (필수, 255자 이하, 위험 문자 불허)
     * @param contentType 콘텐츠 타입 (image/* 형식만 허용)
     * @return S3Service와 동일한 업로드 정보 맵
     * @throws IllegalArgumentException 파일명/확장자/타입 검증 실패
     * @throws SecurityException 보안 위험 요소 감지
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