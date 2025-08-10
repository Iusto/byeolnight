package com.byeolnight.service.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 파일 업로드 Rate Limiting 서비스
 * IpUtil과 연동하여 IP 기반 업로드 제한 적용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadRateLimitService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public boolean isUploadAllowed(String clientIp, long fileSize) {
        return checkUploadLimit(clientIp) && 
               checkFileSizeLimit(clientIp, fileSize) && 
               checkConcurrentUpload(clientIp);
    }
    
    public boolean isPresignedUrlAllowed(String clientIp) {
        return checkRateLimit("presigned_url_1h:" + clientIp, 20, 60, 60) &&
               checkRateLimit("presigned_url_1d:" + clientIp, 100, 1440, 1440);
    }
    
    private boolean checkUploadLimit(String clientIp) {
        return checkRateLimit("file_upload_1h:" + clientIp, 10, 60, 60) &&
               checkRateLimit("file_upload_1d:" + clientIp, 50, 1440, 1440);
    }
    
    private boolean checkFileSizeLimit(String clientIp, long fileSize) {
        String sizeKey = "file_size_1h:" + clientIp;
        String currentSizeStr = redisTemplate.opsForValue().get(sizeKey);
        long currentSize = currentSizeStr != null ? Long.parseLong(currentSizeStr) : 0;
        
        if (currentSize + fileSize > 50 * 1024 * 1024) return false;
        
        if (currentSize == 0) {
            redisTemplate.opsForValue().set(sizeKey, String.valueOf(fileSize), Duration.ofMinutes(60));
        } else {
            redisTemplate.opsForValue().increment(sizeKey, fileSize);
        }
        return true;
    }
    
    private boolean checkConcurrentUpload(String clientIp) {
        String concurrentKey = "concurrent_upload:" + clientIp;
        String currentCount = redisTemplate.opsForValue().get(concurrentKey);
        return currentCount == null || Integer.parseInt(currentCount) < 3;
    }
    
    public void startUpload(String clientIp) {
        String key = "concurrent_upload:" + clientIp;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofMinutes(10));
    }
    
    public void finishUpload(String clientIp) {
        redisTemplate.opsForValue().decrement("concurrent_upload:" + clientIp);
    }
    
    private boolean checkRateLimit(String key, int limit, int windowMinutes, int blockMinutes) {
        try {
            if (redisTemplate.hasKey(key + ":blocked")) return false;
            
            String countStr = redisTemplate.opsForValue().get(key);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            
            if (currentCount >= limit) {
                if (blockMinutes > 0) {
                    redisTemplate.opsForValue().set(key + ":blocked", "1", Duration.ofMinutes(blockMinutes));
                }
                return false;
            }
            
            if (currentCount == 0) {
                redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(windowMinutes));
            } else {
                redisTemplate.opsForValue().increment(key);
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }
    
    public void clearIpLimit(String clientIp) {
        String[] prefixes = {"file_upload_1h:", "file_upload_1d:", "file_size_1h:", 
                           "presigned_url_1h:", "presigned_url_1d:", "concurrent_upload:"};
        
        for (String prefix : prefixes) {
            redisTemplate.delete(prefix + clientIp);
            redisTemplate.delete(prefix + clientIp + ":blocked");
        }
    }
}