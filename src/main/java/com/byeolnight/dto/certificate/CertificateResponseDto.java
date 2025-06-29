package com.byeolnight.dto.certificate;

import com.byeolnight.domain.entity.certificate.Certificate;
import com.byeolnight.domain.entity.certificate.UserCertificate;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class CertificateResponseDto {

    private String type;
    private String name;
    private String icon;
    private String description;
    private boolean isRepresentative;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime issuedAt;

    public static CertificateResponseDto from(UserCertificate userCertificate) {
        Certificate.CertificateType type = userCertificate.getCertificateType();
        
        return CertificateResponseDto.builder()
                .type(type.name())
                .name(type.getName())
                .icon(type.getIcon())
                .description(type.getDescription())
                .isRepresentative(userCertificate.isRepresentative())
                .issuedAt(userCertificate.getCreatedAt())
                .build();
    }
}