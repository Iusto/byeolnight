package com.byeolnight.application.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PhoneAuthService {

    private final StringRedisTemplate redisTemplate;
    private final Random random = new Random();
    private final long EXPIRE_TIME = 60 * 5; // 5분

    public void sendCode(String phone) {
        String code = String.format("%06d", random.nextInt(999999));
        redisTemplate.opsForValue().set("phone:auth:" + phone, code, Duration.ofSeconds(EXPIRE_TIME));
        System.out.println("[Mock SMS] " + phone + " 인증코드: " + code);
    }

    public boolean verifyCode(String phone, String inputCode) {
        String savedCode = redisTemplate.opsForValue().get("phone:auth:" + phone);
        boolean valid = savedCode != null && savedCode.equals(inputCode);
        if (valid) {
            redisTemplate.opsForValue().set("phone:verified:" + phone, "true");
        }
        return valid;
    }
}