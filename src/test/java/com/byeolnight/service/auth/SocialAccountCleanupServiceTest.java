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
    @DisplayName("복구 가능한 계정 확인 - 30일 내 탈퇴한 소셜 사용자만")
    void hasRecoverableAccount_WithinThirtyDays_ShouldReturnTrue() {
        // given
        User recentWithdrawnSocialUser = User.builder()
                .id(7L)
                .email("recent@test.com")
                .password(null)
                .build();
        recentWithdrawnSocialUser.setSocialProvider("google"); // 소셜 사용자
        recentWithdrawnSocialUser.withdraw("테스트");
        setWithdrawnAt(recentWithdrawnSocialUser, LocalDateTime.now().minusDays(15)); // 15일 전 탈퇴

        when(userRepository.findByEmail("recent@test.com"))
                .thenReturn(Optional.of(recentWithdrawnSocialUser));

        // when
        boolean result = socialAccountCleanupService.hasRecoverableAccount("recent@test.com");

        // then
        assertTrue(result);
        // 상태는 변경되지 않음 (확인만 함)
        assertEquals(User.UserStatus.WITHDRAWN, recentWithdrawnSocialUser.getStatus());
        assertNotNull(recentWithdrawnSocialUser.getWithdrawnAt());
    }

    @Test
    @DisplayName("복구 가능한 계정 확인 - 일반 사용자는 복구 불가")
    void hasRecoverableAccount_RegularUser_ShouldReturnFalse() {
        // given
        User recentWithdrawnRegularUser = User.builder()
                .id(11L)
                .email("regular@test.com")
                .password("password")
                .build();
        // socialProvider 설정 안함 (일반 사용자)
        recentWithdrawnRegularUser.withdraw("테스트");
        setWithdrawnAt(recentWithdrawnRegularUser, LocalDateTime.now().minusDays(15)); // 15일 전 탈퇴

        when(userRepository.findByEmail("regular@test.com"))
                .thenReturn(Optional.of(recentWithdrawnRegularUser));

        // when
        boolean result = socialAccountCleanupService.hasRecoverableAccount("regular@test.com");

        // then
        assertFalse(result); // 일반 사용자는 복구 불가
    }

    @Test
    @DisplayName("복구 가능한 계정 확인 - 30일 경과")
    void hasRecoverableAccount_AfterThirtyDays_ShouldReturnFalse() {
        // given
        User oldWithdrawnUser = User.builder()
                .id(8L)
                .email("old@test.com")
                .password(null)
                .build();
        oldWithdrawnUser.setSocialProvider("naver");
        oldWithdrawnUser.withdraw("테스트");
        setWithdrawnAt(oldWithdrawnUser, LocalDateTime.now().minusDays(35)); // 35일 전 탈퇴

        when(userRepository.findByEmail("old@test.com"))
                .thenReturn(Optional.of(oldWithdrawnUser));

        // when
        boolean result = socialAccountCleanupService.hasRecoverableAccount("old@test.com");

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("복구 가능한 계정 확인 - 활성 계정")
    void hasRecoverableAccount_ActiveAccount_ShouldReturnFalse() {
        // given
        when(userRepository.findByEmail("social@test.com"))
                .thenReturn(Optional.of(activeSocialUser));

        // when
        boolean result = socialAccountCleanupService.hasRecoverableAccount("social@test.com");

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("30일 내 소셜 계정 복구 성공 - 닉네임 자동 생성")
    void recoverWithdrawnAccount_WithinThirtyDays_ShouldRecover() {
        // given
        User recentWithdrawnUser = User.builder()
                .id(7L)
                .email("testuser@gmail.com")
                .nickname("탈퇴회원_7")
                .password(null)
                .build();
        recentWithdrawnUser.setSocialProvider("google"); // 소셜 사용자로 설정
        recentWithdrawnUser.withdraw("테스트");
        setWithdrawnAt(recentWithdrawnUser, LocalDateTime.now().minusDays(15)); // 15일 전 탈퇴

        when(userRepository.findByEmail("testuser@gmail.com"))
                .thenReturn(Optional.of(recentWithdrawnUser));
        when(userRepository.existsByNickname("testuser"))
                .thenReturn(false); // 닉네임 사용 가능

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("testuser@gmail.com");

        // then
        assertTrue(result);
        assertEquals(User.UserStatus.ACTIVE, recentWithdrawnUser.getStatus());
        assertNull(recentWithdrawnUser.getWithdrawnAt());
        assertNull(recentWithdrawnUser.getWithdrawalReason());
        assertEquals("testuser", recentWithdrawnUser.getNickname()); // 이메일 기반 닉네임 생성
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
    @DisplayName("닉네임 중복 시 숫자 접미사 추가")
    void recoverWithdrawnAccount_DuplicateNickname_ShouldAddSuffix() {
        // given
        User recentWithdrawnUser = User.builder()
                .id(8L)
                .email("popular@gmail.com")
                .nickname("탈퇴회원_8")
                .password(null)
                .build();
        recentWithdrawnUser.setSocialProvider("google");
        recentWithdrawnUser.withdraw("테스트");
        setWithdrawnAt(recentWithdrawnUser, LocalDateTime.now().minusDays(10));

        when(userRepository.findByEmail("popular@gmail.com"))
                .thenReturn(Optional.of(recentWithdrawnUser));
        when(userRepository.existsByNickname("popular"))
                .thenReturn(true); // 기본 닉네임 중복
        when(userRepository.existsByNickname("popular1"))
                .thenReturn(false); // 숫자 접미사 추가한 닉네임 사용 가능

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("popular@gmail.com");

        // then
        assertTrue(result);
        assertEquals(User.UserStatus.ACTIVE, recentWithdrawnUser.getStatus());
        assertEquals("popular1", recentWithdrawnUser.getNickname()); // 숫자 접미사 추가
    }

    @Test
    @DisplayName("짧은 이메일 닉네임 처리")
    void recoverWithdrawnAccount_ShortEmailNickname_ShouldUseDefault() {
        // given
        User recentWithdrawnUser = User.builder()
                .id(9L)
                .email("a@test.com")
                .nickname("탈퇴회원_9")
                .password(null)
                .build();
        recentWithdrawnUser.setSocialProvider("kakao");
        recentWithdrawnUser.withdraw("테스트");
        setWithdrawnAt(recentWithdrawnUser, LocalDateTime.now().minusDays(5));

        when(userRepository.findByEmail("a@test.com"))
                .thenReturn(Optional.of(recentWithdrawnUser));
        when(userRepository.existsByNickname("사용자"))
                .thenReturn(false);

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("a@test.com");

        // then
        assertTrue(result);
        assertEquals("사용자", recentWithdrawnUser.getNickname()); // 기본 닉네임 사용
    }

    @Test
    @DisplayName("긴 이메일 닉네임 8자로 제한")
    void recoverWithdrawnAccount_LongEmailNickname_ShouldTruncate() {
        // given
        User recentWithdrawnUser = User.builder()
                .id(10L)
                .email("verylongusername@test.com")
                .nickname("탈퇴회원_10")
                .password(null)
                .build();
        recentWithdrawnUser.setSocialProvider("naver");
        recentWithdrawnUser.withdraw("테스트");
        setWithdrawnAt(recentWithdrawnUser, LocalDateTime.now().minusDays(7));

        when(userRepository.findByEmail("verylongusername@test.com"))
                .thenReturn(Optional.of(recentWithdrawnUser));
        when(userRepository.existsByNickname("verylong"))
                .thenReturn(false);

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("verylongusername@test.com");

        // then
        assertTrue(result);
        assertEquals("verylong", recentWithdrawnUser.getNickname()); // 8자로 제한
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
    @DisplayName("탈퇴 신청 후 30일 이전 사용자 재로그인 시 복구 처리 완전 성공")
    void recoverWithdrawnAccount_Within30Days_CompleteRecovery() {
        // given - 15일 전 탈퇴한 소셜 사용자
        User withdrawnUser = User.builder()
                .id(100L)
                .email("recover@gmail.com")
                .nickname("탈퇴회원_100")
                .password(null)
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .points(500) // 기존 포인트
                .build();
        withdrawnUser.setSocialProvider("google");
        withdrawnUser.withdraw("사용자 요청");
        setWithdrawnAt(withdrawnUser, LocalDateTime.now().minusDays(15)); // 15일 전 탈퇴

        when(userRepository.findByEmail("recover@gmail.com"))
                .thenReturn(Optional.of(withdrawnUser));
        when(userRepository.existsByNickname("recover"))
                .thenReturn(false);

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("recover@gmail.com");

        // then - 완전 복구 확인
        assertTrue(result);
        assertEquals(User.UserStatus.ACTIVE, withdrawnUser.getStatus());
        assertNull(withdrawnUser.getWithdrawnAt());
        assertNull(withdrawnUser.getWithdrawalReason());
        assertEquals("recover", withdrawnUser.getNickname()); // 이메일 기반 닉네임
        assertEquals(500, withdrawnUser.getPoints()); // 기존 포인트 유지
        assertEquals("google", withdrawnUser.getSocialProvider()); // 소셜 제공자 유지
    }

    @Test
    @DisplayName("탈퇴 신청 후 30일 경과 사용자 재로그인 시 복구 불가 - 새 계정 처리")
    void hasRecoverableAccount_After30Days_ShouldReturnFalse_NewAccountProcess() {
        // given - 35일 전 탈퇴한 소셜 사용자
        User expiredWithdrawnUser = User.builder()
                .id(200L)
                .email("expired@gmail.com")
                .nickname("탈퇴회원_200")
                .password(null)
                .build();
        expiredWithdrawnUser.setSocialProvider("kakao");
        expiredWithdrawnUser.withdraw("사용자 요청");
        setWithdrawnAt(expiredWithdrawnUser, LocalDateTime.now().minusDays(35)); // 35일 전 탈퇴

        when(userRepository.findByEmail("expired@gmail.com"))
                .thenReturn(Optional.of(expiredWithdrawnUser));

        // when - 복구 가능 여부 확인
        boolean isRecoverable = socialAccountCleanupService.hasRecoverableAccount("expired@gmail.com");
        boolean recoverResult = socialAccountCleanupService.recoverWithdrawnAccount("expired@gmail.com");

        // then - 복구 불가, 새 계정 처리 필요
        assertFalse(isRecoverable); // 30일 경과로 복구 불가
        assertFalse(recoverResult); // 복구 실패
        assertEquals(User.UserStatus.WITHDRAWN, expiredWithdrawnUser.getStatus()); // 상태 변경 없음
    }

    @Test
    @DisplayName("탈퇴 신청 후 30일 경과 유저 Soft Delete 및 연동 해제 처리")
    void maskPersonalInfoAfterThirtyDays_ShouldSoftDeleteAndDisconnect() {
        // given - 31일 전 탈퇴한 소셜 사용자
        LocalDateTime thirtyOneDaysAgo = LocalDateTime.now().minusDays(31);
        
        User expiredSocialUser = User.builder()
                .id(300L)
                .email("softdelete@gmail.com")
                .nickname("소프트삭제유저")
                .password(null)
                .build();
        expiredSocialUser.setSocialProvider("naver");
        expiredSocialUser.withdraw("사용자 요청");
        setWithdrawnAt(expiredSocialUser, thirtyOneDaysAgo);

        List<User> expiredUsers = List.of(expiredSocialUser);

        when(userRepository.findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(expiredUsers);

        // when - 30일 경과 계정 개인정보 마스킹 실행
        socialAccountCleanupService.maskPersonalInfoAfterThirtyDays();

        // then - Soft Delete 처리 확인
        verify(userRepository).findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class));
        
        // 개인정보 마스킹이 실제로 실행되었는지 확인
        // maskPersonalInfoAfterThirtyDays() 메서드가 completelyRemovePersonalInfo()를 호출하여 마스킹 완료
        assertEquals("DELETED_300", expiredSocialUser.getNickname()); // 마스킹된 닉네임
        assertEquals("deleted_300@removed.local", expiredSocialUser.getEmail()); // 마스킹된 이메일
        assertEquals("5년 경과로 인한 자동 삭제", expiredSocialUser.getWithdrawalReason()); // 마스킹 사유
        assertTrue(expiredSocialUser.isSocialUser()); // 소셜 제공자 정보는 여전히 유지
        assertEquals(User.UserStatus.WITHDRAWN, expiredSocialUser.getStatus()); // 상태는 여전히 WITHDRAWN
    }

    @Test
    @DisplayName("탈퇴 신청 후 5년 경과 소셜 계정 완전 삭제 및 연동 해제")
    void cleanupWithdrawnSocialAccounts_After5Years_CompleteDelete() {
        // given - 5년 1일 전 탈퇴한 소셜 사용자
        LocalDateTime fiveYearsAgo = LocalDateTime.now().minusYears(5).minusDays(1);
        
        User fiveYearOldUser = User.builder()
                .id(400L)
                .email("deleted_400@removed.local") // 이미 마스킹된 상태
                .nickname("DELETED_400")
                .password(null)
                .build();
        fiveYearOldUser.setSocialProvider("google"); // 소셜 제공자 정보 유지
        fiveYearOldUser.withdraw("사용자 요청");
        setWithdrawnAt(fiveYearOldUser, fiveYearsAgo);

        List<User> fiveYearOldUsers = List.of(fiveYearOldUser);

        when(userRepository.findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(fiveYearOldUsers);

        // when - 5년 경과 소셜 계정 완전 삭제
        socialAccountCleanupService.cleanupWithdrawnSocialAccounts();

        // then - 완전 삭제 확인
        verify(userRepository).findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class));
        verify(userRepository).delete(fiveYearOldUser); // 소셜 사용자만 삭제
        
        assertTrue(fiveYearOldUser.isSocialUser()); // 삭제 전 소셜 사용자 확인
        // 연동 해제 및 완전 삭제 처리 확인
        assertNotNull(fiveYearOldUser.getSocialProvider()); // 삭제 전까지 소셜 제공자 정보 유지
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