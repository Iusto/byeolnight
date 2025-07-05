package com.byeolnight.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PointAwardRequestDto {
    
    @NotNull(message = "포인트는 필수입니다.")
    @Min(value = 1, message = "포인트는 1 이상이어야 합니다.")
    private Integer points;
    
    @NotBlank(message = "사유는 필수입니다.")
    private String reason;
}