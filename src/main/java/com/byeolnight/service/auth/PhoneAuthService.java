package com.byeolnight.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PhoneAuthService {

    private final CoolSmsService coolSmsService;
    private final StringRedisTemplate redisTemplate;

    public void sendCode(String phone) {
        String code = generateCode();
        redisTemplate.opsForValue().set("phone:" + phone, code, 5, TimeUnit.MINUTES);

        String text = "[별 헤는 밤] 인증번호는 " + code + "입니다.";
        coolSmsService.send(phone, text);
    }

    public boolean verifyCode(String phone, String code) {
        String saved = redisTemplate.opsForValue().get("phone:" + phone);
        if (saved != null && saved.equals(code)) {
            redisTemplate.delete("phone:" + phone);
            return true;
        }
        return false;
    }

    private String generateCode() {
        return String.valueOf((int)(Math.random() * 900000 + 100000));
    }
}
