package com.byeolnight.dto.comment;

import com.byeolnight.domain.entity.comment.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponseDto {
    private Long id;
    private String content;
    private String writer;
    private Long writerId;
    private Boolean blinded;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private Long parentId;
    private String parentWriter;
    private String writerIcon;
    private java.util.List<String> writerCertificates;

    public static CommentResponseDto from(Comment comment) {
        System.out.println("CommentResponseDto.from 호출 - 댓글 ID: " + comment.getId());
        
        // writer 정보 상세 체크
        String writerName;
        if (comment.getWriter() != null) {
            writerName = comment.getWriter().getNickname();
            System.out.println("정상 writer: " + writerName);
        } else {
            writerName = "알 수 없는 사용자";
            System.out.println("Writer가 null입니다!");
        }
        
        // 부모 댓글 정보 처리
        Long parentId = null;
        String parentWriter = null;
        if (comment.getParent() != null) {
            parentId = comment.getParent().getId();
            parentWriter = (comment.getParent().getWriter() != null) ? 
                comment.getParent().getWriter().getNickname() : "알 수 없는 사용자";
        }
        
        // 사용자 아이콘 정보 가져오기
        String writerIcon = null;
        java.util.List<String> writerCertificates = new java.util.ArrayList<>();
        
        if (comment.getWriter() != null) {
            // 장착된 아이콘 정보 가져오기
            writerIcon = comment.getWriter().getEquippedIconName();
            
            // 대표 인증서 조회
            try {
                com.byeolnight.service.certificate.CertificateService certificateService = 
                    com.byeolnight.infrastructure.config.ApplicationContextProvider
                        .getBean(com.byeolnight.service.certificate.CertificateService.class);
                com.byeolnight.domain.entity.certificate.UserCertificate repCert = 
                    certificateService.getRepresentativeCertificate(comment.getWriter());
                if (repCert != null) {
                    writerCertificates.add(repCert.getCertificateType().getName());
                }
            } catch (Exception e) {
                // 인증서 조회 실패 시 무시
            }
        }
        
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getBlinded() ? "[블라인드 처리된 댓글입니다]" : comment.getContent())
                .writer(writerName)
                .writerId(comment.getWriter() != null ? comment.getWriter().getId() : null)
                .blinded(comment.getBlinded())
                .deleted(comment.getDeleted())
                .createdAt(comment.getCreatedAt())
                .parentId(parentId)
                .parentWriter(parentWriter)
                .writerIcon(writerIcon)
                .writerCertificates(writerCertificates)
                .build();
    }
}