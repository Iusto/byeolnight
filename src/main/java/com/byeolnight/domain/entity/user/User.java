package com.byeolnight.domain.entity.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * 사용자 엔티티
 * - Spring Security 연동을 위한 UserDetails 구현
 * - 닉네임 변경 제약, 밴 여부, 경험치/레벨, 로그인 보안 정책 포함
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class User implements UserDetails {

    public enum Role {
        USER, ADMIN
    }

    public enum UserStatus {
        ACTIVE, BANNED, SUSPENDED, WITHDRAWN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이메일(로그인 ID) */
    @Column(nullable = false)
    private String email;

    /** 비밀번호(BCrypt 암호화 저장) */
    @Column(nullable = false)
    private String password;

    /** 유저 닉네임 */
    @Column(nullable = false)
    private String nickname;

    /** 전화번호 */
    @Column(nullable = false)
    private String phone;

    /** 유저 권한 (USER / ADMIN) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** 유저 현재 상태 (정상/밴/정지/탈퇴) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    /** 닉네임 변경 여부 (최초 변경 허용용) */
    @Column(nullable = false)
    private boolean nicknameChanged = false;

    /** 마지막 닉네임 변경 시점 */
    @Column(nullable = false)
    private LocalDateTime nicknameUpdatedAt;

    /** 마지막 로그인 시각 */
    @Column
    private LocalDateTime lastLoginAt;

    /** 로그인 실패 횟수 (기본값: 0) */
    @Column(nullable = false)
    private int loginFailCount = 0;

    /** 계정 잠금 여부 - true면 로그인 불가 */
    @Column(nullable = false)
    private boolean accountLocked = false;

    /** 마지막 로그인 실패 시각 */
    @Column
    private LocalDateTime lastFailedLogin;

    /** 이메일 인증 여부 */
    @Column(nullable = false)
    private boolean emailVerified = false;

    /** 휴대폰 인증 여부 */
    @Column(nullable = false)
    private boolean phoneVerified = false;

    /** 밴 사유 */
    @Column
    private String banReason;

    /** 유저 레벨 */
    @Column(nullable = false)
    private int level = 1;

    /** 유저 경험치 */
    @Column(nullable = false)
    private int exp = 0;

    /** 탈퇴 사유 */
    @Column(length = 255)
    private String withdrawalReason;

    // ========================== 도메인 메서드 ==========================

    public void updateNickname(String newNickname, LocalDateTime now) {
        if (!this.nickname.equals(newNickname)) {
            if (this.nicknameChanged &&
                    this.nicknameUpdatedAt != null &&
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

    public void updatePhone(String newPhone) {
        this.phone = newPhone;
    }

    public void increaseExp(int value) {
        this.exp += value;
    }

    public void ban(String reason) {
        this.status = UserStatus.BANNED;
        this.banReason = reason;
    }

    public void unban() {
        this.status = UserStatus.ACTIVE;
        this.banReason = null;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void verifyPhone() {
        this.phoneVerified = true;
    }

    /** 로그인 성공 처리 (로그인 시각 기록 + 실패횟수 초기화) */
    public void loginSuccess() {
        this.lastLoginAt = LocalDateTime.now();
        this.loginFailCount = 0;
        this.accountLocked = false;
        this.lastFailedLogin = null;
    }

    /**
     * 로그인 실패 처리
     * - 실패 횟수 증가
     * - 마지막 실패 시간 기록
     * - 일정 횟수 초과 시 계정 잠금
     */
    public void loginFail() {
        this.loginFailCount++;
        this.lastFailedLogin = LocalDateTime.now();
        if (this.loginFailCount >= 5) {
            this.accountLocked = true;
        }
    }

    public void withdraw(String reason) {
        this.status = UserStatus.WITHDRAWN;
        this.withdrawalReason = reason;
        this.nickname = "탈퇴회원_" + this.id;
        this.email = "withdrawn_" + this.id + "@byeolnight.local";
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // ======================== Spring Security ========================

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

    /** 정지/밴 상태면 로그인 불가 */
    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.BANNED && status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** ACTIVE 상태만 로그인 허용 */
    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    // =========================== equals ============================

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
