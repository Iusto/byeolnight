package com.byeolnight.dto.user;

import com.byeolnight.entity.user.User;
import com.byeolnight.entity.certificate.UserCertificate;
import lombok.Builder;
import lombok.Getter;

/**
 * 현재 로그인 사용자 정보 응답 DTO
 * GET /api/auth/me 엔드포인트에서 사용
 */
@Getter
@Builder
public class CurrentUserResponseDto {
    private Long id;
    private String email;
    private String nickname;
    private String role;
    private int points;
    private Long equippedIconId;
    private String equippedIconName;
    private String socialProvider;
    private RepresentativeCertificateDto representativeCertificate;

    @Getter
    @Builder
    public static class RepresentativeCertificateDto {
        private String icon;
        private String name;

        public static RepresentativeCertificateDto from(UserCertificate certificate) {
            if (certificate == null) {
                return null;
            }
            return RepresentativeCertificateDto.builder()
                    .icon(certificate.getCertificateType().getIcon())
                    .name(certificate.getCertificateType().getName())
                    .build();
        }
    }

    public static CurrentUserResponseDto from(User user, UserCertificate representativeCertificate) {
        return CurrentUserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .points(user.getPoints())
                .equippedIconId(user.getEquippedIconId())
                .equippedIconName(user.getEquippedIconName())
                .socialProvider(user.getSocialProvider())
                .representativeCertificate(RepresentativeCertificateDto.from(representativeCertificate))
                .build();
    }
}
