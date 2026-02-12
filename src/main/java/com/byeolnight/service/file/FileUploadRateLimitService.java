package com.byeolnight.service.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 파일 업로드 Rate Limiting 서비스
 * IpUtil과 연동하여 IP 기반 업로드 제한 적용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadRateLimitService {
    
    private final RedisTemplate<String, String> redisTemplate;

    private static final String CHECK_RATE_LIMIT_SCRIPT = """
        local key = KEYS[1]
        local blockedKey = KEYS[2]
        local limit = tonumber(ARGV[1])
        local windowSeconds = tonumber(ARGV[2])
        local blockSeconds = tonumber(ARGV[3])

        if redis.call('EXISTS', blockedKey) == 1 then
            return -1
        end

        local current = tonumber(redis.call('GET', key) or '0')

        if current >= limit then
            if blockSeconds > 0 then
                redis.call('SET', blockedKey, '1', 'EX', blockSeconds)
            end
            return -1
        end

        if current == 0 then
            redis.call('SET', key, '1', 'EX', windowSeconds)
        else
            redis.call('INCR', key)
        end

        return current + 1
        """;

    private static final String CHECK_FILE_SIZE_LIMIT_SCRIPT = """
        local key = KEYS[1]
        local fileSize = tonumber(ARGV[1])
        local maxSize = tonumber(ARGV[2])
        local windowSeconds = tonumber(ARGV[3])

        local current = tonumber(redis.call('GET', key) or '0')

        if current + fileSize > maxSize then
            return -1
        end

        if current == 0 then
            redis.call('SET', key, tostring(fileSize), 'EX', windowSeconds)
        else
            redis.call('INCRBY', key, fileSize)
        end

        return current + fileSize
        """;

    private static final String TRY_ACQUIRE_CONCURRENT_SLOT_SCRIPT = """
        local key = KEYS[1]
        local maxConcurrent = tonumber(ARGV[1])
        local ttlSeconds = tonumber(ARGV[2])

        local current = tonumber(redis.call('GET', key) or '0')

        if current >= maxConcurrent then
            return -1
        end

        redis.call('INCR', key)
        redis.call('EXPIRE', key, ttlSeconds)
        return current + 1
        """;

    public boolean isUploadAllowed(String clientIp, long fileSize) {
        return checkUploadLimit(clientIp) &&
               checkFileSizeLimit(clientIp, fileSize) &&
               tryAcquireConcurrentSlot(clientIp);
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
        try {
            String sizeKey = "file_size_1h:" + clientIp;

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(CHECK_FILE_SIZE_LIMIT_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(sizeKey),
                    String.valueOf(fileSize),
                    String.valueOf(50L * 1024 * 1024),
                    String.valueOf(3600));

            return result != null && result != -1L;
        } catch (Exception e) {
            return true;
        }
    }

    private boolean tryAcquireConcurrentSlot(String clientIp) {
        try {
            String key = "concurrent_upload:" + clientIp;

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(TRY_ACQUIRE_CONCURRENT_SLOT_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(key),
                    String.valueOf(3),
                    String.valueOf(600));

            return result != null && result != -1L;
        } catch (Exception e) {
            return true;
        }
    }
    
    public void finishUpload(String clientIp) {
        redisTemplate.opsForValue().decrement("concurrent_upload:" + clientIp);
    }
    
    private boolean checkRateLimit(String key, int limit, int windowMinutes, int blockMinutes) {
        try {
            String blockedKey = key + ":blocked";
            List<String> keys = Arrays.asList(key, blockedKey);

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(CHECK_RATE_LIMIT_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, keys,
                    String.valueOf(limit),
                    String.valueOf(windowMinutes * 60L),
                    String.valueOf(blockMinutes * 60L));

            return result != null && result != -1L;
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