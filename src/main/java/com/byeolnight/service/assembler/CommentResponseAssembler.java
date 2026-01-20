package com.byeolnight.service.assembler;

import com.byeolnight.dto.comment.CommentResponseDto;
import com.byeolnight.entity.certificate.UserCertificate;
import com.byeolnight.entity.comment.Comment;
import com.byeolnight.entity.user.User;
import com.byeolnight.service.certificate.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Comment -> CommentResponseDto 변환을 담당하는 Assembler
 * - DTO에서 서비스 의존성을 제거하고 명시적인 의존성 주입 사용
 * - 배치 조회를 통한 N+1 문제 방지
 */
@Component
@RequiredArgsConstructor
public class CommentResponseAssembler {

    private final CertificateService certificateService;

    /**
     * 단일 댓글 변환
     */
    public CommentResponseDto toDto(Comment comment, User currentUser) {
        return toDto(comment, currentUser, false);
    }

    /**
     * 단일 댓글 변환 (관리자 모드)
     */
    public CommentResponseDto toDto(Comment comment, User currentUser, boolean forAdmin) {
        String writerName = getWriterName(comment);
        Long parentId = null;
        String parentWriter = null;

        if (comment.getParent() != null) {
            parentId = comment.getParent().getId();
            parentWriter = getWriterName(comment.getParent());
        }

        String writerIcon = null;
        List<String> writerCertificates = new ArrayList<>();

        if (comment.getWriter() != null) {
            writerIcon = comment.getWriter().getEquippedIconName();

            try {
                UserCertificate repCert = certificateService.getRepresentativeCertificate(comment.getWriter());
                if (repCert != null) {
                    writerCertificates.add(repCert.getCertificateType().getName());
                }
            } catch (Exception e) {
                // 인증서 조회 실패 시 무시
            }
        }

        String displayContent = getDisplayContent(comment, currentUser, forAdmin);

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

    /**
     * 댓글 목록 변환 (배치 조회로 N+1 방지)
     */
    public List<CommentResponseDto> toDtoList(List<Comment> comments, User currentUser) {
        return toDtoList(comments, currentUser, false);
    }

    /**
     * 댓글 목록 변환 (관리자 모드, 배치 조회로 N+1 방지)
     */
    public List<CommentResponseDto> toDtoList(List<Comment> comments, User currentUser, boolean forAdmin) {
        if (comments.isEmpty()) {
            return new ArrayList<>();
        }

        // 모든 작성자의 대표 인증서를 한 번에 조회
        Set<User> writers = comments.stream()
                .map(Comment::getWriter)
                .filter(writer -> writer != null)
                .collect(Collectors.toSet());

        Map<Long, UserCertificate> certMap = certificateService.getRepresentativeCertificatesForUsers(writers);

        return comments.stream()
                .map(comment -> toDtoWithCertMap(comment, currentUser, forAdmin, certMap))
                .collect(Collectors.toList());
    }

    private CommentResponseDto toDtoWithCertMap(Comment comment, User currentUser, boolean forAdmin,
                                                 Map<Long, UserCertificate> certMap) {
        String writerName = getWriterName(comment);
        Long parentId = null;
        String parentWriter = null;

        if (comment.getParent() != null) {
            parentId = comment.getParent().getId();
            parentWriter = getWriterName(comment.getParent());
        }

        String writerIcon = null;
        List<String> writerCertificates = new ArrayList<>();

        if (comment.getWriter() != null) {
            writerIcon = comment.getWriter().getEquippedIconName();

            UserCertificate repCert = certMap.get(comment.getWriter().getId());
            if (repCert != null) {
                writerCertificates.add(repCert.getCertificateType().getName());
            }
        }

        String displayContent = getDisplayContent(comment, currentUser, forAdmin);

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

    private String getWriterName(Comment comment) {
        return comment.getWriter() != null ? comment.getWriter().getNickname() : "알 수 없는 사용자";
    }

    private String getDisplayContent(Comment comment, User currentUser, boolean forAdmin) {
        if (forAdmin) {
            return comment.getContent();
        }

        boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.getRole().name());

        if (comment.getBlinded() && !isAdmin) {
            return "이 댓글은 블라인드 처리되었습니다.";
        }
        if (comment.getDeleted() && !isAdmin) {
            return "이 댓글은 삭제되었습니다.";
        }
        return comment.getContent();
    }
}