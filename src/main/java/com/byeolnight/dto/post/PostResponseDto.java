package com.byeolnight.dto.post;

import com.byeolnight.domain.entity.post.Post;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 게시글 상세 응답 DTO
 * - 추천 수, 내가 추천했는지, 블라인드 여부 포함
 * - 리스트와 상세 조회에 따라 두 가지 팩토리 메서드 지원
 */
@Getter
@Builder
@AllArgsConstructor
public class PostResponseDto {

    private Long id;
    private String title;
    private String content;
    private String category;
    private String writer;
    private boolean blinded;
    private long likeCount;
    private boolean likedByMe;
    private boolean hot;
    private long viewCount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static PostResponseDto of(Post post, boolean likedByMe, long likeCount, boolean isHot) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .writer(post.getWriter().getNickname())
                .blinded(post.isBlinded())
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .hot(isHot)
                .viewCount(post.getViewCount())
                .updatedAt(post.getUpdatedAt())
                .createdAt(post.getCreatedAt())
                .build();
    }

    public static PostResponseDto from(Post post, boolean isHot) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .writer(post.getWriter().getNickname())
                .blinded(post.isBlinded())
                .likeCount(post.getLikeCount())
                .likedByMe(false)
                .hot(isHot)
                .viewCount(post.getViewCount())
                .updatedAt(post.getUpdatedAt())
                .createdAt(post.getCreatedAt())
                .build();
    }

    public static PostResponseDto from(Post post) {
        return from(post, false);
    }
}
