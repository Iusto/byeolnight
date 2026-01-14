package com.byeolnight.dto.file;

import lombok.Builder;
import lombok.Getter;

/**
 * 이미지 검열 결과 응답 DTO
 * POST /api/files/moderate-url, /api/files/moderate-direct 엔드포인트에서 사용
 */
@Getter
@Builder
public class ModerationResultDto {
    private String status;      // "completed", "error", "skipped"
    private boolean isSafe;
    private String message;
    private String url;         // nullable - moderateDirect에서만 사용
    private String s3Key;       // nullable - moderateDirect에서만 사용
    private String originalName; // nullable - moderateDirect에서만 사용
    private String contentType; // nullable - moderateDirect에서만 사용

    public static ModerationResultDto completed(boolean isSafe, String message) {
        return ModerationResultDto.builder()
                .status("completed")
                .isSafe(isSafe)
                .message(message)
                .build();
    }

    public static ModerationResultDto error(String message) {
        return ModerationResultDto.builder()
                .status("error")
                .isSafe(false)
                .message(message)
                .build();
    }

    public static ModerationResultDto skipped(String message) {
        return ModerationResultDto.builder()
                .status("skipped")
                .isSafe(true)
                .message(message)
                .build();
    }

    public static ModerationResultDto completedWithUpload(
            boolean isSafe,
            String message,
            String url,
            String s3Key,
            String originalName,
            String contentType
    ) {
        return ModerationResultDto.builder()
                .status("completed")
                .isSafe(isSafe)
                .message(message)
                .url(url)
                .s3Key(s3Key)
                .originalName(originalName)
                .contentType(contentType)
                .build();
    }
}
