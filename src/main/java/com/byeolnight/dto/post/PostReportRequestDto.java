package com.byeolnight.dto.post;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostReportRequestDto {

    @NotBlank(message = "신고 사유를 입력해주세요.")
    private String reason;
    
    private String description; // 상세 설명 (선택사항)
}
