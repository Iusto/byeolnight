package com.byeolnight.controller.file;

import com.byeolnight.dto.file.ViewUrlResponseDto;
import com.byeolnight.dto.file.UploadedImageResponseDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.service.file.CloudFrontService;
import com.byeolnight.service.file.FileUploadRateLimitService;
import com.byeolnight.service.file.SecureS3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@Slf4j
@Tag(name = "📁 파일 API", description = "AWS S3 파일 업로드 및 관리 API")
public class FileController {

    private final SecureS3Service secureS3Service;
    private final CloudFrontService cloudFrontService;
    private final FileUploadRateLimitService rateLimitService;

    public FileController(
            SecureS3Service secureS3Service,
            CloudFrontService cloudFrontService,
            FileUploadRateLimitService rateLimitService) {
        this.secureS3Service = secureS3Service;
        this.cloudFrontService = cloudFrontService;
        this.rateLimitService = rateLimitService;
    }

    @Operation(summary = "검열 후 이미지 업로드", description = "서버에서 이미지를 검열하고 통과한 파일만 S3에 업로드합니다.")
    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<UploadedImageResponseDto>> uploadImage(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {
        String clientIp = IpUtil.getClientIp(request);
        if (!rateLimitService.isUploadAllowed(clientIp, file.getSize())) {
            return ResponseEntity.status(429)
                    .body(CommonResponse.error("이미지 업로드 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."));
        }

        try {
            UploadedImageResponseDto result = secureS3Service.uploadModeratedImage(file);
            log.info("검열 완료 이미지 업로드 성공 - s3Key={}, clientIp={}", result.s3Key(), clientIp);
            return ResponseEntity.ok(CommonResponse.success(result));
        } finally {
            rateLimitService.finishUpload(clientIp);
        }
    }

    /**
     * 이미지 조회용 CloudFront Signed URL 생성
     */
    @GetMapping("/view-url")
    public ResponseEntity<CommonResponse<ViewUrlResponseDto>> getViewUrl(
            @RequestParam("s3Key") String s3Key) {

        try {
            String signedUrl = cloudFrontService.generateSignedUrl(s3Key, 60); // 1시간
            return ResponseEntity.ok(CommonResponse.success(ViewUrlResponseDto.of(signedUrl, s3Key)));
        } catch (Exception e) {
            log.error("이미지 조회 URL 생성 실패: s3Key={}", s3Key, e);
            return ResponseEntity.status(500).body(CommonResponse.error("이미지 URL 생성에 실패했습니다."));
        }
    }
    
}
