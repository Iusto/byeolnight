
package com.byeolnight.service.user;

import com.byeolnight.domain.entity.token.PasswordResetToken;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.*;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.dto.user.UpdateProfileRequestDto;
import com.byeolnight.dto.user.UserSignUpRequestDto;
import com.byeolnight.dto.user.UserSummaryDto;
import com.byeolnight.service.auth.GmailEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private NicknameChangeHistoryRepository nicknameChangeHistoryRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditSignupLogRepository auditSignupLogRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private GmailEmailService gmailEmailService;

    @InjectMocks private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = User.builder()
                .email("test@byeol.com")
                .password("encoded-password")
                .nickname("기존닉네임")
                .phone("010-0000-0000")
                .nicknameChanged(true)
                .nicknameUpdatedAt(LocalDateTime.now().minusMonths(7))
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(true)
                .loginFailCount(0)
                .level(1)
                .exp(0)
                .build();
    }

    @Test
    void registerSuccess() throws Exception {
        UserSignUpRequestDto dto = new UserSignUpRequestDto("new@byeol.com", "Password1!", "Password1!", "새닉네임", "010-1111-2222");

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userRepository.existsByNickname(dto.getNickname())).thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encoded-password");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(saved, 42L);
            return saved;
        });

        Long userId = userService.register(dto, "127.0.0.1");

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void updateProfileSuccess() {
        UpdateProfileRequestDto dto = new UpdateProfileRequestDto("새닉네임", "010-1234-5678", "encoded-password");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())).thenReturn(true);

        userService.updateProfile(1L, dto);

        assertThat(user.getPhone()).isEqualTo(dto.getPhone());
    }

    @Test
    void updateNicknameSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.updateNickname(1L, "새닉네임", "127.0.0.1");

        assertThat(user.getNickname()).isEqualTo("새닉네임");
        verify(nicknameChangeHistoryRepository).save(any());
    }

    @Test
    void withdrawSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        userService.withdraw(1L, "encoded-password", "탈퇴사유");

        assertThat(user.getStatus()).isEqualTo(User.UserStatus.WITHDRAWN);
    }

    @Test
    void requestPasswordResetSuccess() {
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);

        userService.requestPasswordReset(user.getEmail());

        verify(passwordResetTokenRepository).save(any());
        verify(gmailEmailService).send(eq(user.getEmail()), any(), contains("https://byeolnight.com/reset-password?token="));
    }

    @Test
    void resetPasswordSuccess() {
        PasswordResetToken token = PasswordResetToken.create(user.getEmail(), "token123", Duration.ofMinutes(30));
        when(passwordResetTokenRepository.findByToken("token123")).thenReturn(Optional.of(token));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpw")).thenReturn("encodedNewPw");

        userService.resetPassword("token123", "newpw");

        assertThat(user.getPassword()).isEqualTo("encodedNewPw");
    }

    @Test
    void increaseLoginFailCount() {
        userService.increaseLoginFailCount(user);
        assertThat(user.getLoginFailCount()).isEqualTo(1);
        verify(userRepository).save(user);
    }

    @Test
    void resetLoginFailCount() {
        userService.resetLoginFailCount(user);
        assertThat(user.getLoginFailCount()).isEqualTo(0);
        verify(userRepository).save(user);
    }

    @Test
    void checkPasswordTrue() {
        when(passwordEncoder.matches("raw", "encoded")).thenReturn(true);
        user = user.toBuilder().password("encoded").build();
        boolean result = userService.checkPassword("raw", user);
        assertThat(result).isTrue();
    }

    @Test
    void findByEmailReturnsUser() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        Optional<User> found = userService.findByEmail(user.getEmail());
        assertThat(found).contains(user);
    }

    @Test
    void getAllUserSummaries() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        List<UserSummaryDto> summaries = userService.getAllUserSummaries();
        assertThat(summaries).hasSize(1);
    }
}
