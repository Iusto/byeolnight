package com.byeolnight.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record AccountRecoveryTicketRequestDto(
        @NotBlank(message = "복구 티켓이 필요합니다.")
        String ticket
) {
}
