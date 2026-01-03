package com.byeolnight.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 설정 클래스
 * - 분산락, 분산 컬렉션 등 고급 Redis 기능 제공
 * - 기존 RedisTemplate과 함께 사용 가능
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        String address = "redis://" + redisHost + ":" + redisPort;
        config.useSingleServer()
                .setAddress(address)
                .setConnectionMinimumIdleSize(5)   // 기본 연결 5개 유지
                .setConnectionPoolSize(15)         // 최대 15개 (동시 접속자 100명 기준)
                .setIdleConnectionTimeout(30000)   // 30초 (빠른 정리)
                .setConnectTimeout(5000);          // 5초 (빠른 실패)

        // 비밀번호가 설정된 경우에만 적용
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }

        return Redisson.create(config);
    }
}
