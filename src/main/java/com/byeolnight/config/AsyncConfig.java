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
        executor.setCorePoolSize(5);  // 기본 스레드 수
        executor.setMaxPoolSize(10);  // 최대 스레드 수
        executor.setQueueCapacity(25); // 대기열 크기
        executor.setThreadNamePrefix("ImageValidator-");
        executor.initialize();
        return executor;
    }
}