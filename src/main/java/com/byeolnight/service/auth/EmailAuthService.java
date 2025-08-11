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
            log.warn("이메일 인증 코드 전송 차단 - 이미 인증된 이메일");
            throw new IllegalStateException("이미 인증이 완료된 이메일입니다.");
        }
        
        String code = generateCode();
        cacheService.set("email:" + email, code, Duration.ofMinutes(5));

        log.info("이메일 인증 코드 생성 완료");

        String subject = "[별 헤는 밤] 이메일 인증 코드";
        String htmlBody = createEmailTemplate(code);
        
        // HTML 이메일로 재시도 로직과 함께 전송
        sendHtmlEmailWithRetry(email, subject, htmlBody, 3);
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

    public boolean verifyCode(String email, String code, String clientIp) {
        // 시도 횟수 제한 확인
        if (isVerificationBlocked(email, clientIp)) {
            log.warn("이메일 인증 시도 차단 - 제한 횟수 초과");
            throw new IllegalStateException("인증 시도 횟수를 초과했습니다. 10분 후 다시 시도해주세요.");
        }
        
        String key = "email:" + email;
        String saved = cacheService.get(key);

        log.info("이메일 인증 검증 요청 처리 중");

        if (saved != null && saved.equals(code)) {
            cacheService.delete(key);  // 검증 성공 시 삭제
            // 검증 성공 상태 저장 (10분간 유효)
            cacheService.set("verified:email:" + email, "true", Duration.ofMinutes(10));
            // 성공 시 시도 횟수 초기화
            clearVerificationAttempts(email, clientIp);
            log.info("이메일 인증 성공 - 검증 상태 저장 완료");
            return true;
        }
        
        // 실패 시 시도 횟수 증가
        incrementVerificationAttempts(email, clientIp);
        log.warn("이메일 인증 실패");
        return false;
    }

    /**
     * 8자리 영숫자 인증 코드 생성 (대문자 + 숫자, 혼동 문자 제외)
     */
    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // I, O, 0, 1 제외
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return code.toString();
    }
    
    /**
     * HTML 이메일 템플릿 생성
     */
    private String createEmailTemplate(String code) {
        return """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>별 헤는 밤 - 이메일 인증</title>
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #0f0f23 0%%, #1a1a3e 100%); color: #ffffff;">
            <div style="max-width: 600px; margin: 0 auto; padding: 40px 20px;">
                <!-- 헤더 -->
                <div style="text-align: center; margin-bottom: 40px;">
                    <h1 style="color: #ffd700; font-size: 28px; margin: 0; text-shadow: 0 0 10px rgba(255, 215, 0, 0.5);">
                        🌌 별 헤는 밤
                    </h1>
                    <p style="color: #b8b8d4; font-size: 16px; margin: 10px 0 0 0;">우주의 신비를 함께 탐험하는 커뮤니티</p>
                </div>
                
                <!-- 메인 콘텐츠 -->
                <div style="background: rgba(255, 255, 255, 0.05); border-radius: 15px; padding: 30px; border: 1px solid rgba(255, 215, 0, 0.2); backdrop-filter: blur(10px);">
                    <h2 style="color: #ffd700; font-size: 22px; margin: 0 0 20px 0; text-align: center;">이메일 인증 코드</h2>
                    
                    <p style="color: #e0e0e0; font-size: 16px; line-height: 1.6; margin: 0 0 25px 0; text-align: center;">
                        안녕하세요! 별 헤는 밤에 오신 것을 환영합니다.<br>
                        아래 인증 코드를 입력하여 회원가입을 완료해주세요.
                    </p>
                    
                    <!-- 인증 코드 박스 -->
                    <div style="background: linear-gradient(45deg, #2d1b69 0%%, #11998e 100%); border-radius: 10px; padding: 25px; text-align: center; margin: 25px 0; border: 2px solid #ffd700; box-shadow: 0 0 20px rgba(255, 215, 0, 0.3);">
                        <p style="color: #b8b8d4; font-size: 14px; margin: 0 0 10px 0;">인증 코드</p>
                        <div style="font-size: 32px; font-weight: bold; color: #ffd700; letter-spacing: 4px; font-family: 'Courier New', monospace; text-shadow: 0 0 10px rgba(255, 215, 0, 0.8);">
                            %s
                        </div>
                        <p style="color: #b8b8d4; font-size: 12px; margin: 10px 0 0 0;">유효시간: 5분</p>
                    </div>
                    
                    <div style="background: rgba(255, 215, 0, 0.1); border-radius: 8px; padding: 15px; margin: 20px 0; border-left: 4px solid #ffd700;">
                        <p style="color: #e0e0e0; font-size: 14px; margin: 0; line-height: 1.5;">
                            💡 <strong>안내사항</strong><br>
                            • 인증 코드는 5분간 유효합니다<br>
                            • 대소문자를 구분하여 정확히 입력해주세요<br>
                            • 본인이 요청하지 않았다면 이 이메일을 무시해주세요
                        </p>
                    </div>
                </div>
                
                <!-- 푸터 -->
                <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid rgba(255, 215, 0, 0.2);">
                    <p style="color: #8a8aa0; font-size: 14px; margin: 0;">
                        별 헤는 밤과 함께 우주의 신비를 탐험해보세요 ✨
                    </p>
                    <p style="color: #6a6a80; font-size: 12px; margin: 10px 0 0 0;">
                        이 이메일은 자동으로 발송되었습니다. 회신하지 마세요.
                    </p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(code);
    }
    
    /**
     * HTML 이메일 재시도 로직과 함께 전송
     */
    private void sendHtmlEmailWithRetry(String email, String subject, String htmlBody, int maxRetries) {
        int attempt = 1;
        while (attempt <= maxRetries) {
            try {
                gmailEmailService.sendHtml(email, subject, htmlBody);
                log.info("HTML 이메일 전송 성공 (시도: {}/{})", attempt, maxRetries);
                return;
            } catch (Exception e) {
                log.warn("HTML 이메일 전송 실패 (시도: {}/{}): {}", attempt, maxRetries, e.getMessage());
                
                if (attempt == maxRetries) {
                    log.error("HTML 이메일 전송 최종 실패 - 모든 재시도 소진");
                    throw new RuntimeException("이메일 전송에 실패했습니다. 잠시 후 다시 시도해주세요.");
                }
                
                // 지수 백오프: 1초, 2초, 4초
                try {
                    Thread.sleep(1000L * (1L << (attempt - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("이메일 전송 중 인터럽트 발생", ie);
                }
                
                attempt++;
            }
        }
    }
    
    /**
     * 인증 시도 횟수 제한 확인 (이메일별 + IP별)
     */
    private boolean isVerificationBlocked(String email, String clientIp) {
        String emailKey = "verify_attempts:email:" + email;
        String ipKey = "verify_attempts:ip:" + clientIp;
        
        Integer emailAttempts = cacheService.get(emailKey);
        Integer ipAttempts = cacheService.get(ipKey);
        
        return (emailAttempts != null && emailAttempts >= 5) || 
               (ipAttempts != null && ipAttempts >= 10);
    }
    
    /**
     * 인증 시도 횟수 증가
     */
    private void incrementVerificationAttempts(String email, String clientIp) {
        String emailKey = "verify_attempts:email:" + email;
        String ipKey = "verify_attempts:ip:" + clientIp;
        
        // 이메일별 시도 횟수 (5회 제한)
        Integer emailAttempts = cacheService.get(emailKey);
        cacheService.set(emailKey, (emailAttempts == null ? 1 : emailAttempts + 1), Duration.ofMinutes(10));
        
        // IP별 시도 횟수 (10회 제한)
        Integer ipAttempts = cacheService.get(ipKey);
        cacheService.set(ipKey, (ipAttempts == null ? 1 : ipAttempts + 1), Duration.ofMinutes(10));
    }
    
    /**
     * 인증 성공 시 시도 횟수 초기화
     */
    private void clearVerificationAttempts(String email, String clientIp) {
        cacheService.delete("verify_attempts:email:" + email);
        cacheService.delete("verify_attempts:ip:" + clientIp);
    }
}


