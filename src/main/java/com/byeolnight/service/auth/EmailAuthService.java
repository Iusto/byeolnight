package com.byeolnight.service.auth;

import com.byeolnight.infrastructure.cache.RedissonCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailAuthService {

    private final RedissonCacheService cacheService;
    private final GmailEmailService gmailEmailService;

    public void sendCode(String email) {
        // 이미 인증된 이메일인지 확인
        if (isAlreadyVerified(email)) {
            // log.warn("[⚠️ 이메일 코드 전송 차단] 이미 인증된 이메일: {}", email);
            throw new IllegalStateException("이미 인증이 완료된 이메일입니다.");
        }
        
        String code = generateCode();
        cacheService.set("email:" + email, code, Duration.ofMinutes(5));

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
        String verified = cacheService.get("verified:email:" + email);
        log.info("[🔍 이메일 인증 상태 확인] email={}, Redis 값: {}", email, verified);
        return Boolean.TRUE.toString().equals(verified);
    }
    
    /**
     * 페이지 이탈 시 이메일 인증 관련 데이터 모두 정리
     */
    public void clearAllEmailData(String email) {
        cacheService.delete("email:" + email);           // 인증 코드 삭제
        cacheService.delete("verified:email:" + email);  // 인증 상태 삭제
        log.info("[🧹 이메일 인증 데이터 전체 삭제] email={}", email);
    }
    
    /**
     * 회원가입 완료 시에만 인증 상태 삭제 (인증 코드는 이미 삭제됨)
     */
    public void clearVerificationStatus(String email) {
        cacheService.delete("verified:email:" + email);  // 인증 상태만 삭제
        log.info("[🧹 이메일 인증 상태 삭제] email={}", email);
    }

    public boolean verifyCode(String email, String code) {
        String key = "email:" + email;
        String saved = cacheService.get(key);

        log.info("[🔐 이메일 인증 검증 요청] key={}, 입력값: {}, Redis 저장값: {}", key, code, saved);

        if (saved != null && saved.equals(code)) {
            cacheService.delete(key);  // 검증 성공 시 삭제
            // 검증 성공 상태 저장 (10분간 유효)
            cacheService.set("verified:email:" + email, "true", Duration.ofMinutes(10));
            log.info("[✅ 이메일 인증 성공] email={}, 검증 상태 저장 완료 (TTL: 10분)", email);
            return true;
        }
        log.warn("[❌ 이메일 인증 실패] email={}, 입력값: {}", email, code);
        return false;
    }

    private String generateCode() {
        return String.valueOf((int)(Math.random() * 900000 + 100000));
    }
}


