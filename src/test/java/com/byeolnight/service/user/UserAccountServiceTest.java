package com.byeolnight.service.user;

import com.byeolnight.dto.user.UserSignUpRequestDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.log.AuditSignupLogRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.auth.EmailAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditSignupLogRepository auditSignupLogRepository;
    @Mock
    private UserSecurityService userSecurityService;
    @Mock
    private EmailAuthService emailAuthService;

    @InjectMocks
    private UserAccountService userAccountService;

    @Test
    @DisplayName("닉네임 중복 검사 - 중복")
    void isNicknameDuplicated_Duplicate() {
        String nickname = "test";
        given(userRepository.existsByNicknameAndStatusNotIn(nickname, 
            List.of(User.UserStatus.WITHDRAWN, User.UserStatus.BANNED))).willReturn(true);

        boolean result = userAccountService.isNicknameDuplicated(nickname);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("닉네임 중복 검사 - 빈 닉네임")
    void isNicknameDuplicated_Empty() {
        boolean result = userAccountService.isNicknameDuplicated("");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("닉네임 중복 검사 - 중복 아님")
    void isNicknameDuplicated_NotDuplicate() {
        String nickname = "test";
        given(userRepository.existsByNicknameAndStatusNotIn(nickname, 
            List.of(User.UserStatus.WITHDRAWN, User.UserStatus.BANNED))).willReturn(false);

        boolean result = userAccountService.isNicknameDuplicated(nickname);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("비밀번호 검증 - 소셜 사용자")
    void checkPassword_SocialUser() {
        User user = User.builder().socialProvider("google").build();

        boolean result = userAccountService.checkPassword("password", user);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("사용자 저장")
    void save() {
        User user = User.builder().email("test@test.com").build();
        given(userRepository.save(any(User.class))).willReturn(user);

        User result = userAccountService.save(user);

        assertThat(result).isNotNull();
        verify(userRepository).save(user);
    }
}
