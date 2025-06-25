package com.byeolnight.controller.post;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.file.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileUploadController {

    private final S3Service s3Service;

    /**
     * Presigned URL 발급 API
     */
    @Operation(summary = "Presigned URL 발급", description = "AWS S3 업로드용 Presigned URL을 발급합니다. 허용된 확장자(jpg/jpeg/png/gif)만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Presigned URL 반환",
                    content = @Content(schema = @Schema(implementation = URL.class))),
            @ApiResponse(responseCode = "400", description = "허용되지 않은 확장자",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/presign")
    public ResponseEntity<CommonResponse<URL>> getPresignedUrl(
            @RequestParam @NotBlank String fileName,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        List<String> allowed = List.of("jpg", "jpeg", "png", "gif");

        if (!allowed.contains(extension)) {
            return ResponseEntity.badRequest()
                    .body(CommonResponse.fail("허용되지 않은 파일 확장자입니다."));
        }

        String safeFileName = String.format("uploads/%d/%s.%s", user.getId(), UUID.randomUUID(), extension);
        URL url = s3Service.generatePresignedUploadUrl(safeFileName);

        return ResponseEntity.ok(CommonResponse.success(url));
    }
}
