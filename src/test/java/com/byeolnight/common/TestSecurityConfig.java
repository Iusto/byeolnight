package com.byeolnight.common;

import com.byeolnight.infrastructure.security.AuthWhitelist;
import com.byeolnight.infrastructure.security.JwtAuthenticationEntryPoint;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 컨트롤러 테스트용 공통 Security 설정
 * - JWT 필터, OAuth2 등 복잡한 인프라 없이 URL 기반 인가만 적용
 * - SecurityConfig를 excludeFilters로 제외한 @WebMvcTest에서 @Import로 사용
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(AuthWhitelist.PATHS).permitAll()
                .requestMatchers("/api/member/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint())
            );
        return http.build();
    }
}
