package com.byeolnight.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 검색 엔진 확인 파일처럼 백엔드가 직접 제공하는 정적 리소스만 설정한다.
 * CORS 정책은 {@code CorsConfig}에서 일괄 관리한다.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/robots.txt", "/naver*.html")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }
}
