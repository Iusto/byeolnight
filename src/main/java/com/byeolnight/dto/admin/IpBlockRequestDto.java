package com.byeolnight.dto.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpBlockRequestDto {
    private String ip;
    private long durationMinutes; // 차단 유지 시간 (분)
}
