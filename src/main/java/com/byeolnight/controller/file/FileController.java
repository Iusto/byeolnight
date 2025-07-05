package com.byeolnight.controller.file;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.file.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/member/files")
@RequiredArgsConstructor
@Tag(name = "📁 파일 API", description = "AWS S3 파일 업로드 및 관리 API")
public class FileController {

    private final S3Service s3Service;

    @Operation(summary = "S3 Presigned URL 생성", description = "파일 업로드를 위한 S3 Presigned URL을 생성합니다.")
    @PostMapping("/presigned-url")
    public ResponseEntity<CommonResponse<Map<String, String>>> getPresignedUrl(@RequestBody Map<String, String> request) {
        String filename = request.get("filename");
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
}