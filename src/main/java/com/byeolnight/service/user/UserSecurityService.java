package com.byeolnight.service.user;

import com.byeolnight.entity.log.AuditSignupLog;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.log.AuditSignupLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 사용자 보안 관련 도메인 서비스
 * - 비밀번호 정책 검증, 로그인 실패 처리, IP 차단 등
 */
@Service
@RequiredArgsConstructor
public class UserSecurityService {

    private final AuditSignupLogRepository auditSignupLogRepository;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.max-login-attempts:10}")
    private int maxLoginAttempts;

    @Value("${app.security.ip-block-duration:1h}")
    private Duration ipBlockDuration;

    /**
     * 비밀번호 정책 검증
     * - 8자 이상, 영문/숫자/특수문자 포함
     */
    public boolean isValidPassword(String password) {
        return password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$");
    }

    /**
     * 로그인 실패 처리 및 보안 정책 적용
     */
    public void handleLoginFailure(User user, String ipAddress, String userAgent) {
        // 로그인 실패 처리
        user.loginFail();
        
        int failCount = user.getLoginFailCount();
        String email = user.getEmail();

        // 보안 로그 기록 (5, 10, 15회 시점)
        if (failCount == 5 || failCount == maxLoginAttempts || failCount == 15) {
            String reason;
            if (failCount == 5) {
                reason = "로그인 5회 실패 - 사용자 경고 로그 기록";
            } else if (failCount == maxLoginAttempts) {
                reason = "로그인 " + maxLoginAttempts + "회 실패 - 계정 잠금 처리됨";
            } else if (failCount == 15) {
                reason = "로그인 15회 실패 - IP 차단 처리됨";
            } else {
                reason = "로그인 실패 경고";
            }

            auditSignupLogRepository.save(
                    AuditSignupLog.failure(email, ipAddress, reason)
            );
        }

        // IP 차단 처리 (15회 실패 시)
        if (failCount == 15) {
            redisTemplate.opsForValue().set(
                    "blocked:ip:" + ipAddress,
                    "true",
                    ipBlockDuration
            );
        }
    }

    /**
     * IP 차단 여부 확인
     */
    public boolean isIpBlocked(String ipAddress) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blocked:ip:" + ipAddress));
    }

    /**
     * 비밀번호 암호화
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 비밀번호 일치 여부 확인
     */
    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}