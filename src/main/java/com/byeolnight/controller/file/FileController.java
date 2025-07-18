package com.byeolnight.controller.file;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.service.file.FileUploadRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import com.byeolnight.service.file.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "📁 파일 API", description = "AWS S3 파일 업로드 및 관리 API")
public class FileController {

    private final S3Service s3Service;
    private final FileUploadRateLimitService rateLimitService;

    @Operation(summary = "S3 Presigned URL 생성", description = "파일 업로드를 위한 S3 Presigned URL을 생성합니다.")
    @PostMapping("/presigned-url")
    public ResponseEntity<CommonResponse<Map<String, String>>> getPresignedUrl(
            @RequestParam(value = "filename", required = false) String filename,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {
        
        // MultipartFile에서 파일명 추출
        if (file != null && !file.isEmpty()) {
            filename = file.getOriginalFilename();
        }
        
        if (filename == null || filename.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(CommonResponse.error("파일명이 필요합니다."));
        }
        
        // Rate Limiting 검사
        String clientIp = IpUtil.getClientIp(request);
        
        // Presigned URL 생성 제한 확인
        if (!rateLimitService.isPresignedUrlAllowed(clientIp)) {
            return ResponseEntity.status(429).body(CommonResponse.error("Presigned URL 생성 한도를 초과했습니다. 잠시 후 다시 시도해주세요."));
        }
        
        // 파일 크기 제한
        if (file != null && file.getSize() > 10 * 1024 * 1024) { // 10MB 제한
            return ResponseEntity.badRequest().body(CommonResponse.error("파일 크기는 10MB를 초과할 수 없습니다."));
        }
        
        try {
            Map<String, String> result = s3Service.generatePresignedUrl(filename);
            return ResponseEntity.ok(CommonResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(CommonResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "이미지 직접 업로드 및 검열", description = "이미지를 직접 업로드하고 Google Vision API로 검열합니다.")
    @PostMapping("/upload-image")
    public ResponseEntity<CommonResponse<Map<String, String>>> uploadImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {
        
        // 요청 정보 로깅
        String contentType = request.getContentType();
        log.info("업로드 요청 Content-Type: {}", contentType);
        // 요청 헤더 로깅
        StringBuilder headers = new StringBuilder();
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.append(headerName).append("=").append(request.getHeader(headerName)).append(", ");
        }
        log.info("요청 헤더: {}", headers.toString());
        
        try {
            // multipart/form-data 형식 확인
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                return ResponseEntity.badRequest().body(CommonResponse.error("multipart/form-data 형식으로 요청해주세요. 현재 Content-Type: " + contentType));
            }
            
            if (file == null || file.isEmpty()) {
                log.warn("파일이 비어있거나 없습니다. file={}", file);
                return ResponseEntity.badRequest().body(CommonResponse.error("파일이 필요합니다. multipart/form-data 형식으로 'file' 필드에 파일을 첨부해주세요."));
            }
            
            // 파일 정보 로깅
            log.info("파일 정보: 이름={}, 크기={}, 형식={}", 
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
        } catch (Exception e) {
            log.error("파일 정보 처리 중 오류", e);
            return ResponseEntity.badRequest().body(CommonResponse.error("파일 정보를 처리하는 중 오류가 발생했습니다."));
        }
        
        // 파일 크기 제한 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(CommonResponse.error("이미지 크기는 5MB를 초과할 수 없습니다."));
        }
        
        // Rate Limiting 검사
        String clientIp = IpUtil.getClientIp(request);
        
        // 파일 업로드 제한 확인
        if (!rateLimitService.isUploadAllowed(clientIp, file.getSize())) {
            return ResponseEntity.status(429).body(CommonResponse.error("파일 업로드 한도를 초과했습니다. 잠시 후 다시 시도해주세요."));
        }
        
        // 동시 업로드 시작
        rateLimitService.startUpload(clientIp);
        
        try {
            // 파일 확장자 검사
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                return ResponseEntity.badRequest().body(CommonResponse.error("파일명이 없습니다."));
            }
            
            // 파일 확장자 추출
            String extension = "";
            int lastDotIndex = originalFilename.lastIndexOf('.');
            if (lastDotIndex > 0) {
                extension = originalFilename.substring(lastDotIndex + 1).toLowerCase();
            }
            
            // 이미지 파일 확장자 검사
            if (!extension.matches("jpg|jpeg|png|gif|bmp|webp|svg")) {
                return ResponseEntity.badRequest().body(CommonResponse.error("지원되지 않는 이미지 형식입니다. 지원 형식: jpg, jpeg, png, gif, bmp, webp, svg"));
            }
            
            log.info("이미지 업로드 시도: 파일명={}, 크기={}, 형식={}", 
                    originalFilename, file.getSize(), file.getContentType());
            
            Map<String, String> result = s3Service.uploadImageWithValidation(file);
            log.info("이미지 업로드 성공: {}", result.get("url"));
            return ResponseEntity.ok(CommonResponse.success(result));
        } catch (IllegalArgumentException e) {
            log.warn("이미지 업로드 유효성 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("이미지 업로드 오류", e);
            return ResponseEntity.internalServerError().body(CommonResponse.error("이미지 업로드 중 오류가 발생했습니다."));
        } finally {
            // 동시 업로드 완료
            rateLimitService.finishUpload(clientIp);
        }
    }

    @Operation(summary = "업로드된 이미지 검증", description = "이미 업로드된 이미지를 Google Vision API로 검증합니다.")
    @PostMapping("/validate-image")
    public ResponseEntity<CommonResponse<Map<String, Object>>> validateImage(
            @RequestParam("imageUrl") String imageUrl) {
        
        try {
            boolean isValid = s3Service.validateImageByUrl(imageUrl);
            Map<String, Object> result = Map.of(
                "isValid", isValid,
                "message", isValid ? "이미지가 안전합니다." : "부적절한 이미지가 감지되었습니다."
            );
            return ResponseEntity.ok(CommonResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(CommonResponse.error("이미지 검증 중 오류가 발생했습니다."));
        }
    }
}