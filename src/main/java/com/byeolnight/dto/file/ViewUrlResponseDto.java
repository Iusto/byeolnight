package com.byeolnight.dto.file;

/**
 * CloudFront 이미지 조회 URL 응답 DTO
 * GET /api/files/view-url 엔드포인트에서 사용
 */
public record ViewUrlResponseDto(
        String viewUrl,
        String s3Key
) {
    public static ViewUrlResponseDto of(String viewUrl, String s3Key) {
        return new ViewUrlResponseDto(viewUrl, s3Key);
    }
}
