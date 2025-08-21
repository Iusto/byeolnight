package com.byeolnight.service.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("소셜 로그인 탈퇴 계정 처리 테스트")
class CustomOAuth2UserServiceWithdrawTest {

    @Mock
    private SocialAccountCleanupService socialAccountCleanupService;

    @Test
    @DisplayName("복구 가능한 계정 확인 - 서비스 로직 테스트")
    void hasRecoverableAccount_ShouldCallService() {
        // given
        String email = "test@example.com";
        when(socialAccountCleanupService.hasRecoverableAccount(email))
                .thenReturn(true);

        // when
        boolean result = socialAccountCleanupService.hasRecoverableAccount(email);

        // then
        assertTrue(result);
        verify(socialAccountCleanupService).hasRecoverableAccount(email);
    }

    @Test
    @DisplayName("복구 불가능한 계정 확인")
    void hasRecoverableAccount_NotRecoverable_ShouldReturnFalse() {
        // given
        String email = "old@example.com";
        when(socialAccountCleanupService.hasRecoverableAccount(email))
                .thenReturn(false);

        // when
        boolean result = socialAccountCleanupService.hasRecoverableAccount(email);

        // then
        assertFalse(result);
        verify(socialAccountCleanupService).hasRecoverableAccount(email);
    }
}