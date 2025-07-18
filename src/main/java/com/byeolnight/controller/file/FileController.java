package com.byeolnight.controller.file;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.service.file.FileUploadRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import com.byeolnight.service.file.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "📁 파일 API", description = "AWS S3 파일 업로드 및 관리 API")
public class FileController {

    private final S3Service s3Service;
    private final FileUploadRateLimitService rateLimitService;

    @Operation(summary = "S3 Presigned URL 생성", description = "파일 업로드를 위한 S3 Presigned URL을 생성합니다.")
    @PostMapping("/presigned-url")
    public ResponseEntity<CommonResponse<Map<String, String>>> getPresignedUrl(
            @RequestParam("filename") String filename,
            @RequestParam(value = "contentType", required = false) String contentType,
            HttpServletRequest request) {
        
        if (filename == null || filename.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(CommonResponse.error("파일명이 필요합니다."));
        }
        
        // 파일 확장자 검사
        String extension = "";
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = filename.substring(lastDotIndex + 1).toLowerCase();
        } else {
            return ResponseEntity.badRequest().body(CommonResponse.error("파일 확장자가 필요합니다."));
        }
        
        // 이미지 파일 확장자 검사
        if (!extension.matches("jpg|jpeg|png|gif|bmp|webp|svg")) {
            return ResponseEntity.badRequest().body(CommonResponse.error("지원되지 않는 이미지 형식입니다. 지원 형식: jpg, jpeg, png, gif, bmp, webp, svg"));
        }
        
        // Rate Limiting 검사
        String clientIp = IpUtil.getClientIp(request);
        
        // Presigned URL 생성 제한 확인
        if (!rateLimitService.isPresignedUrlAllowed(clientIp)) {
            return ResponseEntity.status(429).body(CommonResponse.error("Presigned URL 생성 한도를 초과했습니다. 잠시 후 다시 시도해주세요."));
        }
        
        try {
            Map<String, String> result = s3Service.generatePresignedUrl(filename, contentType);
            return ResponseEntity.ok(CommonResponse.success(result));
        } catch (Exception e) {
            log.error("Presigned URL 생성 오류", e);
            return ResponseEntity.badRequest().body(CommonResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "업로드된 이미지 검증", description = "이미 업로드된 이미지를 Google Vision API로 검증합니다. 부적절한 이미지는 자동으로 삭제됩니다.")
    @PostMapping("/validate-image")
    public ResponseEntity<CommonResponse<Map<String, Object>>> validateImage(
            @RequestParam("imageUrl") String imageUrl) {
        
        try {
            boolean isValid = s3Service.validateImageByUrl(imageUrl);
            Map<String, Object> result;
            
            if (isValid) {
                result = Map.of(
                    "isValid", true,
                    "message", "이미지가 안전합니다.",
                    "url", imageUrl
                );
            } else {
                result = Map.of(
                    "isValid", false,
                    "message", "부적절한 이미지가 감지되어 자동으로 삭제되었습니다."
                );
            }
            
            return ResponseEntity.ok(CommonResponse.success(result));
        } catch (Exception e) {
            log.error("이미지 검증 오류", e);
            return ResponseEntity.internalServerError().body(CommonResponse.error("이미지 검증 중 오류가 발생했습니다."));
        }
    }
}