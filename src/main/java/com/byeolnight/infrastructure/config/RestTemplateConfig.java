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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Configuration
public class RestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        // 타임아웃 설정
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10초
        factory.setReadTimeout(15000);    // 15초
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // UTF-8 인코딩 설정
        restTemplate.getMessageConverters()
                .stream()
                .filter(converter -> converter instanceof StringHttpMessageConverter)
                .forEach(converter -> ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8));
        
        log.info("RestTemplate 빈 생성 완료 (UTF-8 인코딩, 타임아웃 설정)");
        return restTemplate;
    }

    /**
     * 날씨 API 전용 RestTemplate
     *
     * 사용자 요청 경로에서 호출되므로 기본 빈(connect 10s / read 15s)보다 짧게 설정한다.
     * 외부 API가 지연되면 Tomcat 스레드가 그만큼 묶여 날씨와 무관한 요청까지
     * 대기열에 쌓이므로, 응답 상한을 짧게 두고 실패 시 폴백 응답으로 처리한다.
     */
    @Bean
    public RestTemplate weatherRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); // 2초
        factory.setReadTimeout(3000);    // 3초

        log.info("weatherRestTemplate 빈 생성 완료 (connect 2s / read 3s)");
        return new RestTemplate(factory);
    }
}