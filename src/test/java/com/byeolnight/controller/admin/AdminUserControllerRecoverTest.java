package com.byeolnight.controller.admin;

import com.byeolnight.service.auth.SocialAccountCleanupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 계정 복구 서비스 테스트")
class AdminUserControllerRecoverTest {

    @Mock
    private SocialAccountCleanupService socialAccountCleanupService;

    @Test
    @DisplayName("계정 복구 성공")
    void recoverWithdrawnAccount_Success() {
        // given
        String email = "withdrawn@test.com";
        when(socialAccountCleanupService.recoverWithdrawnAccount(email))
                .thenReturn(true);

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount(email);

        // then
        assertTrue(result);
        verify(socialAccountCleanupService).recoverWithdrawnAccount(email);
    }

    @Test
    @DisplayName("복구 불가능한 계정")
    void recoverWithdrawnAccount_NotRecoverable() {
        // given
        String email = "old@test.com";
        when(socialAccountCleanupService.recoverWithdrawnAccount(email))
                .thenReturn(false);

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount(email);

        // then
        assertFalse(result);
        verify(socialAccountCleanupService).recoverWithdrawnAccount(email);
    }

    @Test
    @DisplayName("복구 중 예외 발생")
    void recoverWithdrawnAccount_Exception() {
        // given
        String email = "error@test.com";
        when(socialAccountCleanupService.recoverWithdrawnAccount(email))
                .thenThrow(new RuntimeException("DB 연결 오류"));

        // when & then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> socialAccountCleanupService.recoverWithdrawnAccount(email)
        );

        assertEquals("DB 연결 오류", exception.getMessage());
        verify(socialAccountCleanupService).recoverWithdrawnAccount(email);
    }
}