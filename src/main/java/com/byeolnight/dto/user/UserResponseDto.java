package com.byeolnight.dto.user;

import com.byeolnight.entity.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserResponseDto {

    private final Long id;
    private final String email;
    private final String nickname;
    private final String role;
    private final boolean nicknameChanged;
    private final LocalDateTime nicknameUpdatedAt;
    private final int points;
    private final int attendanceCount;
    private final Long equippedIconId;
    private final String equippedIconName;
    private final boolean isSocialUser;
    private final String socialProvider;

    @Builder
    public UserResponseDto(Long id, String email, String nickname, String role, 
                          boolean nicknameChanged, LocalDateTime nicknameUpdatedAt, int points, int attendanceCount,
                          Long equippedIconId, String equippedIconName, boolean isSocialUser, String socialProvider) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.nicknameChanged = nicknameChanged;
        this.nicknameUpdatedAt = nicknameUpdatedAt;
        this.points = points;
        this.attendanceCount = attendanceCount;
        this.equippedIconId = equippedIconId;
        this.equippedIconName = equippedIconName;
        this.isSocialUser = isSocialUser;
        this.socialProvider = socialProvider;
    }

    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .nicknameChanged(user.isNicknameChanged())
                .nicknameUpdatedAt(user.getNicknameUpdatedAt())
                .points(user.getPoints())
                .attendanceCount(0) // 기본값, 실제 값은 컨트롤러에서 설정
                .isSocialUser(user.isSocialUser())
                .socialProvider(user.getSocialProvider())
                .build();
    }
}
