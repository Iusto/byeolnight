package com.byeolnight.dto.user;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.entity.user.User.UserStatus;
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
    private String phone;
    private String role;
    private UserStatus status;

    public static UserSummaryDto from(User user) {
        return new UserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getPhone(),
                user.getRole().name(),
                user.getStatus()
        );
    }
}
