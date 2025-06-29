package com.byeolnight.controller.file;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.file.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final S3Service s3Service;

    @GetMapping("/presign")
    @Operation(summary = "S3 Presigned URL 발급", description = "파일 업로드를 위한 S3 Presigned URL을 발급합니다. 허용된 확장자(jpg/jpeg/png/gif/webp)만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Presigned URL 발급 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonResponse<Map<String, String>>> getPresignedUrl(
            @RequestParam @NotBlank String filename,
            @AuthenticationPrincipal User user
    ) {
        try {
            // 파일 확장자 검증
            String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif", "webp");
            
            if (!allowedExtensions.contains(extension)) {
                return ResponseEntity.badRequest()
                        .body(CommonResponse.fail("허용되지 않은 파일 확장자입니다. 허용: jpg, jpeg, png, gif, webp"));
            }
            
            Map<String, String> result = s3Service.generatePresignedUrl(filename);
            return ResponseEntity.ok(CommonResponse.success(result));
        } catch (Exception e) {
            log.error("Presigned URL 생성 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("파일 업로드 URL 생성에 실패했습니다."));
        }
    }
}