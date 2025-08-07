package com.byeolnight.service.auth;

import com.byeolnight.domain.entity.log.AuditLoginLog;
import com.byeolnight.domain.entity.log.AuditSignupLog;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.log.AuditLoginLogRepository;
import com.byeolnight.domain.repository.log.AuditSignupLogRepository;
import com.byeolnight.dto.user.LoginRequestDto;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 인증 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final AuditLoginLogRepository auditLoginLogRepository;
    private final AuditSignupLogRepository auditSignupLogRepository;
    private final com.byeolnight.service.user.UserSecurityService userSecurityService;
    private final com.byeolnight.service.certificate.CertificateService certificateService;

    /**
     * 로그인 인증 처리
     */
    public LoginResult authenticate(LoginRequestDto dto, HttpServletRequest request) {
        String ip = IpUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        log.info("로그인 시도 - 이메일: {}, IP: {}, User-Agent: {}", dto.getEmail(), ip, userAgent);

        // IP 차단 확인
        validateIpNotBlocked(ip);

        // 사용자 조회 및 검증
        User user = findAndValidateUser(dto.getEmail(), ip);

        // 비밀번호 검증
        validatePassword(dto.getPassword(), user, ip, userAgent);

        // 로그인 성공 처리
        userService.resetLoginFailCount(user);
        auditLoginLogRepository.save(AuditLoginLog.of(user.getEmail(), ip, userAgent));

        // 인증서 발급 체크
        certificateService.checkAndIssueCertificates(user, com.byeolnight.service.certificate.CertificateService.CertificateCheckType.LOGIN);

        // 토큰 생성 및 저장
        return createTokens(user);
    }

    private void validateIpNotBlocked(String ip) {
        if (userSecurityService.isIpBlocked(ip)) {
            // log.warn("🚫 차단된 IP 로그인 시도: {}", ip);
            throw new SecurityException("🚫 해당 IP는 비정상적인 로그인 시도(15회 이상)로 인해 1시간 차단되었습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private User findAndValidateUser(String email, String ip) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> {
                    auditSignupLogRepository.save(AuditSignupLog.failure(email, ip, "존재하지 않는 이메일"));
                    // log.info("로그인 시도 실패: 존재하지 않는 이메일 - {}", email);
                    return new BadCredentialsException("존재하지 않는 아이디입니다.");
                });

        // 계정 상태 확인
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            auditSignupLogRepository.save(AuditSignupLog.failure(user.getEmail(), ip, "비활성 상태: " + user.getStatus()));
            throw new BadCredentialsException("해당 계정은 로그인할 수 없습니다. 현재 상태: " + user.getStatus());
        }

        // 계정 잠금 확인
        if (user.isAccountLocked()) {
            auditSignupLogRepository.save(AuditSignupLog.failure(user.getEmail(), ip, "계정 잠김 상태"));
            throw new BadCredentialsException("🔒 계정이 잠겨 있습니다. 비밀번호 초기화를 통해 잠금을 해제하거나 관리자에게 문의하세요.");
        }

        return user;
    }

    private void validatePassword(String password, User user, String ip, String userAgent) {
        if (!userService.checkPassword(password, user)) {
            userService.increaseLoginFailCount(user, ip, userAgent);
            // log.info("로그인 시도 실패: 비밀번호 불일치 - {} (IP: {})", user.getEmail(), ip);

            int failCount = user.getLoginFailCount();
            
            // IP 차단 경고 (15회 시 차단)
            if (failCount >= 15) {
                throw new BadCredentialsException("비정상적인 로그인 시도로 인해 IP가 차단되었습니다. 잠시 후 다시 시도해 주세요.");
            }
            
            // 계정 잠금 상태 확인 (10회 이상)
            if (failCount >= 10) {
                user.lockAccount(); // 계정 잠금 처리
                throw new BadCredentialsException("비밀번호가 10회 이상 틀렸습니다. 계정이 잠겼습니다. 비밀번호를 초기화해야 잠금이 해제됩니다.");
            }
            
            // 5회 이상 실패 시 경고 메시지
            if (failCount >= 5) {
                int remainingAttempts = 10 - failCount;
                throw new BadCredentialsException("⚠️ 경고: 비밀번호를 " + failCount + "회 틀렸습니다. " + remainingAttempts + "회 더 틀리면 계정이 잠깁니다.");
            }

            // 기본 실패 메시지 (1-4회)
            throw new BadCredentialsException("비밀번호가 올바르지 않습니다. (" + failCount + "/10)");
        }
    }

    private LoginResult createTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        long refreshTokenValidity = jwtTokenProvider.getRefreshTokenValidity();

        // Refresh Token을 Redis에 저장
        tokenService.saveRefreshToken(user.getEmail(), refreshToken, refreshTokenValidity);

        return new LoginResult(accessToken, refreshToken, refreshTokenValidity);
    }

    /**
     * 로그인 결과를 담는 내부 클래스
     */
    public static class LoginResult {
        private final String accessToken;
        private final String refreshToken;
        private final long refreshTokenValidity;

        public LoginResult(String accessToken, String refreshToken, long refreshTokenValidity) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.refreshTokenValidity = refreshTokenValidity;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public long getRefreshTokenValidity() { return refreshTokenValidity; }
    }
}