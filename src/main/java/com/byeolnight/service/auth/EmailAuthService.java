package com.byeolnight.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailAuthService {

    private final StringRedisTemplate redisTemplate;
    private final GmailEmailService gmailEmailService;

    public void sendCode(String email) {
        String code = generateCode();
        redisTemplate.opsForValue().set("email:" + email, code, 5, TimeUnit.MINUTES);

        log.info("[📨 이메일 인증 코드 전송] email={}, code={}", email, code);

        String subject = "[별 헤는 밤] 이메일 인증 코드";
        String body = "인증 코드는: " + code;
        gmailEmailService.send(email, subject, body);
    }

    public boolean isAlreadyVerified(String email) {
        return Boolean.TRUE.toString().equals(redisTemplate.opsForValue().get("verified:email:" + email));
    }

    public boolean verifyCode(String email, String code) {
        String key = "email:" + email;
        String saved = redisTemplate.opsForValue().get(key);

        log.info("[🔐 이메일 인증 검증 요청] key={}, 입력값: {}, Redis 저장값: {}", key, code, saved);

        if (saved != null && saved.equals(code)) {
            redisTemplate.delete(key);  // 검증 성공 시 삭제
            return true;
        }
        return false;
    }

    private String generateCode() {
        return String.valueOf((int)(Math.random() * 900000 + 100000));
    }
}


