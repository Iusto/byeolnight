package com.byeolnight.infrastructure.security;

import com.byeolnight.entity.user.User;
import com.byeolnight.service.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class NicknameRequiredFilter extends OncePerRequestFilter {

    @Autowired
    @Lazy
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            try {
                User user = userService.findByEmail(auth.getName()).orElse(null);
                
                if (user != null && (user.getNickname() == null || user.getNickname().trim().isEmpty())) {
                    String requestURI = request.getRequestURI();
                    
                    // 인증 및 공개 API는 허용
                    if (!requestURI.startsWith("/auth/") &&
                        !requestURI.startsWith("/public/")) {
                        
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"success\":false,\"message\":\"닉네임 설정이 필요합니다\",\"code\":\"NICKNAME_REQUIRED\"}");
                        return;
                    }
                }
            } catch (Exception e) {
                // 사용자 조회 실패 시 계속 진행
            }
        }
        
        filterChain.doFilter(request, response);
    }
}