package com.byeolnight.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EmailAuthServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private GmailEmailService gmailEmailService;

    @InjectMocks
    private EmailAuthService emailAuthService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("이메일 인증코드 전송 - Redis에 저장 및 이메일 발송")
    void testSendCode() {
        String email = "test@example.com";

        emailAuthService.sendCode(email);

        verify(valueOperations).set(startsWith("email:"), anyString(), eq(5L), eq(java.util.concurrent.TimeUnit.MINUTES));
        verify(gmailEmailService).send(eq(email), contains("이메일 인증"), contains("인증 코드"));
    }

    @Test
    @DisplayName("이메일 인증 성공")
    void testVerifyCode_success() {
        String email = "test@example.com";
        String code = "123456";

        when(valueOperations.get("email:" + email)).thenReturn("123456");

        boolean result = emailAuthService.verifyCode(email, code);

        assertThat(result).isTrue();
        verify(redisTemplate).delete("email:" + email);
    }

    @Test
    @DisplayName("이메일 인증 실패")
    void testVerifyCode_fail() {
        String email = "test@example.com";
        String code = "wrong";

        when(valueOperations.get("email:" + email)).thenReturn("123456");

        boolean result = emailAuthService.verifyCode(email, code);

        assertThat(result).isFalse();
        verify(redisTemplate, never()).delete(any(String.class));
    }
}
