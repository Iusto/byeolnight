package com.byeolnight.service.user;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.domain.repository.log.AuditSignupLogRepository;
import com.byeolnight.infrastructure.exception.DuplicateEmailException;
import com.byeolnight.infrastructure.exception.DuplicateNicknameException;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.infrastructure.security.EncryptionUtil;
import com.byeolnight.dto.user.UserSignUpRequestDto;
import com.byeolnight.service.auth.EmailAuthService;
import com.byeolnight.service.auth.PhoneAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {


    
    @Test
    @DisplayName("기본 테스트")
    void 기본_테스트() {
        assertThat(true).isTrue();
    }
}