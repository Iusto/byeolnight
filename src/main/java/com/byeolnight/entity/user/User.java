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
    @Builder.Default
    private LocalDateTime nicknameUpdatedAt = LocalDateTime.now();

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

    /** 계정 생성 시각 */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();



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
        this.withdrawnAt = LocalDateTime.now(); // 밴 시점 기록 (5년 후 삭제용)
    }

    /** 계정 밴 해제 */
    public void unban() {
        this.status = UserStatus.ACTIVE;
        this.banReason = null;
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
        this.points += value;
    }

    /** 포인트 차감 (구매 시) */
    public void decreasePoints(int value) {
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
    }

    /** 회원 탈퇴 처리 */
    public void withdraw(String reason) {
        this.status = UserStatus.WITHDRAWN;
        this.withdrawalReason = reason;
        this.withdrawnAt = LocalDateTime.now();
        this.nickname = "탈퇴회원_" + this.id;
        this.email = "withdrawn_" + this.id + "@byeolnight.local";
        // socialProvider 유지 (소셜 사용자 구분용)
    }

    /** 탈퇴 정보 초기화 (복구 시 사용) */
    public void clearWithdrawalInfo() {
        this.withdrawalReason = null;
        this.withdrawnAt = null;
        // 탈퇴 시 변경된 이메일과 닉네임은 복구 시 수동으로 변경
    }

    /** 개인정보 완전 삭제 (5년 경과 후) */
    public void completelyRemovePersonalInfo() {
        this.nickname = "DELETED_" + this.id;
        this.email = "deleted_" + this.id + "@removed.local";
        this.withdrawalReason = "5년 경과로 인한 자동 삭제";
    }

    /** 소셜 로그인 사용자인지 확인 */
    public boolean isSocialUser() {
        return this.socialProvider != null;
    }
    
    /** 소셜 로그인 제공자 설정 */
    public void setSocialProvider(String provider) {
        this.socialProvider = provider;
    }
    
    /** 소셜 로그인 제공자 이름 반환 */
    public String getSocialProviderName() {
        if (socialProvider == null) return null;
        return switch (socialProvider.toLowerCase()) {
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
        return status != UserStatus.BANNED && status != UserStatus.SUSPENDED;
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
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
