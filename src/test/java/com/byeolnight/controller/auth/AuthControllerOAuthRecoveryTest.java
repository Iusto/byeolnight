package com.byeolnight.controller.auth;

import com.byeolnight.dto.auth.AccountRecoveryDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController OAuth 복구 기능 통합 테스트")
@Disabled("테스트 컨텍스트 로딩 문제로 임시 비활성화")
class AuthControllerOAuthRecoveryTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private User withdrawnSocialUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 탈퇴한 소셜 사용자 생성
        withdrawnSocialUser = User.builder()
                .email("testuser@gmail.com")
                .nickname("테스트유저")
                .password(null)
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .points(100)
                .build();
        withdrawnSocialUser.setSocialProvider("google");
        withdrawnSocialUser = userRepository.save(withdrawnSocialUser);
        
        // 탈퇴 처리 (30일 내)
        withdrawnSocialUser.withdraw("테스트 탈퇴");
        userRepository.save(withdrawnSocialUser);
    }

    @Test
    @DisplayName("소셜 계정 복구 승인 - 성공")
    void handleAccountRecovery_RecoverTrue_ShouldRecoverAccount() throws Exception {
        // given
        AccountRecoveryDto dto = new AccountRecoveryDto();
        dto.setEmail("testuser@gmail.com");
        dto.setProvider("google");
        dto.setRecover(true);

        // when & then
        mockMvc.perform(post("/api/auth/oauth/recover")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("계정이 복구되었습니다. 다시 로그인해주세요."));

        // 데이터베이스에서 복구 확인
        User recoveredUser = userRepository.findByEmail("testuser@gmail.com").orElse(null);
        assertNotNull(recoveredUser);
        assertEquals(User.UserStatus.ACTIVE, recoveredUser.getStatus());
        assertNull(recoveredUser.getWithdrawnAt());
        assertNull(recoveredUser.getWithdrawalReason());
        assertEquals("testuser", recoveredUser.getNickname()); // 이메일 기반 닉네임
    }

    @Test
    @DisplayName("소셜 계정 복구 거부 - 새 계정 생성 플래그 설정")
    void handleAccountRecovery_RecoverFalse_ShouldSetSkipFlag() throws Exception {
        // given
        AccountRecoveryDto dto = new AccountRecoveryDto();
        dto.setEmail("testuser@gmail.com");
        dto.setProvider("google");
        dto.setRecover(false);

        MockHttpSession session = new MockHttpSession();

        // when & then
        mockMvc.perform(post("/api/auth/oauth/recover")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("새 계정으로 진행합니다. 다시 로그인해주세요."));

        // 세션에 건너뛰기 플래그 설정 확인
        assertEquals("true", session.getAttribute("skip_recovery_check"));

        // 계정 상태는 변경되지 않음
        User unchangedUser = userRepository.findByEmail("testuser@gmail.com").orElse(null);
        assertNotNull(unchangedUser);
        assertEquals(User.UserStatus.WITHDRAWN, unchangedUser.getStatus());
    }

    @Test
    @DisplayName("복구 불가능한 계정 - 실패")
    void handleAccountRecovery_NonRecoverableAccount_ShouldFail() throws Exception {
        // given - 30일 경과한 계정 생성
        User oldWithdrawnUser = User.builder()
                .email("old@gmail.com")
                .nickname("오래된유저")
                .password(null)
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build();
        oldWithdrawnUser.setSocialProvider("google");
        oldWithdrawnUser = userRepository.save(oldWithdrawnUser);
        
        oldWithdrawnUser.withdraw("오래된 탈퇴");
        // 리플렉션으로 탈퇴 시점을 35일 전으로 설정
        setWithdrawnAt(oldWithdrawnUser, LocalDateTime.now().minusDays(35));
        userRepository.save(oldWithdrawnUser);

        AccountRecoveryDto dto = new AccountRecoveryDto();
        dto.setEmail("old@gmail.com");
        dto.setProvider("google");
        dto.setRecover(true);

        // when & then
        mockMvc.perform(post("/api/auth/oauth/recover")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("복구할 수 없는 계정입니다."));
    }

    @Test
    @DisplayName("닉네임 중복 시 숫자 접미사 추가")
    void handleAccountRecovery_DuplicateNickname_ShouldAddSuffix() throws Exception {
        // given - 동일한 닉네임을 가진 활성 사용자 생성
        User existingUser = User.builder()
                .email("existing@test.com")
                .nickname("testuser")
                .password("password")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build();
        userRepository.save(existingUser);

        AccountRecoveryDto dto = new AccountRecoveryDto();
        dto.setEmail("testuser@gmail.com");
        dto.setProvider("google");
        dto.setRecover(true);

        // when & then
        mockMvc.perform(post("/api/auth/oauth/recover")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 복구된 사용자의 닉네임에 숫자 접미사 확인
        User recoveredUser = userRepository.findByEmail("testuser@gmail.com").orElse(null);
        assertNotNull(recoveredUser);
        assertEquals("testuser1", recoveredUser.getNickname()); // 중복으로 인한 접미사 추가
    }

    @Test
    @DisplayName("탈퇴 신청 후 30일 이전 사용자 완전 복구 테스트")
    void handleAccountRecovery_Within30Days_CompleteRecoveryWithAllData() throws Exception {
        // given - 10일 전 탈퇴한 소셜 사용자 (포인트, 역할 등 모든 데이터 유지)
        User completeRecoveryUser = User.builder()
                .email("complete@gmail.com")
                .nickname("완전복구유저")
                .password(null)
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .points(1000) // 기존 포인트
                .equippedIconId(5L) // 장착 아이콘
                .equippedIconName("우주선")
                .build();
        completeRecoveryUser.setSocialProvider("google");
        completeRecoveryUser = userRepository.save(completeRecoveryUser);
        
        completeRecoveryUser.withdraw("사용자 요청");
        setWithdrawnAt(completeRecoveryUser, LocalDateTime.now().minusDays(10)); // 10일 전 탈퇴
        userRepository.save(completeRecoveryUser);

        AccountRecoveryDto dto = new AccountRecoveryDto();
        dto.setEmail("complete@gmail.com");
        dto.setProvider("google");
        dto.setRecover(true);

        // when & then
        mockMvc.perform(post("/api/auth/oauth/recover")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("계정이 복구되었습니다. 다시 로그인해주세요."));

        // 완전 복구 확인 - 모든 데이터 유지
        User recoveredUser = userRepository.findByEmail("complete@gmail.com").orElse(null);
        assertNotNull(recoveredUser);
        assertEquals(User.UserStatus.ACTIVE, recoveredUser.getStatus());
        assertNull(recoveredUser.getWithdrawnAt());
        assertNull(recoveredUser.getWithdrawalReason());
        assertEquals("complete", recoveredUser.getNickname()); // 이메일 기반 닉네임
        assertEquals(1000, recoveredUser.getPoints()); // 기존 포인트 유지
        assertEquals(5L, recoveredUser.getEquippedIconId()); // 장착 아이콘 유지
        assertEquals("우주선", recoveredUser.getEquippedIconName());
        assertEquals("google", recoveredUser.getSocialProvider()); // 소셜 제공자 유지
    }

    @Test
    @DisplayName("탈퇴 신청 후 30일 경과 사용자 새 계정 처리")
    void handleAccountRecovery_After30Days_ShouldFailAndRequireNewAccount() throws Exception {
        // given - 35일 전 탈퇴한 소셜 사용자
        User expiredUser = User.builder()
                .email("expired30days@gmail.com")
                .nickname("만료유저")
                .password(null)
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .points(500)
                .build();
        expiredUser.setSocialProvider("kakao");
        expiredUser = userRepository.save(expiredUser);
        
        expiredUser.withdraw("사용자 요청");
        setWithdrawnAt(expiredUser, LocalDateTime.now().minusDays(35)); // 35일 전 탈퇴
        userRepository.save(expiredUser);

        AccountRecoveryDto dto = new AccountRecoveryDto();
        dto.setEmail("expired30days@gmail.com");
        dto.setProvider("kakao");
        dto.setRecover(true);

        // when & then - 복구 실패
        mockMvc.perform(post("/api/auth/oauth/recover")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("복구할 수 없는 계정입니다."));

        // 계정 상태 변경 없음 확인
        User unchangedUser = userRepository.findByEmail("expired30days@gmail.com").orElse(null);
        assertNotNull(unchangedUser);
        assertEquals(User.UserStatus.WITHDRAWN, unchangedUser.getStatus()); // 여전히 탈퇴 상태
        assertNotNull(unchangedUser.getWithdrawnAt()); // 탈퇴 시점 유지
        
        // 새 계정 생성 플래그 설정 테스트
        AccountRecoveryDto newAccountDto = new AccountRecoveryDto();
        newAccountDto.setEmail("expired30days@gmail.com");
        newAccountDto.setProvider("kakao");
        newAccountDto.setRecover(false); // 새 계정 선택

        mockMvc.perform(post("/api/auth/oauth/recover")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAccountDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("새 계정으로 진행합니다. 다시 로그인해주세요."));
    }

    // 리플렉션을 사용하여 withdrawnAt 필드 설정
    private void setWithdrawnAt(User user, LocalDateTime withdrawnAt) {
        try {
            var field = User.class.getDeclaredField("withdrawnAt");
            field.setAccessible(true);
            field.set(user, withdrawnAt);
        } catch (Exception e) {
            throw new RuntimeException("withdrawnAt 필드 설정 실패", e);
        }
    }
}
