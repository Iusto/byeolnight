package com.byeolnight.service.user;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserQueryService userQueryService;
    @Mock
    private UserSecurityService userSecurityService;

    @InjectMocks
    private UserAdminService userAdminService;

    @Test
    @DisplayName("전체 사용자 요약 조회")
    void getAllUserSummaries() {
        User user1 = User.builder().email("user1@test.com").role(User.Role.USER).build();
        User admin = User.builder().email("admin@test.com").role(User.Role.ADMIN).build();
        given(userRepository.findAll()).willReturn(Arrays.asList(user1, admin));

        var result = userAdminService.getAllUserSummaries();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("계정 잠금")
    void lockUserAccount() {
        Long userId = 1L;
        User user = User.builder().email("test@test.com").build();
        given(userQueryService.findById(userId)).willReturn(user);

        userAdminService.lockUserAccount(userId);

        verify(userQueryService).findById(userId);
    }

    @Test
    @DisplayName("계정 잠금 해제")
    void unlockUserAccount() {
        Long userId = 1L;
        User user = User.builder().email("test@test.com").build();
        given(userQueryService.findById(userId)).willReturn(user);

        userAdminService.unlockUserAccount(userId);

        verify(userQueryService).findById(userId);
    }
}
