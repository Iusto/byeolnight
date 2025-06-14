package com.byeolnight.dto.user;

import com.byeolnight.domain.entity.user.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserResponseDto {

    private final Long id;
    private final String email;
    private final String nickname;
    private final String phone;
    private final String role;

    @Builder
    public UserResponseDto(Long id, String email, String nickname, String phone, String role) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.phone = phone;
        this.role = role;
    }

    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .build();
    }
}
