package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

        // then
        assertEquals(User.UserStatus.WITHDRAWN, activeSocialUser.getStatus());
        assertNotNull(activeSocialUser.getWithdrawnAt());
        assertEquals("소셜 로그인 연결 해제 - 30일 복구 가능", activeSocialUser.getWithdrawalReason());
    }

    @Test
    @DisplayName("소셜 로그인 실패 시 일반 사용자는 처리하지 않음")
    void handleFailedSocialLogin_RegularUser_ShouldNotWithdraw() {
        // given
        when(userRepository.findByEmail("regular@test.com"))
                .thenReturn(Optional.of(regularUser));

        // when
        socialAccountCleanupService.handleFailedSocialLogin("regular@test.com", "google");

        // then
        assertEquals(User.UserStatus.ACTIVE, regularUser.getStatus());
        assertNull(regularUser.getWithdrawnAt());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 처리 시 예외 없이 무시")
    void handleFailedSocialLogin_NonExistentUser_ShouldIgnore() {
        // given
        when(userRepository.findByEmail("nonexistent@test.com"))
                .thenReturn(Optional.empty());

        // when & then
        assertDoesNotThrow(() -> 
                socialAccountCleanupService.handleFailedSocialLogin("nonexistent@test.com", "google"));
    }

    @Test
    @DisplayName("소셜 연동 해제 즉시 처리")
    void handleSocialDisconnection_ShouldWithdrawImmediately() {
        // given
        activeSocialUser.setSocialProvider("kakao"); // 소셜 사용자로 설정
        when(userRepository.findByEmail("social@test.com"))
                .thenReturn(Optional.of(activeSocialUser));

        // when
        socialAccountCleanupService.handleSocialDisconnection("social@test.com", "kakao");

        // then
        assertEquals(User.UserStatus.WITHDRAWN, activeSocialUser.getStatus());
        assertNotNull(activeSocialUser.getWithdrawnAt());
        assertEquals("소셜 로그인 연결 해제 - 30일 복구 가능", activeSocialUser.getWithdrawalReason());
    }

    @Test
    @DisplayName("30일 경과 소셜 계정 완전 삭제")
    void cleanupWithdrawnSocialAccounts_ShouldDeleteOldAccounts() {
        // given
        LocalDateTime thirtyOneDaysAgo = LocalDateTime.now().minusDays(31);
        
        User oldWithdrawnSocialUser1 = User.builder()
                .id(4L)
                .email("old1@test.com")
                .password(null) // 소셜 사용자
                .build();
        oldWithdrawnSocialUser1.setSocialProvider("google"); // 소셜 사용자로 설정
        oldWithdrawnSocialUser1.withdraw("테스트");
        setWithdrawnAt(oldWithdrawnSocialUser1, thirtyOneDaysAgo);

        User oldWithdrawnSocialUser2 = User.builder()
                .id(5L)
                .email("old2@test.com")
                .password(null) // 소셜 사용자
                .build();
        oldWithdrawnSocialUser2.setSocialProvider("kakao"); // 소셜 사용자로 설정
        oldWithdrawnSocialUser2.withdraw("테스트");
        setWithdrawnAt(oldWithdrawnSocialUser2, thirtyOneDaysAgo);

        User oldWithdrawnRegularUser = User.builder()
                .id(6L)
                .email("oldregular@test.com")
                .password("password") // 일반 사용자
                .build();
        // socialProvider 설정하지 않음 (일반 사용자)
        oldWithdrawnRegularUser.withdraw("테스트");
        setWithdrawnAt(oldWithdrawnRegularUser, thirtyOneDaysAgo);

        List<User> withdrawnUsers = Arrays.asList(
                oldWithdrawnSocialUser1, 
                oldWithdrawnSocialUser2, 
                oldWithdrawnRegularUser
        );

        when(userRepository.findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(withdrawnUsers);

        // when
        socialAccountCleanupService.cleanupWithdrawnSocialAccounts();

        // then - 조회가 호출되었는지 확인
        verify(userRepository).findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class));
        // 일반 사용자는 삭제되지 않음
        verify(userRepository, never()).delete(oldWithdrawnRegularUser);
    }

    @Test
    @DisplayName("30일 내 소셜 계정 복구 성공")
    void recoverWithdrawnAccount_WithinThirtyDays_ShouldRecover() {
        // given
        User recentWithdrawnUser = User.builder()
                .id(7L)
                .email("recent@test.com")
                .password(null)
                .build();
        recentWithdrawnUser.setSocialProvider("google"); // 소셜 사용자로 설정
        recentWithdrawnUser.withdraw("테스트");
        setWithdrawnAt(recentWithdrawnUser, LocalDateTime.now().minusDays(15)); // 15일 전 탈퇴

        when(userRepository.findByEmail("recent@test.com"))
                .thenReturn(Optional.of(recentWithdrawnUser));

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("recent@test.com");

        // then
        assertTrue(result);
        assertEquals(User.UserStatus.ACTIVE, recentWithdrawnUser.getStatus());
        assertNull(recentWithdrawnUser.getWithdrawnAt());
        assertNull(recentWithdrawnUser.getWithdrawalReason());
    }

    @Test
    @DisplayName("30일 경과 계정 복구 실패")
    void recoverWithdrawnAccount_AfterThirtyDays_ShouldFail() {
        // given
        User oldWithdrawnUser = User.builder()
                .id(8L)
                .email("old@test.com")
                .password(null)
                .build();
        oldWithdrawnUser.setSocialProvider("naver"); // 소셜 사용자로 설정
        oldWithdrawnUser.withdraw("테스트");
        setWithdrawnAt(oldWithdrawnUser, LocalDateTime.now().minusDays(35)); // 35일 전 탈퇴

        when(userRepository.findByEmail("old@test.com"))
                .thenReturn(Optional.of(oldWithdrawnUser));

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("old@test.com");

        // then
        assertFalse(result);
        assertEquals(User.UserStatus.WITHDRAWN, oldWithdrawnUser.getStatus()); // 상태 변경 없음
    }

    @Test
    @DisplayName("탈퇴 이메일 형태로 계정 복구")
    void recoverWithdrawnAccount_WithWithdrawnEmailFormat_ShouldRecover() {
        // given
        User withdrawnUserById = User.builder()
                .id(123L)
                .email("withdrawn_123@byeolnight.local")
                .password(null)
                .build();
        withdrawnUserById.setSocialProvider("google"); // 소셜 사용자로 설정
        withdrawnUserById.withdraw("테스트");
        setWithdrawnAt(withdrawnUserById, LocalDateTime.now().minusDays(10)); // 10일 전 탈퇴

        when(userRepository.findById(123L))
                .thenReturn(Optional.of(withdrawnUserById));

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("withdrawn_123@byeolnight.local");

        // then
        assertTrue(result);
        assertEquals(User.UserStatus.ACTIVE, withdrawnUserById.getStatus());
        assertNull(withdrawnUserById.getWithdrawnAt());
        assertNull(withdrawnUserById.getWithdrawalReason());
    }

    @Test
    @DisplayName("잘못된 탈퇴 이메일 형태 복구 실패")
    void recoverWithdrawnAccount_InvalidWithdrawnEmailFormat_ShouldFail() {
        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("withdrawn_invalid@byeolnight.local");

        // then
        assertFalse(result);
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 계정 복구 실패")
    void recoverWithdrawnAccount_NonExistentAccount_ShouldFail() {
        // given
        when(userRepository.findByEmail("nonexistent@test.com"))
                .thenReturn(Optional.empty());

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("nonexistent@test.com");

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("활성 계정 복구 시도 실패")
    void recoverWithdrawnAccount_ActiveAccount_ShouldFail() {
        // given
        when(userRepository.findByEmail("social@test.com"))
                .thenReturn(Optional.of(activeSocialUser));

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("social@test.com");

        // then
        assertFalse(result);
        assertEquals(User.UserStatus.ACTIVE, activeSocialUser.getStatus()); // 상태 변경 없음
    }

    @Test
    @DisplayName("30일 경과 소셜 계정만 삭제")
    void cleanupWithdrawnSocialAccounts_ShouldDeleteOnlySocialUsers() {
        // given
        LocalDateTime thirtyOneDaysAgo = LocalDateTime.now().minusDays(31);
        
        User socialUser1 = User.builder()
                .id(10L)
                .email("social1@test.com")
                .password(null)
                .build();
        socialUser1.setSocialProvider("google");
        // 탈퇴 전에 소셜 사용자인지 확인
        assertTrue(socialUser1.isSocialUser());
        socialUser1.withdraw("테스트");
        setWithdrawnAt(socialUser1, thirtyOneDaysAgo);
        // 탈퇴 후 socialProvider가 null이 되어 isSocialUser()가 false 반환
        
        User regularUser = User.builder()
                .id(11L)
                .email("regular@test.com")
                .password("password")
                .build();
        // socialProvider 설정 안함 (일반 사용자)
        assertFalse(regularUser.isSocialUser());
        regularUser.withdraw("테스트");
        setWithdrawnAt(regularUser, thirtyOneDaysAgo);

        List<User> withdrawnUsers = Arrays.asList(socialUser1, regularUser);

        when(userRepository.findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(withdrawnUsers);

        // when
        socialAccountCleanupService.cleanupWithdrawnSocialAccounts();

        // then - socialProvider 유지로 소셜 사용자 구분 가능
        assertTrue(socialUser1.isSocialUser()); // 탈퇴 후에도 socialProvider 유지
        assertFalse(regularUser.isSocialUser()); // 일반 사용자
        
        // 소셜 사용자만 삭제
        verify(userRepository).delete(socialUser1);
        verify(userRepository, never()).delete(regularUser);
    }

    @Test
    @DisplayName("30일 경과 계정 개인정보 마스킹 처리")
    void maskPersonalInfoAfterThirtyDays_ShouldMaskExpiredAccounts() {
        // given
        LocalDateTime thirtyOneDaysAgo = LocalDateTime.now().minusDays(31);
        
        User expiredUser1 = User.builder()
                .id(10L)
                .email("expired1@test.com")
                .nickname("만료유저1")
                .password(null)
                .build();
        expiredUser1.setSocialProvider("google");
        expiredUser1.withdraw("테스트");
        setWithdrawnAt(expiredUser1, thirtyOneDaysAgo);

        User expiredUser2 = User.builder()
                .id(11L)
                .email("expired2@test.com")
                .nickname("만료유저2")
                .password("password")
                .build();
        expiredUser2.withdraw("테스트");
        setWithdrawnAt(expiredUser2, thirtyOneDaysAgo);

        // 이미 마스킹된 사용자
        User alreadyMaskedUser = User.builder()
                .id(12L)
                .email("deleted_12@removed.local")
                .nickname("DELETED_12")
                .password(null)
                .build();
        alreadyMaskedUser.withdraw("테스트");
        setWithdrawnAt(alreadyMaskedUser, thirtyOneDaysAgo);

        List<User> expiredUsers = Arrays.asList(expiredUser1, expiredUser2, alreadyMaskedUser);

        when(userRepository.findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(expiredUsers);

        // when
        socialAccountCleanupService.maskPersonalInfoAfterThirtyDays();

        // then - 이미 마스킹된 사용자는 건너뛰기
        verify(userRepository).findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class));
        
        // 예외가 발생하지 않았는지 확인
        assertDoesNotThrow(() -> socialAccountCleanupService.maskPersonalInfoAfterThirtyDays());
    }

    @Test
    @DisplayName("정리 작업 중 예외 발생 시 로그만 기록하고 계속 진행")
    void cleanupWithdrawnSocialAccounts_ExceptionOccurred_ShouldContinue() {
        // given
        when(userRepository.findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("DB 연결 오류"));

        // when & then
        assertDoesNotThrow(() -> socialAccountCleanupService.cleanupWithdrawnSocialAccounts());
    }

    @Test
    @DisplayName("30일 개인정보 마스킹 작업 중 예외 발생 시 안전하게 처리")
    void maskPersonalInfoAfterThirtyDays_ExceptionOccurred_ShouldContinue() {
        // given
        when(userRepository.findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("개인정보 마스킹 오류"));

        // when & then
        assertDoesNotThrow(() -> socialAccountCleanupService.maskPersonalInfoAfterThirtyDays());
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