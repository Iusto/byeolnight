package com.byeolnight.service.auth;

import com.byeolnight.entity.user.User;
import com.byeolnight.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * 탈퇴 후 유예 기간 안에 있는 계정의 복구 규칙을 한곳에서 관리한다.
 *
 * <p>OAuth 복구는 제공자와 제공자 회원 ID를 다시 검증하고, 비밀번호 계정은
 * 로그인 단계에서 비밀번호 검증을 통과한 일회용 티켓만 허용한다. 이 서비스는
 * 티켓 발급 여부를 판단하지 않고 최종 계정 상태 변경만 담당한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private static final int RECOVERY_GRACE_PERIOD_DAYS = 30;
    private static final String MASKED_EMAIL_PREFIX = "withdrawn_";

    private final UserRepository userRepository;

    /** 관리자 기능에서 이메일로 탈퇴 계정을 복구할 때 사용한다. */
    @Transactional
    public boolean recoverWithdrawnAccount(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            log.warn("복구 시도 실패: 존재하지 않는 이메일 - {}", email);
            return false;
        }

        User user = userOptional.get();
        if (!isRecoverable(user)) {
            log.warn("복구 시도 실패: 복구 기간 만료 또는 탈퇴 계정 아님 - {}", email);
            return false;
        }

        try {
            user.clearWithdrawalInfo();
            log.info("계정 복구 완료: 이메일={}, 닉네임={}, 타입={}",
                    email, user.getNickname(), user.isSocialUser()
                            ? "소셜(" + user.getSocialProvider() + ")"
                            : "일반");
            return true;
        } catch (Exception e) {
            log.error("계정 복구 중 오류 발생: 이메일={}, 오류={}", email, e.getMessage(), e);
            return false;
        }
    }

    /** OAuth 재인증에서 확인한 외부 계정과 정확히 일치하는 계정만 복구한다. */
    @Transactional
    public User recoverOAuthAccount(Long userId, String provider, String providerUserId) {
        User user = findRecoveryTarget(userId);

        if (!user.isSocialUser() || !Objects.equals(provider, user.getSocialProvider())) {
            throw new IllegalArgumentException("복구할 소셜 계정 정보가 일치하지 않습니다.");
        }

        if (user.getSocialProviderId() == null) {
            // 레거시 회원은 OAuth 재인증으로 확인한 불변 식별자를 복구 시점에 연결한다.
            user.linkSocialIdentity(provider, providerUserId);
        } else if (!Objects.equals(providerUserId, user.getSocialProviderId())) {
            throw new IllegalArgumentException("복구할 외부 계정 식별 정보가 일치하지 않습니다.");
        }

        validateRecoverableUser(user);
        user.clearWithdrawalInfo();
        log.info("OAuth 계정 복구 완료: 사용자ID={}, 제공자={}", user.getId(), provider);
        return user;
    }

    /** 로그인 비밀번호 검증 후 발급한 일회용 티켓으로 일반 계정을 복구한다. */
    @Transactional
    public User recoverPasswordAccount(Long userId) {
        User user = findRecoveryTarget(userId);
        if (user.isSocialUser()) {
            throw new IllegalArgumentException("비밀번호 계정 복구 요청이 아닙니다.");
        }

        validateRecoverableUser(user);
        user.clearWithdrawalInfo();
        log.info("일반 계정 복구 완료: 사용자ID={}", user.getId());
        return user;
    }

    /** 계정이 마스킹되기 전 30일 유예 기간 안에 있는지 확인한다. */
    @Transactional(readOnly = true)
    public boolean canRecover(String email) {
        return userRepository.findByEmail(email)
                .map(this::isRecoverable)
                .orElse(false);
    }

    private User findRecoveryTarget(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("복구할 계정을 찾을 수 없습니다."));
    }

    private void validateRecoverableUser(User user) {
        if (!isRecoverable(user)) {
            throw new IllegalArgumentException("복구 가능한 탈퇴 계정이 아니거나 복구 기간이 지났습니다.");
        }
    }

    private boolean isRecoverable(User user) {
        if (!user.isWithdrawalRequested() || user.getWithdrawnAt() == null) {
            return false;
        }
        if (user.getEmail().startsWith(MASKED_EMAIL_PREFIX)) {
            return false;
        }

        LocalDateTime recoveryDeadline = LocalDateTime.now().minusDays(RECOVERY_GRACE_PERIOD_DAYS);
        return !user.getWithdrawnAt().isBefore(recoveryDeadline);
    }
}
