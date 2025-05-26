package com.byeolnight.application.user;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.UserRepository;
import com.byeolnight.dto.user.UpdateProfileRequestDto;
import com.byeolnight.infrastructure.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded")
                .nickname("oldNick")
                .phone("01012345678")
                .role(User.Role.USER)
                .nicknameUpdatedAt(LocalDateTime.now().minusMonths(7))
                .build();
    }

    @Test
    @DisplayName("닉네임이 중복되면 예외가 발생한다")
    void testNicknameDuplicate() {
        UpdateProfileRequestDto dto = new UpdateProfileRequestDto("newNick", "01099998888");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("newNick")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용 중인 닉네임");
    }

    @Test
    @DisplayName("닉네임 변경은 6개월 이내에는 불가하다")
    void testNicknameChangeWithin6Months() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded")
                .nickname("oldNick")
                .phone("01012345678")
                .role(User.Role.USER)
                .nicknameUpdatedAt(LocalDateTime.now().minusMonths(3)) // ❗ 최근에 변경함
                .build();

        UpdateProfileRequestDto dto = new UpdateProfileRequestDto("newNick", "01099998888");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("newNick")).thenReturn(false);

        assertThatThrownBy(() -> userService.updateProfile(1L, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("닉네임은 6개월마다");
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 예외 발생")
    void testUserNotFound() {
        UpdateProfileRequestDto dto = new UpdateProfileRequestDto("any", "01099998888");
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(999L, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}
