package com.byeolnight.service.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AccountRecoveryTicketService {

    private static final String KEY_PREFIX = "account_recovery:";
    private static final Duration TICKET_TTL = Duration.ofMinutes(10);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public String issueOAuth(Long userId, String provider, String providerUserId) {
        if (userId == null || provider == null || provider.isBlank()
                || providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("OAuth 복구 티켓을 발급할 수 없습니다.");
        }
        return issue(new RecoveryIdentity(userId, AuthenticationMethod.OAUTH, provider, providerUserId));
    }

    public String issuePassword(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("계정 복구 티켓을 발급할 수 없습니다.");
        }
        return issue(new RecoveryIdentity(userId, AuthenticationMethod.PASSWORD, null, null));
    }

    public RecoveryIdentity consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new IllegalArgumentException("복구 티켓이 필요합니다.");
        }

        String serialized = redisTemplate.opsForValue().getAndDelete(key(ticket));
        if (serialized == null) {
            throw new IllegalArgumentException("복구 티켓이 만료되었거나 이미 사용되었습니다.");
        }

        try {
            return objectMapper.readValue(serialized, RecoveryIdentity.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("복구 티켓이 올바르지 않습니다.", e);
        }
    }

    private String issue(RecoveryIdentity identity) {
        String ticket = createRandomTicket();
        try {
            redisTemplate.opsForValue().set(key(ticket), objectMapper.writeValueAsString(identity), TICKET_TTL);
            return ticket;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("계정 복구 티켓을 저장할 수 없습니다.", e);
        }
    }

    private String createRandomTicket() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String key(String ticket) {
        return KEY_PREFIX + hash(ticket);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    public enum AuthenticationMethod {
        OAUTH, PASSWORD
    }

    public record RecoveryIdentity(
            Long userId,
            AuthenticationMethod authenticationMethod,
            String provider,
            String providerUserId
    ) {
    }
}
