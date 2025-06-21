package com.byeolnight.service.auth;

import com.byeolnight.service.auth.CoolSmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
        // 이미 인증된 경우
        if (isAlreadyVerified(phone)) return false;

        String saved = redisTemplate.opsForValue().get("phone:" + phone);
        if (saved != null && saved.equals(code)) {
            redisTemplate.delete("phone:" + phone);
            redisTemplate.opsForValue().set("verified:phone:" + phone, "true", Duration.ofMinutes(10));
            return true;
        }
        return false;
    }

    public boolean isAlreadyVerified(String phone) {
        return Boolean.TRUE.toString().equals(redisTemplate.opsForValue().get("verified:phone:" + phone));
    }

    private String generateCode() {
        return String.valueOf((int)(Math.random() * 900000 + 100000));
    }
}
