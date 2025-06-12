package com.byeolnight.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class EmailAuthService {

    private final StringRedisTemplate redisTemplate;
    private final GmailEmailService gmailEmailService;

    public void sendCode(String email) {
        String code = generateCode();
        redisTemplate.opsForValue().set("email:" + email, code, 5, TimeUnit.MINUTES);

        String subject = "[별 헤는 밤] 이메일 인증 코드";
        String body = "인증 코드는: " + code;
        gmailEmailService.send(email, subject, body);
    }

    public boolean verifyCode(String email, String code) {
        String saved = redisTemplate.opsForValue().get("email:" + email);
        if (saved != null && saved.equals(code)) {
            redisTemplate.delete("email:" + email);
            return true;
        }
        return false;
    }

    private String generateCode() {
        return String.valueOf((int)(Math.random() * 900000 + 100000));
    }
}


