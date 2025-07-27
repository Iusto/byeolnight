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
    private int likeCount;
    private int reportCount;
    private boolean isPopular;

    public static CommentResponseDto from(Comment comment) {
        return from(comment, null);
    }
    
    public static CommentResponseDto from(Comment comment, com.byeolnight.domain.entity.user.User currentUser) {
        // writer 정보 상세 체크
        String writerName;
        if (comment.getWriter() != null) {
            writerName = comment.getWriter().getNickname();
        } else {
            writerName = "알 수 없는 사용자";
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
        
        // 관리자는 원본 내용, 일반 사용자는 마스킹 내용
        String displayContent = comment.getContent();
        if (comment.getBlinded() && (currentUser == null || !"ADMIN".equals(currentUser.getRole().name()))) {
            displayContent = "이 댓글은 블라인드 처리되었습니다.";
        } else if (comment.getDeleted() && (currentUser == null || !"ADMIN".equals(currentUser.getRole().name()))) {
            displayContent = "이 댓글은 삭제되었습니다.";
        }
        
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(displayContent)
                .writer(writerName)
                .writerId(comment.getWriter() != null ? comment.getWriter().getId() : null)
                .blinded(comment.getBlinded())
                .deleted(comment.getDeleted())
                .createdAt(comment.getCreatedAt())
                .parentId(parentId)
                .parentWriter(parentWriter)
                .writerIcon(writerIcon)
                .writerCertificates(writerCertificates)
                .likeCount(comment.getLikeCount())
                .reportCount(comment.getReportCount())
                .isPopular(comment.isPopular())
                .build();
    }
}