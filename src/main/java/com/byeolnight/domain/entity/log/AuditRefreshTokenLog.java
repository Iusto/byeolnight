package com.byeolnight.domain.entity.log;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Refresh Token 재발급 감사 로그
 * - 운영환경 보안 감사 추적용
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuditRefreshTokenLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사용자 이메일 */
    @Column(nullable = false)
    private String email;

    /** 요청 IP 주소 */
    @Column(nullable = false)
    private String ipAddress;

    /** 요청 User-Agent */
    @Column(nullable = false, length = 512)
    private String userAgent;

    /** 토큰 발급 시각 */
    @Column(nullable = false)
    private LocalDateTime issuedAt;

    public static AuditRefreshTokenLog of(String email, String ipAddress, String userAgent) {
        return AuditRefreshTokenLog.builder()
                .email(email)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .issuedAt(LocalDateTime.now())
                .build();
    }
}
