package com.byeolnight.dto.user;

import com.byeolnight.domain.entity.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String nickname;
    private String role;
    private String status;
    private int level;
    private int points;
    private long postCount;
    private long commentCount;

    public static UserProfileDto from(User user, long postCount, long commentCount) {
        return UserProfileDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .level(user.getLevel())
                .points(user.getPoints())
                .postCount(postCount)
                .commentCount(commentCount)
                .build();
    }
}