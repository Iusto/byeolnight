package com.byeolnight.infrastructure.security;

import com.byeolnight.entity.user.User;
import com.byeolnight.service.auth.TokenService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("JWT 인증 필터 테스트")
class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("JWT subject의 사용자 ID로 전체 사용자를 복원한다")
    void authenticatesWithUserIdClaim() throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        TokenService tokenService = mock(TokenService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                tokenProvider, userDetailsService, tokenService);

        User fullUser = User.builder()
                .id(42L)
                .email("member@example.com")
                .nickname("별지기")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build();
        given(tokenProvider.validate("access-token")).willReturn(true);
        given(tokenProvider.getUserIdFromToken("access-token")).willReturn(42L);
        given(tokenService.isAccessTokenBlacklisted("access-token")).willReturn(false);
        given(userDetailsService.loadUserByUsername("42")).willReturn(fullUser);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/member/users/me");
        request.setCookies(new jakarta.servlet.http.Cookie("accessToken", "access-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isSameAs(fullUser);
        verify(userDetailsService).loadUserByUsername("42");
        verify(filterChain).doFilter(request, response);
    }
}
