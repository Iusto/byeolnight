package com.byeolnight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@org.springframework.data.jpa.repository.config.EnableJpaAuditing
public class ByeolnightApplication {
    public static void main(String[] args) {
        SpringApplication.run(ByeolnightApplication.class, args);
    }
}
