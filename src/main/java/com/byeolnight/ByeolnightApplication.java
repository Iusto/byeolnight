package com.byeolnight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@org.springframework.data.jpa.repository.config.EnableJpaAuditing
public class ByeolnightApplication {
    public static void main(String[] args) {
        // 애플리케이션 시작 시 시스템 기본 시간대를 한국 시간으로 설정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(ByeolnightApplication.class, args);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void logTimeInfo() {
        System.out.println("========== 시간 설정 정보 ===========");
        System.out.println("시스템 기본 시간대: " + ZoneId.systemDefault());
        System.out.println("현재 UTC 시간: " + Instant.now());
        System.out.println("현재 서버 시간: " + ZonedDateTime.now());
        System.out.println("현재 서울 시간: " + ZonedDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        System.out.println("현재 시스템 밀리초: " + System.currentTimeMillis());
        System.out.println("===================================");
    }
}
