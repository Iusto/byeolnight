package com.byeolnight.service.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("계정 복구 티켓 서비스")
class AccountRecoveryTicketServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AccountRecoveryTicketService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new AccountRecoveryTicketService(redisTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("OAuth 인증 결과를 10분짜리 해시 키로 저장한다")
    void issueOAuthStoresHashedTicket() {
        String ticket = service.issueOAuth(1L, "google", "google-subject");

        assertThat(ticket).isNotBlank();
        verify(valueOperations).set(
                anyString(),
                anyString(),
                eq(Duration.ofMinutes(10))
        );
    }

    @Test
    @DisplayName("티켓은 조회와 동시에 삭제하고 인증 결과를 반환한다")
    void consumeDeletesTicketAtomically() throws Exception {
        var identity = new AccountRecoveryTicketService.RecoveryIdentity(
                3L,
                AccountRecoveryTicketService.AuthenticationMethod.OAUTH,
                "kakao",
                "12345"
        );
        when(valueOperations.getAndDelete(anyString()))
                .thenReturn(new ObjectMapper().writeValueAsString(identity));

        var result = service.consume("one-time-ticket");

        assertThat(result).isEqualTo(identity);
        verify(valueOperations).getAndDelete(anyString());
    }
}
