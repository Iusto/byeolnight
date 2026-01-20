package com.byeolnight.dto.comment;

import com.byeolnight.entity.comment.Comment;
import com.byeolnight.entity.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private List<String> writerCertificates;
    private int likeCount;
    private int reportCount;
    private boolean isPopular;

    /**
     * @deprecated Use CommentResponseAssembler.toDto() instead for proper certificate loading
     */
    @Deprecated
    public static CommentResponseDto from(Comment comment) {
        return from(comment, null);
    }

    /**
     * @deprecated Use CommentResponseAssembler.toDto() instead for proper certificate loading
     */
    @Deprecated
    public static CommentResponseDto from(Comment comment, User currentUser) {
        String writerName = comment.getWriter() != null
                ? comment.getWriter().getNickname()
                : "알 수 없는 사용자";

        Long parentId = null;
        String parentWriter = null;
        if (comment.getParent() != null) {
            parentId = comment.getParent().getId();
            parentWriter = comment.getParent().getWriter() != null
                    ? comment.getParent().getWriter().getNickname()
                    : "알 수 없는 사용자";
        }

        String writerIcon = null;
        if (comment.getWriter() != null) {
            writerIcon = comment.getWriter().getEquippedIconName();
        }

        String displayContent = comment.getContent();
        boolean isAdmin = currentUser != null && "ADMIN".equals(currentUser.getRole().name());
        if (comment.getBlinded() && !isAdmin) {
            displayContent = "이 댓글은 블라인드 처리되었습니다.";
        } else if (comment.getDeleted() && !isAdmin) {
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
                .writerCertificates(new ArrayList<>())
                .likeCount(comment.getLikeCount())
                .reportCount(comment.getReportCount())
                .isPopular(comment.isPopular())
                .build();
    }

    /**
     * @deprecated Use CommentResponseAssembler.toDto() with forAdmin=true instead
     */
    @Deprecated
    public static CommentResponseDto fromForAdmin(Comment comment, User currentUser) {
        String writerName = comment.getWriter() != null
                ? comment.getWriter().getNickname()
                : "알 수 없는 사용자";

        Long parentId = null;
        String parentWriter = null;
        if (comment.getParent() != null) {
            parentId = comment.getParent().getId();
            parentWriter = comment.getParent().getWriter() != null
                    ? comment.getParent().getWriter().getNickname()
                    : "알 수 없는 사용자";
        }

        String writerIcon = null;
        if (comment.getWriter() != null) {
            writerIcon = comment.getWriter().getEquippedIconName();
        }

        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .writer(writerName)
                .writerId(comment.getWriter() != null ? comment.getWriter().getId() : null)
                .blinded(comment.getBlinded())
                .deleted(comment.getDeleted())
                .createdAt(comment.getCreatedAt())
                .parentId(parentId)
                .parentWriter(parentWriter)
                .writerIcon(writerIcon)
                .writerCertificates(new ArrayList<>())
                .likeCount(comment.getLikeCount())
                .reportCount(comment.getReportCount())
                .isPopular(comment.isPopular())
                .build();
    }
}
