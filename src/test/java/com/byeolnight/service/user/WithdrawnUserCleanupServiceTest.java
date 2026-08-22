package com.byeolnight.service.user;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("탈퇴 회원 정리 서비스 테스트")
class WithdrawnUserCleanupServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WithdrawnUserCleanupService withdrawnUserCleanupService;

    @Test
    @DisplayName("탈퇴 후 30일이 지난 일반·소셜 회원을 모두 마스킹한다")
    void maskWithdrawnUsersAfterThirtyDays_MasksAllAccountTypes() {
        User socialUser = withdrawnUser(1L, "social@example.com", null);
        socialUser.setSocialProvider("google");
        User passwordUser = withdrawnUser(2L, "regular@example.com", "encoded-password");

        when(userRepository.findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(List.of(socialUser, passwordUser));

        withdrawnUserCleanupService.maskWithdrawnUsersAfterThirtyDays();

        assertTrue(socialUser.getEmail().startsWith("withdrawn_"));
        assertEquals("탈퇴회원_1", socialUser.getNickname());
        assertTrue(passwordUser.getEmail().startsWith("withdrawn_"));
        assertEquals("탈퇴회원_2", passwordUser.getNickname());
    }

    @Test
    @DisplayName("탈퇴 후 2년이 지난 계정은 완전히 삭제한다")
    void cleanupWithdrawnUsers_DeletesExpiredAccounts() {
        User expiredUser = withdrawnUser(3L, "withdrawn_3@byeolnight.local", "encoded-password");
        when(userRepository.findByWithdrawnAtBeforeAndStatusIn(
                any(LocalDateTime.class),
                eq(List.of(User.UserStatus.WITHDRAWN, User.UserStatus.BANNED))))
                .thenReturn(List.of(expiredUser));

        withdrawnUserCleanupService.cleanupWithdrawnUsers();

        verify(userRepository).delete(expiredUser);
    }

    @Test
    @DisplayName("정리 대상이 없으면 삭제를 호출하지 않는다")
    void cleanupWithdrawnUsers_NoTargets_DoesNothing() {
        when(userRepository.findByWithdrawnAtBeforeAndStatusIn(
                any(LocalDateTime.class),
                eq(List.of(User.UserStatus.WITHDRAWN, User.UserStatus.BANNED))))
                .thenReturn(List.of());

        withdrawnUserCleanupService.cleanupWithdrawnUsers();

        verify(userRepository, never()).delete(any());
    }

    private User withdrawnUser(Long id, String email, String password) {
        return User.builder()
                .id(id)
                .email(email)
                .nickname("탈퇴회원")
                .password(password)
                .status(User.UserStatus.WITHDRAWN)
                .withdrawnAt(LocalDateTime.now().minusDays(31))
                .build();
    }
}
