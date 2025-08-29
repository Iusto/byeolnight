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
                .password(null)
                .build();
        activeSocialUser.setSocialProvider("google");

        // 탈퇴한 소셜 사용자
        withdrawnSocialUser = User.builder()
                .id(2L)
                .email("withdrawn_2@byeolnight.local")
                .nickname("탈퇴회원_2")
                .password(null)
                .build();
        withdrawnSocialUser.setSocialProvider("google");
        withdrawnSocialUser.withdraw("사용자 탈퇴");

        // 일반 사용자
        regularUser = User.builder()
                .id(3L)
                .email("regular@test.com")
                .nickname("일반유저")
                .password("password123")
                .build();
    }

    @Test
    @DisplayName("소셜 사용자 30일 후 마스킹 처리")
    void maskSocialUsersAfterThirtyDays_ShouldMaskExpiredSocialUsers() {
        // given
        LocalDateTime thirtyOneDaysAgo = LocalDateTime.now().minusDays(31);
        
        User expiredSocialUser = User.builder()
                .id(4L)
                .email("expired@test.com")
                .nickname("소셜유저")
                .password(null)
                .status(User.UserStatus.WITHDRAWN)
                .build();
        expiredSocialUser.setSocialProvider("google");
        setWithdrawnAt(expiredSocialUser, thirtyOneDaysAgo);

        User expiredRegularUser = User.builder()
                .id(5L)
                .email("regular@test.com")
                .nickname("일반유저")
                .password("password")
                .status(User.UserStatus.WITHDRAWN)
                .build();
        setWithdrawnAt(expiredRegularUser, thirtyOneDaysAgo);

        List<User> expiredUsers = Arrays.asList(expiredSocialUser, expiredRegularUser);

        when(userRepository.findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class)))
                .thenReturn(expiredUsers);

        // when
        socialAccountCleanupService.maskSocialUsersAfterThirtyDays();

        // then
        verify(userRepository).findByStatusAndWithdrawnAtBefore(
                eq(User.UserStatus.WITHDRAWN), any(LocalDateTime.class));
        
        // 소셜 사용자만 마스킹되었는지 확인
        assertTrue(expiredSocialUser.getEmail().startsWith("withdrawn_"));
        assertEquals("탈퇴회원_4", expiredSocialUser.getNickname());
        
        // 일반 사용자는 마스킹되지 않음 (이미 탈퇴 시 마스킹됨)
        assertEquals("regular@test.com", expiredRegularUser.getEmail());
    }

    @Test
    @DisplayName("소셜 사용자 복구 성공 - 30일 내")
    void recoverWithdrawnAccount_SocialUser_Within30Days_Success() {
        // given
        User socialUser = User.builder()
                .id(6L)
                .email("recover@gmail.com")
                .nickname("소셜유저")
                .password(null)
                .status(User.UserStatus.WITHDRAWN)
                .build();
        socialUser.setSocialProvider("google");
        setWithdrawnAt(socialUser, LocalDateTime.now().minusDays(15));

        when(userRepository.findByEmail("recover@gmail.com"))
                .thenReturn(Optional.of(socialUser));

        // when
        boolean result = socialAccountCleanupService.recoverWithdrawnAccount("recover@gmail.com");

        // then
        assertTrue(result);
        assertEquals(User.UserStatus.ACTIVE, socialUser.getStatus());
        assertNull(socialUser.getWithdrawnAt());
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
                .nickname("일반유저")
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
    @DisplayName("이메일 기반 고유 닉네임 생성")
    void generateUniqueNicknameFromEmail_ShouldGenerateUniqueNickname() {
        // given
        String email = "testuser@gmail.com";
        when(userRepository.existsByNickname("testuser"))
                .thenReturn(false);

        // when
        String nickname = socialAccountCleanupService.generateUniqueNicknameFromEmail(email);

        // then
        assertEquals("testuser", nickname);
    }

    @Test
    @DisplayName("중복 닉네임 시 숫자 접미사 추가")
    void generateUniqueNicknameFromEmail_DuplicateNickname_ShouldAddSuffix() {
        // given
        String email = "testuser@gmail.com";
        when(userRepository.existsByNickname("testuser"))
                .thenReturn(true);
        when(userRepository.existsByNickname("testuser1"))
                .thenReturn(false);

        // when
        String nickname = socialAccountCleanupService.generateUniqueNicknameFromEmail(email);

        // then
        assertEquals("testuser1", nickname);
    }

    @Test
    @DisplayName("복구 가능 여부 확인 - 30일 내 소셜 사용자")
    void canRecover_SocialUser_Within30Days_True() {
        // given
        User socialUser = User.builder()
                .id(9L)
                .email("recoverable@gmail.com")
                .nickname("소셜유저")
                .password(null)
                .status(User.UserStatus.WITHDRAWN)
                .build();
        socialUser.setSocialProvider("google");
        setWithdrawnAt(socialUser, LocalDateTime.now().minusDays(15));

        when(userRepository.findByEmail("recoverable@gmail.com"))
                .thenReturn(Optional.of(socialUser));

        // when
        boolean result = socialAccountCleanupService.canRecover("recoverable@gmail.com");

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("복구 불가능 - 30일 경과 후 마스킹된 소셜 사용자")
    void canRecover_SocialUser_After30Days_False() {
        // given
        User socialUser = User.builder()
                .id(10L)
                .email("withdrawn_10@byeolnight.local")
                .nickname("탈퇴회원_10")
                .password(null)
                .status(User.UserStatus.WITHDRAWN)
                .build();
        socialUser.setSocialProvider("google");
        setWithdrawnAt(socialUser, LocalDateTime.now().minusDays(35));

        when(userRepository.findByEmail("withdrawn_10@byeolnight.local"))
                .thenReturn(Optional.of(socialUser));

        // when
        boolean result = socialAccountCleanupService.canRecover("withdrawn_10@byeolnight.local");

        // then
        assertFalse(result);
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