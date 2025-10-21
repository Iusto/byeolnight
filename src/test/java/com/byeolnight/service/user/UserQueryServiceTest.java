package com.byeolnight.service.user;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserQueryService userQueryService;

    @Test
    @DisplayName("이메일로 사용자 조회 성공")
    void findByEmail_Success() {
        String email = "test@test.com";
        User user = User.builder().email(email).build();
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        Optional<User> result = userQueryService.findByEmail(email);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("ID로 사용자 조회 성공")
    void findById_Success() {
        Long userId = 1L;
        User user = User.builder().email("test@test.com").build();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        User result = userQueryService.findById(userId);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("ID로 사용자 조회 실패")
    void findById_NotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.findById(999L))
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("닉네임으로 사용자 조회")
    void findByNickname() {
        String nickname = "testUser";
        User user = User.builder().nickname(nickname).build();
        given(userRepository.findByNickname(nickname)).willReturn(Optional.of(user));

        Optional<User> result = userQueryService.findByNickname(nickname);

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("닉네임 존재 확인")
    void existsByNickname() {
        given(userRepository.existsByNickname("test")).willReturn(true);

        boolean result = userQueryService.existsByNickname("test");

        assertThat(result).isTrue();
    }
}
