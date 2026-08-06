package com.byeolnight.dto.file;

/**
 * 서버 검열을 통과한 이미지 업로드 결과.
 */
public record UploadedImageResponseDto(
        String url,
        String s3Key,
        String originalName,
        String contentType
) {
    public static UploadedImageResponseDto of(
            String url,
            String s3Key,
            String originalName,
            String contentType
    ) {
        return new UploadedImageResponseDto(url, s3Key, originalName, contentType);
    }
}
