package com.byeolnight.controller.file;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.service.file.FileUploadRateLimitService;
import com.byeolnight.service.file.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

    @Operation(summary = "이미지 검사", description = "업로드된 이미지를 백그라운드에서 검사합니다. 부적절한 이미지는 자동으로 삭제됩니다.")
    @PostMapping("/check-image")
    public ResponseEntity<CommonResponse<Map<String, Object>>> checkImage(
            @RequestParam("imageUrl") String imageUrl,
            @RequestParam(value = "needsModeration", defaultValue = "false") boolean needsModeration) {
        
        try {
            // 검열이 필요한 경우에만 검사 진행
            if (needsModeration) {
                // 백그라운드에서 검사 시작 (결과는 신경쓰지 않음)
                s3Service.checkImageInBackground(imageUrl);
            }
            
            // 항상 성공 응답 (UI 블로킹 방지)
            return ResponseEntity.ok(CommonResponse.success(Map.of(
                "status", "processing",
                "url", imageUrl
            )));
        } catch (Exception e) {
            log.error("이미지 검사 요청 오류", e);
            // 오류가 발생해도 성공 응답 (UI 블로킹 방지)
            return ResponseEntity.ok(CommonResponse.success(Map.of(
                "status", "error",
                "url", imageUrl,
                "message", "이미지 검사 요청 중 오류가 발생했지만 계속 진행합니다."
            )));
        }
    }

    @Operation(summary = "URL 기반 이미지 검열", description = "업로드된 이미지의 URL을 기반으로 검열합니다. 부적절한 이미지는 자동으로 삭제됩니다.")
    @PostMapping("/moderate-url")
    public ResponseEntity<CommonResponse<Map<String, Object>>> moderateUrl(
            @RequestParam("imageUrl") String imageUrl,
            @RequestParam("s3Key") String s3Key) {
        
        try {
            log.info("URL 기반 이미지 검열 시작: {}", s3Key);
            
            // URL 유효성 검사
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(CommonResponse.error("이미지 URL이 필요합니다."));
            }
            
            // S3 키 유효성 검사
            if (s3Key == null || s3Key.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(CommonResponse.error("S3 키가 필요합니다."));
            }
            
            // 이미지 검증 (백그라운드에서 처리)
            s3Service.checkImageInBackground(imageUrl);
            
            // 항상 성공 응답 (실제 검열은 백그라운드에서 진행)
            return ResponseEntity.ok(CommonResponse.success(Map.of(
                "status", "processing",
                "isSafe", true,
                "message", "이미지 검열이 백그라운드에서 진행 중입니다."
            )));
        } catch (Exception e) {
            log.error("URL 기반 이미지 검열 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResponse.error(
                "이미지 검열 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
    
    @Operation(summary = "클립보드 이미지 직접 검열", description = "클립보드에서 붙여넣은 이미지를 직접 검열하고 업로드합니다.")
    @PostMapping("/moderate-direct")
    public ResponseEntity<CommonResponse<Map<String, Object>>> moderateDirect(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "needsModeration", defaultValue = "true") boolean needsModeration) {
        
        try {
            if (!needsModeration) {
                return ResponseEntity.ok(CommonResponse.success(Map.of(
                    "status", "skipped",
                    "isSafe", true,
                    "message", "검열이 요청되지 않았습니다."
                )));
            }
            
            // 파일 유효성 검사
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(CommonResponse.error("파일이 없습니다."));
            }
            
            // 이미지 검증
            byte[] imageBytes = file.getBytes();
            boolean isSafe = s3Service.validateUploadedImage(imageBytes);
            
            if (!isSafe) {
                log.warn("부적절한 이미지 감지: {}", file.getOriginalFilename());
                return ResponseEntity.ok(CommonResponse.success(Map.of(
                    "status", "completed",
                    "isSafe", false,
                    "message", "부적절한 이미지가 감지되었습니다."
                )));
            }
            
            // 안전한 이미지는 S3에 업로드
            Map<String, String> uploadResult = s3Service.uploadImageWithValidation(file);
            
            log.info("이미지 직접 검열 및 업로드 성공: {} -> {}", file.getOriginalFilename(), uploadResult.get("url"));
            
            // 업로드 결과와 함께 검열 결과 반환
            Map<String, Object> result = new HashMap<>();
            result.put("status", "completed");
            result.put("isSafe", true);
            result.put("url", uploadResult.get("url"));
            result.put("s3Key", uploadResult.get("s3Key"));
            result.put("originalName", file.getOriginalFilename());
            result.put("contentType", file.getContentType());
            result.put("message", "이미지가 안전합니다.");
            
            return ResponseEntity.ok(CommonResponse.success(result));
        } catch (Exception e) {
            log.error("이미지 직접 검열 오류", e);
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
}