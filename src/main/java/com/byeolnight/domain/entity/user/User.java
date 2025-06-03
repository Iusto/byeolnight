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
 * - 닉네임 변경 제약, 밴 여부, 경험치/레벨 기능 포함
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    /**
     * 사용자 권한 구분
     */
    public enum Role {
        USER, ADMIN
    }

    /**
     * 사용자 상태 구분
     * - ACTIVE: 정상
     * - BANNED: 밴 처리됨
     * - SUSPENDED: 일시 정지
     * - WITHDRAWN: 탈퇴 처리됨
     */
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

    /** 마지막 닉네임 변경 시점 (가입 시 now(), 이후 변경 시 갱신) */
    @Column(nullable = false)
    private LocalDateTime nicknameUpdatedAt;

    /** 마지막 로그인 시각 (통계용) */
    @Column
    private LocalDateTime lastLoginAt;

    /** 로그인 실패 횟수 (보안 정책용) */
    @Column(nullable = false)
    private int loginFailCount = 0;

    /** 이메일 인증 여부 */
    @Column(nullable = false)
    private boolean emailVerified = false;

    /** 휴대폰 인증 여부 */
    @Column(nullable = false)
    private boolean phoneVerified = false;

    /** 밴 사유 (status = BANNED일 때만 의미 있음) */
    @Column
    private String banReason;

    /** 유저 레벨 (경험치 기반 성장) */
    @Column(nullable = false)
    private int level = 1;

    /** 유저 경험치 (글쓰기/댓글 등 활동 기반 누적) */
    @Column(nullable = false)
    private int exp = 0;

    /** 탈퇴 사유 저장 */
    @Column(length = 255)
    private String withdrawalReason;

    // ========================== 도메인 메서드 ==========================

    /**
     * 닉네임 변경 처리
     * - 최초 변경 시 nicknameChanged = true로 변경
     */
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

    /** 전화 변호 변경 (전화 번호 변경 시 인증 완료 시에만 수행) */
    public void updatePhone(String newPhone) {
        this.phone = newPhone;
    }


    /** 경험치 증가 (레벨업 로직은 별도 Policy에서 수행) */
    public void increaseExp(int value) {
        this.exp += value;
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

    /** 이메일 인증 처리 */
    public void verifyEmail() {
        this.emailVerified = true;
    }

    /** 휴대폰 인증 처리 */
    public void verifyPhone() {
        this.phoneVerified = true;
    }

    /** 로그인 성공 처리 (로그인 시각 기록 + 실패횟수 초기화) */
    public void loginSuccess() {
        this.lastLoginAt = LocalDateTime.now();
        this.loginFailCount = 0;
    }

    /** 로그인 실패 처리 (보안 정책 기반으로 계정 잠금 가능) */
    public void loginFail() {
        this.loginFailCount++;
    }

    /** 회원탈퇴 처리 (status = WITHDRAWN) */
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
