package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.CustomOAuth2UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StopWatch;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class CustomOAuth2UserServiceNicknameTest {

    @Autowired private CustomOAuth2UserService customOAuth2UserService;
    @MockBean private UserRepository userRepository;

    @Test
    @DisplayName("OAuth2 사용자 닉네임 생성 성능 테스트")
    void testOAuth2UserNicknameGenerationPerformance() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByNickname(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        OAuth2UserRequest userRequest = createMockOAuth2UserRequest();
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // When: OAuth2 사용자 로드 (닉네임 생성 포함)
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);
        
        stopWatch.stop();
        
        // Then: 성능 검증 (500ms 이내)
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(500);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("중복 닉네임 처리 성능 테스트")
    void testDuplicateNicknameHandlingPerformance() {
        // Given: 닉네임 중복 상황 시뮬레이션
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByNickname(anyString()))
            .thenReturn(true, true, false); // 2번 중복, 3번째에 성공
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        OAuth2UserRequest userRequest = createMockOAuth2UserRequest();
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // When: 중복 닉네임 처리
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);
        
        stopWatch.stop();
        
        // Then: 중복 처리 시간이 1초 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(1000);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("대량 OAuth2 사용자 처리 성능 테스트")
    void testBulkOAuth2UserProcessingPerformance() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByNickname(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // When: 10명의 사용자 처리
        for (int i = 0; i < 10; i++) {
            OAuth2UserRequest userRequest = createMockOAuth2UserRequest("test" + i + "@example.com");
            customOAuth2UserService.loadUser(userRequest);
        }
        
        stopWatch.stop();
        
        // Then: 10명 처리 시간이 3초 이내인지 확인
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(3000);
    }

    private OAuth2UserRequest createMockOAuth2UserRequest() {
        return createMockOAuth2UserRequest("test@example.com");
    }

    private OAuth2UserRequest createMockOAuth2UserRequest(String email) {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("google")
            .clientId("test-client-id")
            .clientSecret("test-client-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080/login/oauth2/code/google")
            .authorizationUri("https://accounts.google.com/o/oauth2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v2/userinfo")
            .userNameAttributeName("email")
            .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "test-token",
            Instant.now(),
            Instant.now().plusSeconds(3600)
        );

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", email);
        attributes.put("name", "Test User");
        
        DefaultOAuth2User oAuth2User = new DefaultOAuth2User(
            null, attributes, "email"
        );

        return new OAuth2UserRequest(clientRegistration, accessToken, attributes);
    }
}