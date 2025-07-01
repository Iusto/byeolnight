package com.byeolnight.controller.file;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.file.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/member/files")
@RequiredArgsConstructor
public class FileController {

    private final S3Service s3Service;

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