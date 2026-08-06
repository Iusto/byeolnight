package com.byeolnight.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    @Test
    @DisplayName("JWT 시크릿은 UTF-8 기준 32바이트 이상이면 허용한다")
    void acceptsStrongSecret() {
        assertThatCode(() -> JwtTokenProvider.validateSecret("가나다라마바사아자차카타파하123456"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("JWT 시크릿이 누락되거나 짧으면 애플리케이션 시작을 거부한다")
    void rejectsMissingOrWeakSecret() {
        assertThatThrownBy(() -> JwtTokenProvider.validateSecret("short-secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32바이트");
        assertThatThrownBy(() -> JwtTokenProvider.validateSecret(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("설정되지 않았습니다");
    }

    @Test
    @DisplayName("복호화되지 않은 JWT 시크릿을 거부한다")
    void rejectsEncryptedOrUnresolvedSecret() {
        assertThatThrownBy(() -> JwtTokenProvider.validateSecret("{cipher}0123456789012345678901234567890123456789"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("복호화되지 않았습니다");
        assertThatThrownBy(() -> JwtTokenProvider.validateSecret("${app.security.jwt.secret}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("복호화되지 않았습니다");
    }
}
