package com.byeolnight.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountRecoveryDto {
    private String email;
    private String provider;
    private boolean recover; // true: 복구, false: 새 계정 생성
}