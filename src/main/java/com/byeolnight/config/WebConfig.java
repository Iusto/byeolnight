package com.byeolnight.config;

import com.byeolnight.infrastructure.filter.ContentCachingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 웹 관련 설정 클래스
 * - 필터 등록 및 우선순위 설정
 */
@Configuration
public class WebConfig {

    /**
     * ContentCachingFilter를 가장 높은 우선순위로 등록
     * Spring Security 필터보다 먼저 실행되도록 설정
     */
    @Bean
    public FilterRegistrationBean<ContentCachingFilter> contentCachingFilterRegistration(ContentCachingFilter filter) {
        FilterRegistrationBean<ContentCachingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE); // 가장 높은 우선순위
        registration.addUrlPatterns("/api/*"); // 모든 API 요청에 적용
        return registration;
    }
}