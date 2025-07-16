package com.byeolnight.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // API별 제한 설정
    public enum ApiType {
        GENERAL(100, 10), // 일반 API: 분당 100회, 10분 차단
        LOGIN(5, 30),     // 로그인: 분당 5회, 30분 차단
        SIGNUP(3, 60),    // 회원가입: 분당 3회, 1시간 차단
        FILE_UPLOAD(10, 5); // 파일 업로드: 분당 10회, 5분 차단
        
        private final int limit;
        private final int blockMinutes;
        
        ApiType(int limit, int blockMinutes) {
            this.limit = limit;
            this.blockMinutes = blockMinutes;
        }
    }
    
    // 이메일 인증 제한 설정
    public enum EmailLimitType {
        PER_MINUTE(1, 1, 0),    // 1분에 1회
        PER_HOUR(5, 60, 30),    // 1시간에 5회, 30분 차단
        PER_DAY(10, 1440, 1440); // 1일에 10회, 24시간 차단
        
        private final int limit;
        private final int windowMinutes;
        private final int blockMinutes;
        
        EmailLimitType(int limit, int windowMinutes, int blockMinutes) {
            this.limit = limit;
            this.windowMinutes = windowMinutes;
            this.blockMinutes = blockMinutes;
        }
    }
    
    // IP별 제한 설정
    public enum IpLimitType {
        PER_MINUTE(3, 1, 0),     // 1분에 3회
        PER_HOUR(20, 60, 60),    // 1시간에 20회, 1시간 차단
        PER_DAY(50, 1440, 1440); // 1일에 50회, 24시간 차단
        
        private final int limit;
        private final int windowMinutes;
        private final int blockMinutes;
        
        IpLimitType(int limit, int windowMinutes, int blockMinutes) {
            this.limit = limit;
            this.windowMinutes = windowMinutes;
            this.blockMinutes = blockMinutes;
        }
    }
    
    /**
     * API 요청 제한 확인
     */
    public boolean isApiAllowed(String clientIp, ApiType apiType) {
        String key = "api_limit:" + apiType.name() + ":" + clientIp;
        return checkRateLimit(key, apiType.limit, 1, apiType.blockMinutes);
    }
    
    /**
     * 이메일 인증 제한 확인
     */
    public boolean isEmailAuthAllowed(String email, String clientIp) {
        // 이메일별 제한 확인
        for (EmailLimitType limitType : EmailLimitType.values()) {
            String emailKey = "email_limit:" + limitType.name() + ":" + email;
            if (!checkRateLimit(emailKey, limitType.limit, limitType.windowMinutes, limitType.blockMinutes)) {
                log.warn("이메일 인증 제한 - Email: {}, Type: {}", email, limitType);
                return false;
            }
        }
        
        // IP별 제한 확인
        for (IpLimitType limitType : IpLimitType.values()) {
            String ipKey = "ip_email_limit:" + limitType.name() + ":" + clientIp;
            if (!checkRateLimit(ipKey, limitType.limit, limitType.windowMinutes, limitType.blockMinutes)) {
                log.warn("이메일 인증 IP 제한 - IP: {}, Type: {}", clientIp, limitType);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * SMS 인증 제한 확인 (이메일과 동일한 정책)
     */
    public boolean isSmsAuthAllowed(String phone, String clientIp) {
        // 전화번호별 제한 확인
        for (EmailLimitType limitType : EmailLimitType.values()) {
            String phoneKey = "sms_limit:" + limitType.name() + ":" + phone;
            if (!checkRateLimit(phoneKey, limitType.limit, limitType.windowMinutes, limitType.blockMinutes)) {
                log.warn("SMS 인증 제한 - Phone: {}, Type: {}", phone, limitType);
                return false;
            }
        }
        
        // IP별 제한 확인
        for (IpLimitType limitType : IpLimitType.values()) {
            String ipKey = "ip_sms_limit:" + limitType.name() + ":" + clientIp;
            if (!checkRateLimit(ipKey, limitType.limit, limitType.windowMinutes, limitType.blockMinutes)) {
                log.warn("SMS 인증 IP 제한 - IP: {}, Type: {}", clientIp, limitType);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Rate Limit 확인 및 카운트 증가
     */
    private boolean checkRateLimit(String key, int limit, int windowMinutes, int blockMinutes) {
        try {
            // 차단 상태 확인
            String blockKey = key + ":blocked";
            if (redisTemplate.hasKey(blockKey)) {
                return false;
            }
            
            // 현재 카운트 조회
            String countStr = redisTemplate.opsForValue().get(key);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            
            if (currentCount >= limit) {
                // 제한 초과 시 차단 처리
                if (blockMinutes > 0) {
                    redisTemplate.opsForValue().set(blockKey, "1", Duration.ofMinutes(blockMinutes));
                    log.warn("Rate limit 차단 적용 - Key: {}, BlockMinutes: {}", key, blockMinutes);
                }
                return false;
            }
            
            // 카운트 증가
            if (currentCount == 0) {
                // 첫 요청 시 TTL 설정
                redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(windowMinutes));
            } else {
                // 기존 TTL 유지하며 카운트 증가
                redisTemplate.opsForValue().increment(key);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Rate limit 확인 중 오류 발생 - Key: {}", key, e);
            return true; // 오류 시 허용
        }
    }
    
    /**
     * 특정 키의 제한 해제 (관리자용)
     */
    public void clearRateLimit(String key) {
        redisTemplate.delete(key);
        redisTemplate.delete(key + ":blocked");
        log.info("Rate limit 해제 - Key: {}", key);
    }
    
    /**
     * IP 차단 해제 (관리자용)
     */
    public void clearIpLimit(String clientIp) {
        String pattern = "*:" + clientIp;
        redisTemplate.delete(redisTemplate.keys(pattern));
        log.info("IP 제한 해제 - IP: {}", clientIp);
    }
}