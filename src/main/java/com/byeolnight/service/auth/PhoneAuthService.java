package com.byeolnight.service.auth;

import com.byeolnight.service.auth.CoolSmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneAuthService {

    private final CoolSmsService coolSmsService;
    private final StringRedisTemplate redisTemplate;

    public void sendCode(String phone) {
        // 이미 인증된 번호인지 확인
        if (isAlreadyVerified(phone)) {
            // log.warn("[⚠️ 휴대폰 코드 전송 차단] 이미 인증된 번호: {}", phone);
            throw new IllegalStateException("이미 인증이 완료된 번호입니다.");
        }
        
        String code = generateCode();
        redisTemplate.opsForValue().set("phone:" + phone, code, 5, TimeUnit.MINUTES);
        // log.info("[📱 휴대폰 인증 코드 전송] phone={}, code={}", phone, code);

        String text = """
        [별 헤는 밤] 이메일 인증번호 안내
        
        안녕하세요. '별 헤는 밤'입니다.
        아래 인증번호를 입력해 회원가입을 완료해주세요.
        
        🔐 인증번호: %s
        
        감사합니다.
        """.formatted(code);
        coolSmsService.send(phone, text);
    }

    public boolean verifyCode(String phone, String code) {
        // 이미 인증된 경우
        if (isAlreadyVerified(phone)) {
            // log.info("[⚠️ 휴대폰 인증 시도] 이미 인증된 번호: {}", phone);
            return false;
        }

        String key = "phone:" + phone;
        String saved = redisTemplate.opsForValue().get(key);
        
        // log.info("[🔐 휴대폰 인증 검증 요청] key={}, 입력값: {}, Redis 저장값: {}", key, code, saved);
        
        if (saved != null && saved.equals(code)) {
            redisTemplate.delete(key);
            redisTemplate.opsForValue().set("verified:phone:" + phone, "true", Duration.ofMinutes(10));
            //log.info("[✅ 휴대폰 인증 성공] phone={}, 검증 상태 저장 완료", phone);
            return true;
        }
        // log.warn("[❌ 휴대폰 인증 실패] phone={}, 입력값: {}", phone, code);
        return false;
    }

    public boolean isAlreadyVerified(String phone) {
        String verified = redisTemplate.opsForValue().get("verified:phone:" + phone);
        // log.info("[🔍 휴대폰 인증 상태 확인] phone={}, Redis 값: {}", phone, verified);
        return Boolean.TRUE.toString().equals(verified);
    }
    
    public void clearVerification(String phone) {
        redisTemplate.delete("verified:phone:" + phone);
        // log.info("[🧹 휴대폰 인증 상태 삭제] phone={}", phone);
    }

    private String generateCode() {
        return String.valueOf((int)(Math.random() * 900000 + 100000));
    }
}
