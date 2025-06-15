package com.byeolnight.dto.admin;

import com.byeolnight.domain.entity.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserStatusChangeRequest {
    private User.UserStatus status;
    private String reason;
}
