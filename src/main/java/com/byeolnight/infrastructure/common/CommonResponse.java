package com.byeolnight.infrastructure.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Schema(description = "공통 응답 포맷")
public class CommonResponse<T> {

    @Schema(description = "요청 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "에러 메시지 또는 상태 메시지", example = "요청이 성공적으로 처리되었습니다.")
    private String message;

    @Schema(description = "실제 데이터")
    private T data;

    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(true, null, data);
    }

    public static <T> CommonResponse<T> success(T data, String message) {
        return new CommonResponse<>(true, message, data);
    }

    public static <T> CommonResponse<T> success() {
        return new CommonResponse<>(true, null, null);
    }

    public static <T> CommonResponse<T> fail(String message) {
        return new CommonResponse<>(false, message, null);
    }

    public static <T> CommonResponse<T> error(String message) {
        return new CommonResponse<>(false, message, null);
    }
}
