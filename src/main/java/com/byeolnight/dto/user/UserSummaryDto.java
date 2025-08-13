package com.byeolnight.dto.user;

import com.byeolnight.entity.user.User;
import com.byeolnight.entity.user.User.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 관리자용 사용자 요약 정보 DTO
 */
@Getter
@AllArgsConstructor
public class UserSummaryDto {
    private Long id;
    private String email;
    private String nickname;
    private String role;
    private UserStatus status;
    private boolean accountLocked;
    private int points;
    private String socialProvider;

    public static UserSummaryDto from(User user) {
        return new UserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name(),
                user.getStatus(),
                user.isAccountLocked(),
                user.getPoints(),
                user.getSocialProvider() != null ? user.getSocialProvider().name() : null
        );
    }
}
