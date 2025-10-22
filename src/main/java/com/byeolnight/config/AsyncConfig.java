package com.byeolnight.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
// 스레드 풀을 설정
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "imageValidationExecutor")
    public Executor imageValidationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);    // 기본 3개 (평상시 충분)
        executor.setMaxPoolSize(8);     // 최대 8개 (피크 타임 대응)
        executor.setQueueCapacity(20);  // 대기열 20개 (버퍼)
        executor.setKeepAliveSeconds(60); // 유휴 쓰레드 1분 후 정리
        executor.setThreadNamePrefix("ImageValidator-");
        executor.initialize();
        return executor;
    }
}