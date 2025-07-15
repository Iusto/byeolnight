package com.byeolnight.controller.file;

import com.byeolnight.infrastructure.common.CommonResponse;
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

    @Operation(summary = "S3 Presigned URL 생성", description = "파일 업로드를 위한 S3 Presigned URL을 생성합니다.")
    @PostMapping("/presigned-url")
    public ResponseEntity<CommonResponse<Map<String, String>>> getPresignedUrl(
            @RequestParam(value = "filename", required = false) String filename,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        
        // MultipartFile에서 파일명 추출
        if (file != null && !file.isEmpty()) {
            filename = file.getOriginalFilename();
        }
        
        if (filename == null || filename.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(CommonResponse.error("파일명이 필요합니다."));
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
            @RequestParam("file") MultipartFile file) {
        
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(CommonResponse.error("파일이 필요합니다."));
        }
        
        try {
            Map<String, String> result = s3Service.uploadImageWithValidation(file);
            return ResponseEntity.ok(CommonResponse.success(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(CommonResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(CommonResponse.error("이미지 업로드 중 오류가 발생했습니다."));
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