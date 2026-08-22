package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.user.DefaultIconService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 처음 로그인한 소셜 사용자의 회원 가입과 초기 설정을 담당한다.
 *
 * <p>기존 회원 인증과 신규 회원 초기화는 변경 이유가 다르므로 분리한다.
 * 닉네임 생성, 기본 아이콘, 로그인 인증서처럼 가입 시에만 필요한 작업은
 * 이 서비스 안에서 완료한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialUserRegistrationService {

    private static final int MAX_RANDOM_NICKNAME_ATTEMPTS = 10;

    private final UserRepository userRepository;
    private final DefaultIconService defaultIconService;
    private final CertificateService certificateService;

    /** 제공자에서 검증한 사용자 정보로 신규 소셜 회원을 생성한다. */
    @Transactional
    public User register(String provider, OAuth2UserInfoFactory.OAuth2UserInfo userInfo) {
        String nickname = generateUniqueNickname(userInfo.getEmail().split("@")[0]);

        User newUser = User.builder()
                .email(userInfo.getEmail())
                .password(null)
                .nickname(nickname)
                .profileImageUrl(userInfo.getImageUrl())
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .points(0)
                .nicknameChanged(false)
                .build();

        newUser.linkSocialIdentity(provider, userInfo.getProviderUserId());
        User savedUser = userRepository.save(newUser);
        initializeNewSocialUser(savedUser);
        return savedUser;
    }

    private void initializeNewSocialUser(User user) {
        try {
            defaultIconService.grant(user);
            certificateService.checkAndIssueCertificates(user, CertificateService.CertificateCheckType.LOGIN);
            log.info("소셜 로그인 사용자 {}에게 기본 아이콘 및 인증서 발급 완료", user.getNickname());
        } catch (Exception e) {
            // 부가 초기화 오류를 기록한 뒤 가입 흐름을 계속 시도하는 기존 정책을 유지한다.
            log.error("소셜 로그인 사용자 기본 설정 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private String generateUniqueNickname(String baseNickname) {
        String normalizedNickname = normalizeNickname(baseNickname);

        if (!userRepository.existsByNickname(normalizedNickname)) {
            return normalizedNickname;
        }

        for (int attempt = 1; attempt <= MAX_RANDOM_NICKNAME_ATTEMPTS; attempt++) {
            String uniqueSuffix = UUID.randomUUID().toString().substring(0, 4);
            String prefix = normalizedNickname.length() > 4
                    ? normalizedNickname.substring(0, 4)
                    : normalizedNickname;
            String candidateNickname = prefix + uniqueSuffix;

            if (!userRepository.existsByNickname(candidateNickname)) {
                log.info("고유 닉네임 생성 완료: {} -> {} (시도 횟수: {})", baseNickname, candidateNickname, attempt);
                return candidateNickname;
            }
        }

        // 극히 드문 연속 충돌에서도 가입 흐름을 중단하지 않기 위한 최종 대체값이다.
        String fallbackNickname = "사용자" + System.currentTimeMillis() % 100000;
        log.warn("닉네임 생성 최대 시도 초과, 타임스탬프 기반 닉네임 사용: {}", fallbackNickname);
        return fallbackNickname;
    }

    private String normalizeNickname(String nickname) {
        if (nickname.length() < 2) {
            return "사용자";
        }
        return nickname.length() > 8 ? nickname.substring(0, 8) : nickname;
    }
}
