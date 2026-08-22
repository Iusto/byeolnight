package com.byeolnight.service.auth;

import com.byeolnight.entity.log.AuditLoginLog;
import com.byeolnight.entity.log.AuditSignupLog;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.log.AuditLoginLogRepository;
import com.byeolnight.repository.log.AuditSignupLogRepository;
import com.byeolnight.dto.user.LoginRequestDto;
import com.byeolnight.infrastructure.security.JwtTokenProvider;
import com.byeolnight.infrastructure.util.IpUtil;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.user.UserAccountService;
import com.byeolnight.service.user.UserAdminService;
import com.byeolnight.service.user.UserQueryService;
import com.byeolnight.service.user.UserSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;

/**
 * 인증 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserQueryService userQueryService;
    private final UserAccountService userAccountService;
    private final UserAdminService userAdminService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLoginLogRepository auditLoginLogRepository;
    private final AuditSignupLogRepository auditSignupLogRepository;
    private final UserSecurityService userSecurityService;
    private final CertificateService certificateService;
    private final AccountRecoveryService accountRecoveryService;
    private final AccountRecoveryTicketService accountRecoveryTicketService;
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
        User user = findAndValidateUser(dto.getEmail(), dto.getPassword(), ip, userAgent);

        // 비밀번호 검증
        validatePassword(dto.getPassword(), user, ip, userAgent);

        // 로그인 성공 처리
        userAdminService.resetLoginFailCount(user);
        auditLoginLogRepository.save(AuditLoginLog.of(user.getEmail(), ip, userAgent));

        // 인증서 발급 체크
        certificateService.checkAndIssueCertificates(user, CertificateService.CertificateCheckType.LOGIN);

        // 토큰 생성 및 저장
        return createTokens(user, ip, request.getHeader("User-Agent"));
    }

    private void validateIpNotBlocked(String ip) {
        if (userSecurityService.isIpBlocked(ip)) {
            log.warn("🚫 차단된 IP 로그인 시도: {}", ip);
            throw new SecurityException("🚫 해당 IP는 비정상적인 로그인 시도(15회 이상)로 인해 1시간 차단되었습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private User findAndValidateUser(String email, String password, String ip, String userAgent) {
        User user = userQueryService.findByEmail(email)
                .orElseThrow(() -> {
                    auditSignupLogRepository.save(AuditSignupLog.failure(email, ip, "존재하지 않는 이메일"));
                    return new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
                });

        // 계정 상태 확인
        if (user.getStatus() == User.UserStatus.WITHDRAWN) {
            if (accountRecoveryService.canRecover(user.getEmail())) {
                validatePassword(password, user, ip, userAgent);
                String recoveryTicket = accountRecoveryTicketService.issuePassword(user.getId());
                auditSignupLogRepository.save(AuditSignupLog.failure(user.getEmail(), ip, "탈퇴 계정 복구 가능"));
                throw new BadCredentialsException("ACCOUNT_RECOVERY_REQUIRED:" + recoveryTicket);
            } else {
                // 복구 불가능한 계정 (30일 경과)
                auditSignupLogRepository.save(AuditSignupLog.failure(user.getEmail(), ip, "탈퇴 계정 복구 불가"));
                throw new BadCredentialsException("탈퇴한 계정입니다.");
            }
        } else if (user.getStatus() != User.UserStatus.ACTIVE) {
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
        // 소셜 로그인 사용자는 비밀번호 검증 스킵
        if (user.isSocialUser()) {
            String providerName = user.getSocialProviderName();
            if (providerName != null) {
                throw new BadCredentialsException("해당 이메일은 " + providerName + " 로그인으로 가입된 계정입니다. " + providerName + " 로그인을 이용해주세요.");
            } else {
                throw new BadCredentialsException("해당 이메일은 소셜 로그인으로 가입된 계정입니다. 네이버/구글/카카오 로그인을 이용해주세요.");
            }
        }
        
        if (!userAccountService.checkPassword(password, user)) {
            userAdminService.increaseLoginFailCount(user, ip, userAgent);
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

    public User findOrCreateOAuthUser(String registrationId, OAuth2User oAuth2User) {
        OAuth2UserInfoFactory.OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User);
        
        return userQueryService.findByEmail(userInfo.getEmail())
                .orElseGet(() -> createOAuthUser(userInfo));
    }

    private User createOAuthUser(OAuth2UserInfoFactory.OAuth2UserInfo userInfo) {
        User user = User.builder()
                .email(userInfo.getEmail())
                .password(null) // 소셜 로그인 사용자는 비밀번호 없음
                .nickname("")
                .profileImageUrl(userInfo.getImageUrl())
                .role(User.Role.USER)

                .build();
        
        return userAccountService.save(user);
    }

    public boolean needsNicknameSetup(Long userId) {
        User user = userQueryService.findById(userId);
        return user.getNickname() == null || user.getNickname().trim().isEmpty();
    }

    public String[] loginUser(User user, String clientInfo, String ipAddress, HttpServletResponse response) {
        String[] tokens = jwtTokenProvider.generateTokens(user, clientInfo, ipAddress);
        
        // HttpOnly 쿠키로 Refresh Token 설정
        Cookie refreshCookie = new Cookie("refreshToken", tokens[1]);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/auth");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        refreshCookie.setAttribute("SameSite", "Lax");
        response.addCookie(refreshCookie);
        
        return tokens;
    }

    private LoginResult createTokens(User user, String ipAddress, String userAgent) {
        String[] tokens = jwtTokenProvider.generateTokens(user, userAgent, ipAddress);
        
        return new LoginResult(tokens[0], tokens[1], 7 * 24 * 60 * 60 * 1000L);
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
