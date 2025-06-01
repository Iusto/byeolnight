package com.byeolnight.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final StringRedisTemplate redisTemplate;
    private final Random random = new Random();
    private final long EXPIRE_TIME = 60 * 5; // 5분

    public void sendCode(String email) {
        String code = String.format("%06d", random.nextInt(999999));
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(getKey(email), code, Duration.ofSeconds(EXPIRE_TIME));
        System.out.println("[Mock Email] " + email + " 인증코드: " + code);
    }

    public boolean verifyCode(String email, String inputCode) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String savedCode = ops.get(getKey(email));
        return savedCode != null && savedCode.equals(inputCode);
    }

    private String getKey(String email) {
        return "email:auth:" + email;
    }
}