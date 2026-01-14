package com.byeolnight.dto.file;

/**
 * S3 Presigned URL 응답 DTO
 * POST /api/files/presigned-url 엔드포인트에서 사용
 */
public record PresignedUrlResponseDto(
        String uploadUrl,    // 업로드용 Presigned URL
        String url,          // 조회용 CloudFront URL
        String s3Key,
        String originalName,
        String contentType
) {
    public static PresignedUrlResponseDto of(
            String uploadUrl,
            String url,
            String s3Key,
            String originalName,
            String contentType
    ) {
        return new PresignedUrlResponseDto(uploadUrl, url, s3Key, originalName, contentType);
    }
}
