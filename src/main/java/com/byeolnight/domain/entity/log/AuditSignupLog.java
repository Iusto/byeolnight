package com.byeolnight.domain.entity.log;

import com.byeolnight.domain.entity.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuditSignupLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Result result;

    private String failureReason;

    public enum Result {
        SUCCESS, FAILURE
    }

    public static AuditSignupLog success(String email, String ipAddress) {
        return AuditSignupLog.builder()
                .email(email)
                .ipAddress(ipAddress)
                .result(Result.SUCCESS)
                .build();
    }

    public static AuditSignupLog failure(String email, String ipAddress, String reason) {
        return AuditSignupLog.builder()
                .email(email)
                .ipAddress(ipAddress)
                .result(Result.FAILURE)
                .failureReason(reason)
                .build();
    }
}