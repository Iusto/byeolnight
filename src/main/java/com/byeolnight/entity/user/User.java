package com.byeolnight.entity.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * 사용자 엔티티 클래스
 * - Spring Security의 UserDetails를 구현하여 인증/인가 기능 지원
 * - 일반 로그인 및 소셜 로그인 지원
 */
@Entity
@Table(
        name = "`user`",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_social_identity",
                columnNames = {"social_provider", "social_provider_id"}
        )
)  // H2 호환성을 위해 백틱 사용 (USER는 H2 예약어)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class User implements UserDetails {

    /** 사용자 역할 열거형 (일반 사용자, 관리자) */
    public enum Role {
        USER, ADMIN
    }

    /** 사용자 상태 열거형 */
    public enum UserStatus {
        ACTIVE, BANNED, SUSPENDED, WITHDRAWN
    }

    /** 기본키 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이메일 (로그인 ID) */
    @Column(nullable = false)
    private String email;

    /** 비밀번호 (BCrypt 암호화 저장) - 소셜 로그인 사용자는 null 가능 */
    @Column(nullable = true)
    private String password;

    /** 닉네임 */
    @Size(min = 2, max = 8, message = "닉네임은 2-8자로 입력해주세요.")
    @Column(nullable = false)
    private String nickname;

    /** OAuth 프로필 이미지 URL */
    @Column
    private String profileImageUrl;
    
    /** 소셜 로그인 제공자 (google, naver, kakao) */
    @Column
    private String socialProvider;

    /** OAuth 제공자가 발급한 변경되지 않는 사용자 식별자 */
    @Column(name = "social_provider_id")
    private String socialProviderId;

    /** 사용자 권한 (USER 또는 ADMIN) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** 사용자 계정 상태 (정상, 밴, 정지, 탈퇴) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /** 닉네임을 한 번이라도 변경한 적 있는지 여부 */
    @Column(nullable = false)
    @Builder.Default
    private boolean nicknameChanged = false;

    /** 마지막 닉네임 변경 시각 */
    @Column(nullable = false)
    private LocalDateTime nicknameUpdatedAt;

    /** 마지막 로그인 시각 */
    @Column
    private LocalDateTime lastLoginAt;

    /** 로그인 실패 횟수 */
    @Column(nullable = false)
    @Builder.Default
    private int loginFailCount = 0;

    /** 계정 잠금 여부 */
    @Column(nullable = false)
    @Builder.Default
    private boolean accountLocked = false;

    /** 마지막 로그인 실패 시각 */
    @Column
    private LocalDateTime lastFailedLogin;



    /** 계정 밴 사유 */
    @Column
    private String banReason;

    /** 사용자 포인트 (스텔라 아이콘 구매용) */
    @Column(nullable = false)
    @Builder.Default
    private int points = 0;

    /** 현재 장착 중인 스텔라 아이콘 ID */
    @Column
    private Long equippedIconId;

    /** 현재 장착 중인 스텔라 아이콘 이름 */
    @Column
    private String equippedIconName;

    /** 탈퇴 사유 */
    @Column(length = 255)
    private String withdrawalReason;

    /** 탈퇴 일시 */
    @Column
    private LocalDateTime withdrawnAt;
    
    /** 탈퇴 신청 여부 확인 (소셜 사용자 복구용) */
    public boolean isWithdrawalRequested() {
        return withdrawnAt != null;
    }

    /** 계정 생성 시각 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (nicknameUpdatedAt == null) {
            nicknameUpdatedAt = LocalDateTime.now();
        }
    }



    // ======================== 🧑‍💼 관리자 기능 ========================

    /** 관리자에 의한 계정 잠금 */
    public void lockAccount() {
        this.accountLocked = true;
    }

    /** 계정 잠금 해제 및 실패 횟수 초기화 */
    public void unlockAccount() {
        this.accountLocked = false;
        this.loginFailCount = 0;
        this.lastFailedLogin = null;
    }

    /** 계정 상태 변경 메서드 */
    public void changeStatus(UserStatus newStatus) {
        this.status = newStatus;
    }

    /** 계정 밴 처리 */
    public void ban(String reason) {
        this.status = UserStatus.BANNED;
        this.banReason = reason;
        this.withdrawnAt = LocalDateTime.now(); // 밴 시점 기록 (1년 후 삭제용)
    }

    /** 계정 밴 해제 */
    public void unban() {
        this.status = UserStatus.ACTIVE;
        this.banReason = null;
        this.withdrawnAt = null; // 밴 해제 시 삭제 스케줄 초기화
    }

    // ======================== 사용자 기능 ========================

    /** 닉네임 업데이트 */
    public void updateNickname(String newNickname, LocalDateTime now) {
        if (!this.nickname.equals(newNickname)) {
            if (this.nicknameChanged && this.nicknameUpdatedAt != null &&
                    this.nicknameUpdatedAt.isAfter(now.minusMonths(6))) {
                throw new IllegalStateException("닉네임은 6개월마다 변경할 수 있습니다.");
            }
            this.nickname = newNickname;
            if (!this.nicknameChanged) {
                this.nicknameChanged = true;
            }
            this.nicknameUpdatedAt = now;
        }
    }



    /** 포인트 증가 */
    public void increasePoints(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("증가할 포인트는 0 이상이어야 합니다.");
        }
        if (this.points > Integer.MAX_VALUE - value) {
            throw new IllegalArgumentException("포인트 오버플로우가 발생할 수 있습니다.");
        }
        this.points += value;
    }

    /** 포인트 차감 (구매 시) */
    public void decreasePoints(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("차감할 포인트는 0 이상이어야 합니다.");
        }
        if (this.points < value) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        this.points -= value;
    }

    /** 스텔라 아이콘 장착 */
    public void equipIcon(Long iconId, String iconName) {
        this.equippedIconId = iconId;
        this.equippedIconName = iconName;
    }

    /** 스텔라 아이콘 해제 */
    public void unequipIcon() {
        this.equippedIconId = null;
        this.equippedIconName = null;
    }

    /** 비밀번호 변경 */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 로그인 성공 처리 */
    public void loginSuccess() {
        this.lastLoginAt = LocalDateTime.now();
        this.loginFailCount = 0;
        this.accountLocked = false;
        this.lastFailedLogin = null;
    }

    /** 로그인 실패 처리 */
    public void loginFail() {
        this.loginFailCount++;
        this.lastFailedLogin = LocalDateTime.now();
        
        // 5회 실패 시 계정 잠금
        if (this.loginFailCount >= 5) {
            this.accountLocked = true;
        }
    }

    /** 회원 탈퇴 처리 */
    public void withdraw(String reason) {
        this.withdrawalReason = reason;
        this.withdrawnAt = LocalDateTime.now();
        this.status = UserStatus.WITHDRAWN;

        // 일반 사용자, 소셜 사용자 모두 1달 유예기간 (복구 가능)
        // 30일 후 스케줄러가 마스킹 처리 (복구 불가능)
    }

    /** 탈퇴 정보 초기화 (복구 시 사용) */
    public void clearWithdrawalInfo() {
        this.withdrawalReason = null;
        this.withdrawnAt = null;
        this.status = UserStatus.ACTIVE; // 복구 시 상태를 ACTIVE로 변경
        // 탈퇴 시 변경된 이메일과 닉네임은 복구 시 수동으로 변경
    }

    /** 소셜 사용자 30일 경과 후 마스킹 처리 (복구 불가능) */
    public void maskAfterThirtyDays() {
        this.email = "withdrawn_" + this.id + "@byeolnight.local";
        this.nickname = "탈퇴회원_" + this.id;
    }
    


    /** 소셜 로그인 사용자인지 확인 */
    public boolean isSocialUser() {
        return this.socialProvider != null;
    }
    
    /** 소셜 로그인 제공자 설정 */
    public void setSocialProvider(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new IllegalArgumentException("소셜 프로바이더는 null이거나 빈 문자열일 수 없습니다.");
        }
        this.socialProvider = provider;
    }

    /** 검증된 OAuth 제공자 계정을 현재 사용자에게 연결 */
    public void linkSocialIdentity(String provider, String providerUserId) {
        if (provider == null || provider.isBlank() || providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("소셜 계정 식별 정보가 올바르지 않습니다.");
        }
        this.socialProvider = provider;
        this.socialProviderId = providerUserId;
    }
    
    /** 소셜 로그인 제공자 이름 반환 */
    public String getSocialProviderName() {
        if (socialProvider == null) return null;
        return switch (socialProvider.toLowerCase(java.util.Locale.ROOT)) {
            case "google" -> "구글";
            case "naver" -> "네이버";
            case "kakao" -> "카카오";
            default -> socialProvider;
        };
    }

    /** 관리자에 의한 닉네임 변경 제한 해제 */
    public void resetNicknameChangeRestriction() {
        this.nicknameChanged = false;
        this.nicknameUpdatedAt = LocalDateTime.now().minusMonths(7);
    }

    /** 복구 시 닉네임 강제 변경 (제한 무시) */
    public void forceUpdateNickname(String newNickname) {
        if (newNickname == null || newNickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 null이거나 빈 문자열일 수 없습니다.");
        }
        if (newNickname.length() < 2 || newNickname.length() > 8) {
            throw new IllegalArgumentException("닉네임은 2-8자로 입력해주세요.");
        }
        this.nickname = newNickname;
        this.nicknameChanged = true;
        this.nicknameUpdatedAt = LocalDateTime.now();
    }

    // ======================== Spring Security 구현부 ========================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked && status != UserStatus.BANNED && status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    // ======================== equals & hashCode ========================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        // ID가 null인 경우 이메일로 비교 (영속화 전 엔티티 처리)
        if (id == null || user.id == null) {
            return Objects.equals(email, user.email);
        }
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        // ID가 null인 경우 이메일 해시코드 사용
        return id != null ? Objects.hash(id) : Objects.hash(email);
    }
}
