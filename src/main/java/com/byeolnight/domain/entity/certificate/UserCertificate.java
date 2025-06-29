package com.byeolnight.domain.entity.certificate;

import com.byeolnight.domain.entity.common.BaseTimeEntity;
import com.byeolnight.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_certificates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserCertificate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Certificate.CertificateType certificateType;

    @Column(nullable = false)
    @lombok.Builder.Default
    private boolean isRepresentative = false; // 대표 인증서 여부

    public static UserCertificate of(User user, Certificate.CertificateType certificateType) {
        return UserCertificate.builder()
                .user(user)
                .certificateType(certificateType)
                .build();
    }

    public void setAsRepresentative() {
        this.isRepresentative = true;
    }

    public void unsetAsRepresentative() {
        this.isRepresentative = false;
    }
}