package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("계정 복구 서비스 테스트")
class AccountRecoveryServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountRecoveryService accountRecoveryService;

    @Test
    @DisplayName("관리자는 30일 이내 탈퇴 계정을 이메일로 복구할 수 있다")
    void recoverWithdrawnAccount_WithinGracePeriod_Succeeds() {
        User user = withdrawnPasswordUser(1L, "member@example.com", 15);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        boolean recovered = accountRecoveryService.recoverWithdrawnAccount(user.getEmail());

        assertTrue(recovered);
        assertEquals(User.UserStatus.ACTIVE, user.getStatus());
        assertNull(user.getWithdrawnAt());
    }

    @Test
    @DisplayName("30일이 지나 마스킹된 계정은 복구할 수 없다")
    void recoverWithdrawnAccount_AfterGracePeriod_Fails() {
        User user = withdrawnPasswordUser(2L, "withdrawn_2@byeolnight.local", 35);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertFalse(accountRecoveryService.recoverWithdrawnAccount(user.getEmail()));
    }

    @Test
    @DisplayName("OAuth 복구는 티켓의 제공자 고유 ID가 일치할 때만 성공한다")
    void recoverOAuthAccount_MatchingIdentity_Succeeds() {
        User user = withdrawnSocialUser(3L, "oauth@example.com", "google-subject");
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        User recovered = accountRecoveryService.recoverOAuthAccount(
                3L, "google", "google-subject");

        assertEquals(User.UserStatus.ACTIVE, recovered.getStatus());
        assertNull(recovered.getWithdrawnAt());
    }

    @Test
    @DisplayName("OAuth 복구는 다른 제공자 고유 ID를 거부한다")
    void recoverOAuthAccount_MismatchedIdentity_Fails() {
        User user = withdrawnSocialUser(4L, "oauth2@example.com", "original-subject");
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () ->
                accountRecoveryService.recoverOAuthAccount(4L, "google", "different-subject"));
    }

    @Test
    @DisplayName("비밀번호 복구 티켓으로 소셜 계정을 복구할 수 없다")
    void recoverPasswordAccount_SocialUser_Fails() {
        User user = withdrawnSocialUser(5L, "social@example.com", "google-subject");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> accountRecoveryService.recoverPasswordAccount(5L));
    }

    @Test
    @DisplayName("탈퇴 후 30일 이내이고 마스킹되지 않은 계정만 복구 가능하다")
    void canRecover_WithinGracePeriod_ReturnsTrue() {
        User user = withdrawnPasswordUser(6L, "recoverable@example.com", 10);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertTrue(accountRecoveryService.canRecover(user.getEmail()));
    }

    private User withdrawnPasswordUser(Long id, String email, int daysAgo) {
        return User.builder()
                .id(id)
                .email(email)
                .nickname("일반회원")
                .password("encoded-password")
                .status(User.UserStatus.WITHDRAWN)
                .withdrawnAt(LocalDateTime.now().minusDays(daysAgo))
                .build();
    }

    private User withdrawnSocialUser(Long id, String email, String providerUserId) {
        User user = User.builder()
                .id(id)
                .email(email)
                .nickname("소셜회원")
                .status(User.UserStatus.WITHDRAWN)
                .withdrawnAt(LocalDateTime.now().minusDays(2))
                .build();
        user.linkSocialIdentity("google", providerUserId);
        return user;
    }
}
