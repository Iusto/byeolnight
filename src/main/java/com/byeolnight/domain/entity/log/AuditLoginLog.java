package com.byeolnight.domain.entity.log;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuditLoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String ipAddress;

    private String userAgent;

    private LocalDateTime loggedInAt;

    public static AuditLoginLog of(String email, String ipAddress, String userAgent) {
        return AuditLoginLog.builder()
                .email(email)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .loggedInAt(LocalDateTime.now())
                .build();
    }
}
