package com.byeolnight.dto.file;

/**
 * 이미지 업로드 결과 DTO
 * S3Service.uploadImageWithValidation 메서드에서 사용
 */
public record UploadResultDto(
        String url,          // CloudFront URL
        String s3Key,
        String originalName,
        String contentType,
        long size,
        boolean validated
) {
    public static UploadResultDto of(
            String url,
            String s3Key,
            String originalName,
            String contentType,
            long size
    ) {
        return new UploadResultDto(url, s3Key, originalName, contentType, size, true);
    }
}
