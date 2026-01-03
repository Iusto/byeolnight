package com.byeolnight.service.auth;

import com.byeolnight.dto.auth.EmailJob;
import com.byeolnight.infrastructure.cache.RedissonCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailAuthService {

    private final RedissonCacheService cacheService;

    @Value("${app.security.email-verification-secret}")
    private String verificationSecret;

    public void sendCode(String email) {
        // 이미 인증된 이메일인지 확인
        if (isAlreadyVerified(email)) {
            log.warn("이메일 인증 코드 전송 차단 - 이미 인증된 이메일");
            throw new IllegalStateException("이미 인증이 완료된 이메일입니다.");
        }
        
        // 전송 횟수 제한 확인
        if (isSendBlocked(email)) {
            log.warn("이메일 인증 코드 전송 차단 - 제한 횟수 초과: {}", email);
            throw new IllegalStateException("인증 코드 전송 횟수를 초과했습니다. 10분 후 다시 시도해주세요.");
        }
        
        String code = generateCode();
        String hashedCode = hashCode(email, code);
        cacheService.set("email:" + email, hashedCode, Duration.ofMinutes(5));

        log.info("이메일 인증 코드 생성 및 해시 완료");

        String subject = "[별 헤는 밤] 이메일 인증 코드";
        String htmlBody = createEmailTemplate(code);

        // 전송 횟수 증가
        incrementSendAttempts(email);

        // 비동기 메일 전송 작업을 Redis Stream에 추가
        enqueueEmailJob(email, subject, htmlBody);
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
        String savedHashedCode = cacheService.get(key);

        log.info("이메일 인증 검증 요청 처리 중");

        if (savedHashedCode != null) {
            String inputHashedCode = hashCode(email, code);
            if (savedHashedCode.equals(inputHashedCode)) {
                cacheService.delete(key);  // 검증 성공 시 삭제
                // 검증 성공 상태 저장 (10분간 유효)
                cacheService.set("verified:email:" + email, "true", Duration.ofMinutes(10));

                // 성공 시 시도 횟수 카운터 삭제
                cacheService.deleteCounter("verify_attempts:email:" + email);
                cacheService.deleteCounter("verify_attempts:ip:" + clientIp);

                log.info("이메일 인증 성공 - 검증 상태 저장 완료");
                return true;
            }
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
        String template = """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>별 헤는 밤 - 이메일 인증</title>
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #0f0f23 0%, #1a1a3e 100%); color: #ffffff;">
            <div style="max-width: 600px; margin: 0 auto; padding: 40px 20px;">
                <!-- 헤더 -->
                <div style="text-align: center; margin-bottom: 40px;">
                    <h1 style="color: #ffd700; font-size: 28px; margin: 0; text-shadow: 0 0 10px rgba(255, 215, 0, 0.5);">
                        🌌 별 헤는 밤
                    </h1>
                    <p style="color: #666666; font-size: 16px; margin: 10px 0 0 0; font-weight: 400;">우주의 신비를 함께 탐험하는 커뮤니티</p>
                </div>
                
                <!-- 메인 콘텐츠 -->
                <div style="background: rgba(255, 255, 255, 0.05); border-radius: 15px; padding: 30px; border: 1px solid rgba(255, 215, 0, 0.2); backdrop-filter: blur(10px);">
                    <h2 style="color: #ffd700; font-size: 22px; margin: 0 0 20px 0; text-align: center;">이메일 인증 코드</h2>
                    
                    <p style="color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 25px 0; text-align: center; font-weight: 500;">
                        안녕하세요! 별 헤는 밤에 오신 것을 환영합니다.<br>
                        아래 인증 코드를 입력하여 회원가입을 완료해주세요.
                    </p>
                    
                    <!-- 인증 코드 박스 -->
                    <div style="background: linear-gradient(45deg, #2d1b69 0%, #11998e 100%); border-radius: 10px; padding: 25px; text-align: center; margin: 25px 0; border: 2px solid #ffd700; box-shadow: 0 0 20px rgba(255, 215, 0, 0.3);">
                        <p style="color: #b8b8d4; font-size: 14px; margin: 0 0 10px 0;">인증 코드</p>
                        <div style="font-size: 32px; font-weight: bold; color: #ffd700; letter-spacing: 4px; font-family: 'Courier New', monospace; text-shadow: 0 0 10px rgba(255, 215, 0, 0.8);">
                            {{AUTH_CODE}}
                        </div>
                        <p style="color: #b8b8d4; font-size: 12px; margin: 10px 0 0 0;">유효시간: 5분</p>
                    </div>
                    
                    <div style="background: rgba(255, 215, 0, 0.1); border-radius: 8px; padding: 15px; margin: 20px 0; border-left: 4px solid #ffd700;">
                        <p style="color: #444444; font-size: 14px; margin: 0; line-height: 1.5; font-weight: 400;">
                            💡 <strong style="color: #d4af37;">안내사항</strong><br>
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
        """;
        
        return template.replace("{{AUTH_CODE}}", code);
    }
    
    /**
     * 이메일 전송 작업을 Redis 큐에 추가 (비동기)
     */
    private void enqueueEmailJob(String email, String subject, String htmlBody) {
        EmailJob emailJob = EmailJob.builder()
                .jobId(UUID.randomUUID().toString())
                .email(email)
                .subject(subject)
                .htmlBody(htmlBody)
                .attempt(0)
                .createdAt(Instant.now().toString())
                .build();

        cacheService.enqueue("queue:mail", emailJob);
        log.info("이메일 전송 작업 추가 완료: jobId={}, email={}", emailJob.getJobId(), email);
    }

    /**
     * 인증 시도 횟수 제한 확인 (이메일별 + IP별)
     */
    private boolean isVerificationBlocked(String email, String clientIp) {
        String emailKey = "verify_attempts:email:" + email;
        String ipKey = "verify_attempts:ip:" + clientIp;

        long emailAttempts = cacheService.getCounter(emailKey);
        long ipAttempts = cacheService.getCounter(ipKey);

        return emailAttempts >= 5 || ipAttempts >= 10;
    }

    /**
     * 인증 시도 횟수 증가 (원자적)
     */
    private void incrementVerificationAttempts(String email, String clientIp) {
        String emailKey = "verify_attempts:email:" + email;
        String ipKey = "verify_attempts:ip:" + clientIp;

        // 이메일별 시도 횟수 (5회 제한) - 원자적 증가
        cacheService.incrementAndGet(emailKey, Duration.ofMinutes(10));

        // IP별 시도 횟수 (10회 제한) - 원자적 증가
        cacheService.incrementAndGet(ipKey, Duration.ofMinutes(10));
    }

    /**
     * 인증 코드 전송 횟수 제한 확인 (이메일당 5회, 10분)
     */
    private boolean isSendBlocked(String email) {
        String key = "send_attempts:email:" + email;
        long attempts = cacheService.getCounter(key);
        return attempts >= 5;
    }

    /**
     * 인증 코드 전송 횟수 증가 (원자적)
     */
    private void incrementSendAttempts(String email) {
        String key = "send_attempts:email:" + email;
        cacheService.incrementAndGet(key, Duration.ofMinutes(10));
    }

    /**
     * 인증 코드 해싱 (SHA-256)
     * - email + code + secret을 조합하여 해시
     * - Redis 유출 시에도 인증 코드 보호
     */
    private String hashCode(String email, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = email + ":" + code + ":" + verificationSecret;
            byte[] hashBytes = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

            // Hex 문자열로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다", e);
        }
    }
}


