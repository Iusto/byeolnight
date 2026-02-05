package com.byeolnight.dto.file;

import lombok.Builder;
import lombok.Getter;

/**
 * 이미지 검열 결과 응답 DTO
 * POST /api/files/moderate-url 엔드포인트에서 사용
 */
@Getter
@Builder
public class ModerationResultDto {
    private String status;      // "completed", "error"
    private boolean isSafe;
    private String message;

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
}
