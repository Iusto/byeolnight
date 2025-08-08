package com.byeolnight.entity.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User 엔티티 테스트")
class UserTest {

    @Test
    @DisplayName("User 생성 테스트")
    void User_생성() {
        // Given & When
        User user = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .phone("010-1234-5678")
                .phoneHash("hashedPhone")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build();

        // Then
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getNickname()).isEqualTo("테스트유저");
        assertThat(user.getRole()).isEqualTo(User.Role.USER);
        assertThat(user.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("User 상태 변경 테스트")
    void User_상태_변경() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스트유저")
                .phone("010-1234-5678")
                .phoneHash("hashedPhone")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build();

        // When
        user.updateStatus(User.UserStatus.INACTIVE);

        // Then
        assertThat(user.getStatus()).isEqualTo(User.UserStatus.INACTIVE);
    }
}