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
    private Boolean blinded;
    private LocalDateTime createdAt;
    private Long parentId; // 부모 댓글 ID
    private String parentWriter; // 부모 댓글 작성자

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
        
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getBlinded() ? "[블라인드 처리된 댓글입니다]" : comment.getContent())
                .writer(writerName)
                .blinded(comment.getBlinded())
                .createdAt(comment.getCreatedAt())
                .parentId(parentId)
                .parentWriter(parentWriter)
                .build();
    }
}