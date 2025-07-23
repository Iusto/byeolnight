package com.byeolnight.config;

import com.byeolnight.infrastructure.filter.ContentCachingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 관련 설정 클래스
 * - 필터 등록 및 우선순위 설정
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

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
    
    /**
     * CORS 설정
     * - 모든 출처 허용
     * - 모든 HTTP 메서드 허용
     * - 쿠키 포함 요청 허용 (withCredentials)
     * - 인앱 브라우저 호환성을 위해 SameSite=Lax 설정
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // 모든 출처 허용 (allowCredentials와 함께 사용하기 위해 패턴 사용)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // 쿠키 포함 요청 허용
                .maxAge(3600); // 1시간 동안 preflight 요청 캐싱
    }
    
    /**
     * CORS 필터 설정
     * - Spring Security 필터 체인보다 먼저 실행되도록 설정
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 모든 출처 허용 (또는 특정 도메인만 지정)
        config.addAllowedOriginPattern("*"); // allowCredentials와 함께 사용하기 위해 패턴 사용
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true); // 쿠키 포함 요청 허용
        config.setMaxAge(3600L); // 1시간 동안 preflight 요청 캐싱
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
    
    /**
     * 정적 리소스 설정
     * - robots.txt, sitemap.xml 등의 파일을 제공하기 위한 설정
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/robots.txt", "/sitemap.xml", "/sitemap-*.xml")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // 1시간 캐싱
    }
}