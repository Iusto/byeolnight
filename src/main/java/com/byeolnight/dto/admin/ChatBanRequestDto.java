package com.byeolnight.dto.admin;

import lombok.Data;

@Data
public class ChatBanRequestDto {
    private String username;
    private int duration; // 분 단위
    private String reason;
}