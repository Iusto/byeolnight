package com.byeolnight.infrastructure.security;

import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.UserRepository;
import com.byeolnight.service.auth.TokenService;
import com.byeolnight.service.user.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 전반을 설정하는 클래스
 * - JWT 기반 인증 필터 등록
 * - URL 접근 권한 관리
 * - 비밀번호 인코딩, 인증 매니저 설정
 */
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize 등을 메서드 단위에서 활성화
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenService tokenService; // ✅ Redis 블랙리스트 조회용
    private final UserRepository userRepository;

    /**
     * JWT 인증 필터 등록 (DI: 토큰 제공자 + 사용자 서비스 + 토큰 서비스)
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService, tokenService);
    }

    /**
     * Spring Security 필터 체인 구성
     * - CSRF 비활성화 (JWT 기반이므로)
     * - 인증/인가 설정
     * - JWT 필터 등록 (기존 UsernamePasswordAuthenticationFilter 앞에)
     * - 인증 실패 시 401 반환 처리
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AuthWhitelist.PATHS).permitAll() // 화이트리스트 경로는 인증 제외
                        .anyRequest().authenticated()                    // 그 외 요청은 인증 필요
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                ));

        return http.build();
    }

    /**
     * 비밀번호 암호화용 인코더
     * - BCrypt 사용
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 인증 매니저 등록
     * - 로그인 시 AuthenticationManager를 통해 인증 처리됨
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * 관리자 권한 인증
     * - 로그인 시 AuthenticationManager를 통해 인증 처리됨
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"접근 권한이 없습니다.\"}");
        };
    }
}
