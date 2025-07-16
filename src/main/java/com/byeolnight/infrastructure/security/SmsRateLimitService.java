package com.byeolnight.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsRateLimitService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * SMS 인증 제한 확인
     */
    public boolean isSmsAllowed(String phone, String clientIp) {
        // 전화번호별 제한 확인
        if (!checkPhoneLimit(phone)) {
            return false;
        }
        
        // IP별 제한 확인
        if (!checkIpLimit(clientIp)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 전화번호별 제한 확인
     */
    private boolean checkPhoneLimit(String phone) {
        // 1분에 1회
        if (!checkRateLimit("sms_phone_1m:" + phone, 1, 1, 0)) {
            log.warn("SMS 전화번호 1분 제한 - Phone: {}", phone);
            return false;
        }
        
        // 1시간에 5회, 30분 차단
        if (!checkRateLimit("sms_phone_1h:" + phone, 5, 60, 30)) {
            log.warn("SMS 전화번호 1시간 제한 - Phone: {}", phone);
            return false;
        }
        
        // 1일에 10회, 24시간 차단
        if (!checkRateLimit("sms_phone_1d:" + phone, 10, 1440, 1440)) {
            log.warn("SMS 전화번호 1일 제한 - Phone: {}", phone);
            return false;
        }
        
        return true;
    }
    
    /**
     * IP별 제한 확인
     */
    private boolean checkIpLimit(String clientIp) {
        // 1분에 3회
        if (!checkRateLimit("sms_ip_1m:" + clientIp, 3, 1, 0)) {
            log.warn("SMS IP 1분 제한 - IP: {}", clientIp);
            return false;
        }
        
        // 1시간에 20회, 1시간 차단
        if (!checkRateLimit("sms_ip_1h:" + clientIp, 20, 60, 60)) {
            log.warn("SMS IP 1시간 제한 - IP: {}", clientIp);
            return false;
        }
        
        // 1일에 50회, 24시간 차단
        if (!checkRateLimit("sms_ip_1d:" + clientIp, 50, 1440, 1440)) {
            log.warn("SMS IP 1일 제한 - IP: {}", clientIp);
            return false;
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
                    log.warn("SMS Rate limit 차단 적용 - Key: {}, BlockMinutes: {}", key, blockMinutes);
                }
                return false;
            }
            
            // 카운트 증가
            if (currentCount == 0) {
                redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(windowMinutes));
            } else {
                redisTemplate.opsForValue().increment(key);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("SMS Rate limit 확인 중 오류 발생 - Key: {}", key, e);
            return true; // 오류 시 허용
        }
    }
    
    /**
     * 전화번호별 제한 해제 (관리자용)
     */
    public void clearPhoneLimit(String phone) {
        String[] keys = {
            "sms_phone_1m:" + phone,
            "sms_phone_1h:" + phone,
            "sms_phone_1d:" + phone,
            "sms_phone_1m:" + phone + ":blocked",
            "sms_phone_1h:" + phone + ":blocked",
            "sms_phone_1d:" + phone + ":blocked"
        };
        
        for (String key : keys) {
            redisTemplate.delete(key);
        }
        
        log.info("전화번호 SMS 제한 해제 완료 - Phone: {}", phone);
    }
    
    /**
     * IP별 제한 해제 (관리자용)
     */
    public void clearIpLimit(String clientIp) {
        String[] keys = {
            "sms_ip_1m:" + clientIp,
            "sms_ip_1h:" + clientIp,
            "sms_ip_1d:" + clientIp,
            "sms_ip_1m:" + clientIp + ":blocked",
            "sms_ip_1h:" + clientIp + ":blocked",
            "sms_ip_1d:" + clientIp + ":blocked"
        };
        
        for (String key : keys) {
            redisTemplate.delete(key);
        }
        
        log.info("IP SMS 제한 해제 완료 - IP: {}", clientIp);
    }
}