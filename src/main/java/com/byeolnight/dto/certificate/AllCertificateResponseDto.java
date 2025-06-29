package com.byeolnight.dto.certificate;

import com.byeolnight.domain.entity.certificate.Certificate;
import com.byeolnight.service.certificate.CertificateService;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AllCertificateResponseDto {

    private String type;
    private String name;
    private String icon;
    private String description;
    private String howToGet; // 획득 방법
    private boolean owned; // 보유 여부
    private boolean isRepresentative;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime issuedAt;

    public static AllCertificateResponseDto from(CertificateService.CertificateInfo info) {
        Certificate.CertificateType type = info.getType();
        
        return AllCertificateResponseDto.builder()
                .type(type.name())
                .name(type.getName())
                .icon(type.getIcon())
                .description(type.getDescription())
                .howToGet(getHowToGetText(type))
                .owned(info.isOwned())
                .isRepresentative(info.isRepresentative())
                .issuedAt(info.getIssuedAt())
                .build();
    }

    private static String getHowToGetText(Certificate.CertificateType type) {
        return switch (type) {
            case STARLIGHT_EXPLORER -> "첫 로그인 시 자동으로 발급됩니다.";
            case SPACE_CITIZEN -> "회원가입과 이메일, 전화번호 인증을 완료하면 발급됩니다.";
            case GALAXY_COMMUNICATOR -> "댓글을 5회 이상 작성하면 발급됩니다.";
            case STAR_OBSERVER -> "IMAGE 게시판에 사진을 3장 이상 업로드하면 발급됩니다.";
            case TOUR_MASTER -> "천문대 견학 게시판에서 견학 일정을 3회 이상 조회하면 발급됩니다.";
            case CHAT_MASTER -> "채팅방에서 5회 이상 대화에 참여하면 발급됩니다.";
            case NIGHT_CITIZEN -> "포인트를 200점 이상 누적하면 발급됩니다.";
            case GUARDIAN -> "게시글 신고로 커뮤니티 정화에 기여하면 발급됩니다.";
            case EXPERIMENTER -> "게시글을 3건 이상 작성하면 발급됩니다.";
            case ADMIN_MEDAL -> "관리자가 특별한 기여를 인정하여 수동으로 발급하는 훈장입니다.";
        };
    }
}