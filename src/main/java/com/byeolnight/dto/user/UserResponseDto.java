package com.byeolnight.dto.user;

import com.byeolnight.domain.entity.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserResponseDto {

    private final Long id;
    private final String email;
    private final String nickname;
    private final String phone;
    private final String role;
    private final boolean nicknameChanged;
    private final LocalDateTime nicknameUpdatedAt;
    private final int points;
    private final Long equippedIconId;
    private final String equippedIconName;

    @Builder
    public UserResponseDto(Long id, String email, String nickname, String phone, String role, 
                          boolean nicknameChanged, LocalDateTime nicknameUpdatedAt, int points,
                          Long equippedIconId, String equippedIconName) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.phone = phone;
        this.role = role;
        this.nicknameChanged = nicknameChanged;
        this.nicknameUpdatedAt = nicknameUpdatedAt;
        this.points = points;
        this.equippedIconId = equippedIconId;
        this.equippedIconName = equippedIconName;
    }

    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .nicknameChanged(user.isNicknameChanged())
                .nicknameUpdatedAt(user.getNicknameUpdatedAt())
                .points(user.getPoints())
                .build();
    }
}
