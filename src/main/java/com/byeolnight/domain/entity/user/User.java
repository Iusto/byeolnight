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
 * 사용자 엔티티 클래스
 * - Spring Security의 UserDetails를 구현하여 인증/인가 기능 지원
 * - 다양한 상태 값, 보안 속성, 경험치/레벨 시스템, 탈퇴 등 도메인 요구사항을 포함함
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

    /** 비밀번호 (BCrypt 암호화 저장) */
    @Column(nullable = false)
    private String password;

    /** 닉네임 */
    @Column(nullable = false)
    private String nickname;

    /** 전화번호 */
    @Column(nullable = false)
    private String phone;

    /** 사용자 권한 (USER 또는 ADMIN) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** 사용자 계정 상태 (정상, 밴, 정지, 탈퇴) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    /** 닉네임을 한 번이라도 변경한 적 있는지 여부 */
    @Column(nullable = false)
    private boolean nicknameChanged = false;

    /** 마지막 닉네임 변경 시각 */
    @Column(nullable = false)
    private LocalDateTime nicknameUpdatedAt;

    /** 마지막 로그인 시각 */
    @Column
    private LocalDateTime lastLoginAt;

    /** 로그인 실패 횟수 */
    @Column(nullable = false)
    private int loginFailCount = 0;

    /** 계정 잠금 여부 */
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

    /** 계정 밴 사유 */
    @Column
    private String banReason;

    /** 사용자 레벨 */
    @Column(nullable = false)
    private int level = 1;

    /** 사용자 경험치 */
    @Column(nullable = false)
    private int exp = 0;

    /** 탈퇴 사유 */
    @Column(length = 255)
    private String withdrawalReason;

    // ========================== 도메인 메서드 ==========================

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
    }

    /** 계정 밴 해제 */
    public void unban() {
        this.status = UserStatus.ACTIVE;
        this.banReason = null;
    }

// ======================== 🙋 일반 유저 기능 ========================

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

    /** 전화번호 업데이트 */
    public void updatePhone(String newPhone) {
        this.phone = newPhone;
    }

    /** 이메일 인증 완료 처리 */
    public void verifyEmail() {
        this.emailVerified = true;
    }

    /** 휴대폰 인증 완료 처리 */
    public void verifyPhone() {
        this.phoneVerified = true;
    }

    /** 경험치 증가 */
    public void increaseExp(int value) {
        this.exp += value;
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
        if (this.loginFailCount >= 5) {
            this.accountLocked = true;
        }
    }

    /** 회원 탈퇴 처리 */
    public void withdraw(String reason) {
        this.status = UserStatus.WITHDRAWN;
        this.withdrawalReason = reason;
        this.nickname = "탈퇴회원_" + this.id;
        this.email = "withdrawn_" + this.id + "@byeolnight.local";
    }

// ======================== 🔐 Spring Security 구현부 ========================

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

// ======================== ⚖ equals & hashCode ========================

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
