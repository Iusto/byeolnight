package com.byeolnight.infrastructure.config;

/**
 * RestTemplate 설정 및 빈 등록
 * 
 * 역할:
 * - HTTP 클라이언트 RestTemplate 빈 생성
 * - UTF-8 인코딩 설정으로 한글 처리 지원
 * - 외부 API 호출 시 사용되는 공통 HTTP 클라이언트
 * - @Primary 어노테이션으로 기본 RestTemplate 지정
 */

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class RestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // UTF-8 인코딩 설정
        restTemplate.getMessageConverters()
                .stream()
                .filter(converter -> converter instanceof StringHttpMessageConverter)
                .forEach(converter -> ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8));
        
        log.info("RestTemplate 빈 생성 완료 (UTF-8 인코딩 설정)");
        return restTemplate;
    }
}