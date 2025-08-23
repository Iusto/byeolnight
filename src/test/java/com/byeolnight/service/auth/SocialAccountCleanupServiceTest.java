package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("소셜 계정 정리 서비스 테스트")
class SocialAccountCleanupServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SocialAccountCleanupService socialAccountCleanupService;

    private User activeSocialUser;
    private User withdrawnSocialUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        // 활성 소셜 사용자
        activeSocialUser = User.builder()
                .id(1L)
                .email("social@test.com")
                .nickname("소셜유저")
                .password(null) // 소셜 사용자는 비밀번호 없음
                .build();

        // 탈퇴한 소셜 사용자
        withdrawnSocialUser = User.builder()
                .id(2L)
                .email("withdrawn_2@byeolnight.local")
                .nickname("탈퇴유저")
                .password(null)
                .build();
        withdrawnSocialUser.withdraw("소셜 연동 해제");

        // 일반 사용자
        regularUser = User.builder()
                .id(3L)
                .email("regular@test.com")
                .nickname("일반유저")
                .password("password123")
                .build();
    }

    @Test
    @DisplayName("소셜 로그인 실패 시 활성 소셜 사용자 탈퇴 처리")
    void handleFailedSocialLogin_ActiveSocialUser_ShouldWithdraw() {
        // given
        activeSocialUser.setSocialProvider("google"); // 소셜 사용자로 설정
        when(userRepository.findByEmail("social@test.com"))
                .thenReturn(Optional.of(activeSocialUser))
                .thenReturn(Optional.of(activeSocialUser)); // handleSocialDisconnection에서 한 번 더 호출

        // when
        socialAccountCleanupService.handleFailedSocialLogin("social@test.com", "google");

        // then - 모든 사용자는 탈퇴 시 WITHDRAWN 상태로 변경
        assertEquals(User.UserStatus.WITHDRAWN, activeSocialUser.getStatus());
        assertNotNull(activeSocialUser.getWithdrawnAt());
        assertEquals("소셜 로그인 연결 해제 - 30일 복구 가능", activeSocialUser.getWithdrawalReason());
    }

    @Test
    @DisplayName("30일 후 이메일 마스킹 처리")
    void maskEmailAfterThirtyDays_ShouldMaskExpiredUsers() {
        // given
        LocalDateTime thirtyOneDaysAgo = LocalDateTime.now().minusDays(31);
        
        User expiredSocialUser = User.builder()
                .id(4L)
                .email("expired@test.com")
                .nickname("탈퇴회원_4")
                .password(null)
                .status(User.UserStatus.WITHDRAWN)
                .build();
        expiredSocialUser.setSocialProvider("google");
        setWithdrawnAt(expiredSocialUser, thirtyOneDaysAgo);

        User expiredRegularUser = User.builder()
                .id(5L)
                .email("regular@test.com")
                .nickname("탈퇴회원_5")
                .password("password")
                .status(User.UserStatus.WITHDRAWN)
                .build();
        setWithdrawnAt(expiredRegularUser, thirtyOneDaysAgo);

        List<User> expiredUsers = Arrays.asList(expiredSocialUser, expiredRegularUser);

        when(userRepository.findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(expiredUsers);

        // when
        socialAccountCleanupService.maskEmailAfterThirtyDays();

        // then
        verify(userRepository).findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class));
        
        // 이메일이 마스킹되었는지 확인
        assertTrue(expiredSocialUser.getEmail().startsWith("withdrawn_"));
        assertTrue(expiredRegularUser.getEmail().startsWith("withdrawn_"));
    }

    @Test
    @DisplayName("소셜 사용자 복구 성공 - 30일 내")
    void recoverWithdrawnAccount_SocialUser_Within30Days_Success() {
        // given
        User socialUser = User.builder()
                .id(6L)
                .email("recover@gmail.com")
                .nickname("탈퇴회원_6")
                .password(null)
                .status(User.UserStatus.WITHDRAWN)
                .build();
        socialUser.setSocialProvider("google");
        setWithdrawnAt(socialUser, LocalDateTime.now().minusDays(15));

        when(userRepository.findByEmail("recover@gmail.com"))
                .thenReturn(Optional.of(socialUser));
        when(userRepository.existsByNickname("recover"))
                .thenReturn(false);

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("recover@gmail.com");

        // then
        assertTrue(result);
        assertEquals(User.UserStatus.ACTIVE, socialUser.getStatus());
        assertNull(socialUser.getWithdrawnAt());
        assertEquals("recover", socialUser.getNickname());
    }

    @Test
    @DisplayName("소셜 사용자 복구 실패 - 이메일 마스킹됨")
    void recoverWithdrawnAccount_SocialUser_EmailMasked_Fail() {
        // given
        User socialUser = User.builder()
                .id(7L)
                .email("withdrawn_7@byeolnight.local") // 이미 마스킹된 이메일
                .nickname("탈퇴회원_7")
                .password(null)
                .status(User.UserStatus.WITHDRAWN)
                .build();
        socialUser.setSocialProvider("google");
        setWithdrawnAt(socialUser, LocalDateTime.now().minusDays(35));

        when(userRepository.findByEmail("withdrawn_7@byeolnight.local"))
                .thenReturn(Optional.of(socialUser));

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("withdrawn_7@byeolnight.local");

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("일반 사용자 복구 불가")
    void recoverWithdrawnAccount_RegularUser_Fail() {
        // given
        User regularUser = User.builder()
                .id(8L)
                .email("regular@test.com")
                .nickname("탈퇴회원_8")
                .password("password")
                .status(User.UserStatus.WITHDRAWN)
                .build();
        setWithdrawnAt(regularUser, LocalDateTime.now().minusDays(15));

        when(userRepository.findByEmail("regular@test.com"))
                .thenReturn(Optional.of(regularUser));

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("regular@test.com");

        // then
        assertFalse(result); // 일반 사용자는 복구 불가
    }

    @Test
    @DisplayName("10년 경과 계정 완전 삭제")
    void cleanupWithdrawnAccounts_After10Years_CompleteDelete() {
        // given
        LocalDateTime tenYearsAgo = LocalDateTime.now().minusYears(10).minusDays(1);
        
        User oldUser = User.builder()
                .id(9L)
                .email("deleted_9@removed.local")
                .nickname("DELETED_9")
                .password(null)
                .status(User.UserStatus.WITHDRAWN)
                .build();
        setWithdrawnAt(oldUser, tenYearsAgo);

        List<User> oldUsers = List.of(oldUser);

        when(userRepository.findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(oldUsers);

        // when
        socialAccountCleanupService.cleanupWithdrawnAccounts();

        // then
        verify(userRepository).delete(oldUser);
    }

    @Test
    @DisplayName("5년 경과 계정 개인정보 완전 마스킹")
    void completelyMaskPersonalInfoAfterFiveYears_ShouldMaskOldAccounts() {
        // given
        LocalDateTime fiveYearsAgo = LocalDateTime.now().minusYears(5).minusDays(1);
        
        User oldUser = User.builder()
                .id(10L)
                .email("withdrawn_10@byeolnight.local")
                .nickname("탈퇴회원_10")
                .password(null)
                .status(User.UserStatus.WITHDRAWN)
                .build();
        setWithdrawnAt(oldUser, fiveYearsAgo);

        List<User> oldUsers = List.of(oldUser);

        when(userRepository.findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(oldUsers);

        // when
        socialAccountCleanupService.completelyMaskPersonalInfoAfterFiveYears();

        // then
        verify(userRepository).findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class));
        
        // 개인정보가 완전 마스킹되었는지 확인
        assertEquals("DELETED_10", oldUser.getNickname());
        assertEquals("deleted_10@removed.local", oldUser.getEmail());
    }

    // 리플렉션을 사용하여 withdrawnAt 필드 설정
    private void setWithdrawnAt(User user, LocalDateTime withdrawnAt) {
        try {
            Field field = User.class.getDeclaredField("withdrawnAt");
            field.setAccessible(true);
            field.set(user, withdrawnAt);
        } catch (Exception e) {
            throw new RuntimeException("withdrawnAt 필드 설정 실패", e);
        }
    }
}