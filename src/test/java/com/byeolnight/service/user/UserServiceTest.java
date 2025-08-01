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

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private AuditSignupLogRepository auditSignupLogRepository;
    
    @Mock
    private EncryptionUtil encryptionUtil;
    
    @Mock
    private UserSecurityService userSecurityService;
    
    @Mock
    private EmailAuthService emailAuthService;
    
    @Mock
    private PhoneAuthService phoneAuthService;
    
    @InjectMocks
    private UserService userService;
    
    private UserSignUpRequestDto signUpRequestDto;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        signUpRequestDto = UserSignUpRequestDto.builder()
                .email("test@example.com")
                .password("password123")
                .confirmPassword("password123")
                .nickname("테스트유저")
                .phone("010-1234-5678")
                .build();
                
        testUser = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("테스트유저")
                .phone("encryptedPhone")
                .phoneHash("hashedPhone")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build();
    }
    
    @Test
    @DisplayName("회원가입 성공 테스트")
    void 회원가입_성공() {
        // Given
        String ipAddress = "127.0.0.1";
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByPhoneHash(anyString())).willReturn(false);
        given(encryptionUtil.hashPhone(anyString())).willReturn("hashedPhone");
        given(encryptionUtil.encrypt(anyString())).willReturn("encryptedPhone");
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        given(userSecurityService.isValidPassword(anyString())).willReturn(true);
        given(emailAuthService.isAlreadyVerified(anyString())).willReturn(true);
        given(phoneAuthService.isAlreadyVerified(anyString())).willReturn(true);
        given(userRepository.save(any(User.class))).willReturn(testUser);
        
        // When
        Long userId = userService.register(signUpRequestDto, ipAddress);
        
        // Then
        assertThat(userId).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(auditSignupLogRepository).save(any());
    }
    
    @Test
    @DisplayName("이메일 중복 시 회원가입 실패")
    void 이메일_중복_회원가입_실패() {
        // Given
        String ipAddress = "127.0.0.1";
        given(userRepository.existsByEmail(anyString())).willReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> userService.register(signUpRequestDto, ipAddress))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("이미 사용 중인 이메일");
    }
    
    @Test
    @DisplayName("닉네임 중복 시 회원가입 실패")
    void 닉네임_중복_회원가입_실패() {
        // Given
        String ipAddress = "127.0.0.1";
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByNickname(anyString())).willReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> userService.register(signUpRequestDto, ipAddress))
                .isInstanceOf(DuplicateNicknameException.class)
                .hasMessageContaining("이미 사용 중인 닉네임");
    }
    
    @Test
    @DisplayName("사용자 조회 성공 테스트")
    void 사용자_조회_성공() {
        // Given
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(testUser));
        
        // When
        Optional<User> foundUser = userService.findByEmail("test@example.com");
        
        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.get().getNickname()).isEqualTo("테스트유저");
    }
    
    @Test
    @DisplayName("ID로 사용자 조회 성공 테스트")
    void ID로_사용자_조회_성공() {
        // Given
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        
        // When
        User foundUser = userService.findById(1L);
        
        // Then
        assertThat(foundUser.getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.getNickname()).isEqualTo("테스트유저");
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자 ID 조회 시 예외 발생")
    void 존재하지_않는_사용자_ID_조회_실패() {
        // Given
        given(userRepository.findById(999L)).willReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("닉네임 중복 검사 테스트")
    void 닉네임_중복_검사() {
        // Given
        given(userRepository.existsByNickname("중복닉네임")).willReturn(true);
        given(userRepository.existsByNickname("사용가능닉네임")).willReturn(false);
        
        // When & Then
        assertThat(userService.isNicknameDuplicated("중복닉네임")).isTrue();
        assertThat(userService.isNicknameDuplicated("사용가능닉네임")).isFalse();
        assertThat(userService.isNicknameDuplicated(null)).isTrue();
        assertThat(userService.isNicknameDuplicated("")).isTrue();
    }
}