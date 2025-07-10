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
        // 이미 인증된 이메일인지 확인
        if (isAlreadyVerified(email)) {
            // log.warn("[⚠️ 이메일 코드 전송 차단] 이미 인증된 이메일: {}", email);
            throw new IllegalStateException("이미 인증이 완료된 이메일입니다.");
        }
        
        String code = generateCode();
        redisTemplate.opsForValue().set("email:" + email, code, 5, TimeUnit.MINUTES);

        // log.info("[📨 이메일 인증 코드 전송] email={}, code={}", email, code);

        String subject = "[별 헤는 밤] 이메일 인증 코드";
        String body = """
        [별 헤는 밤] 이메일 인증코드 안내
        
        안녕하세요. '별 헤는 밤'입니다.
        아래 인증코드를 입력해 회원가입을 완료해주세요.
        
        🔐 인증번호: %s
        
        감사합니다.
        """.formatted(code);
        gmailEmailService.send(email, subject, body);
    }

    public boolean isAlreadyVerified(String email) {
        String verified = redisTemplate.opsForValue().get("verified:email:" + email);
        // log.info("[🔍 이메일 인증 상태 확인] email={}, Redis 값: {}", email, verified);
        return Boolean.TRUE.toString().equals(verified);
    }
    
    public void clearVerification(String email) {
        redisTemplate.delete("verified:email:" + email);
        // log.info("[🧹 이메일 인증 상태 삭제] email={}", email);
    }

    public boolean verifyCode(String email, String code) {
        String key = "email:" + email;
        String saved = redisTemplate.opsForValue().get(key);

        // log.info("[🔐 이메일 인증 검증 요청] key={}, 입력값: {}, Redis 저장값: {}", key, code, saved);

        if (saved != null && saved.equals(code)) {
            redisTemplate.delete(key);  // 검증 성공 시 삭제
            // 검증 성공 상태 저장 (10분간 유효)
            redisTemplate.opsForValue().set("verified:email:" + email, "true", Duration.ofMinutes(10));
            // log.info("[✅ 이메일 인증 성공] email={}, 검증 상태 저장 완료", email);
            return true;
        }
        log.warn("[❌ 이메일 인증 실패] email={}, 입력값: {}", email, code);
        return false;
    }

    private String generateCode() {
        return String.valueOf((int)(Math.random() * 900000 + 100000));
    }
}


