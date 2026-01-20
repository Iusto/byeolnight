package com.byeolnight.dto.post;

import com.byeolnight.entity.file.File;
import com.byeolnight.entity.post.Post;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    private Long writerId;
    private boolean blinded;
    private String blindType;
    private long likeCount;
    private boolean likedByMe;
    private boolean hot;
    private long viewCount;
    private long commentCount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "Asia/Seoul")
    private LocalDateTime updatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;
    private List<FileDto> images;
    private String writerIcon;
    private List<String> writerCertificates;

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class FileDto {
        private Long id;
        private String originalName;
        private String url;
    }

    /**
     * @deprecated Use PostResponseAssembler.toDto() instead for proper certificate loading
     */
    @Deprecated
    public static PostResponseDto of(Post post, boolean likedByMe, long likeCount, boolean isHot, long commentCount, List<File> files) {
        String writerName = post.getWriter() != null ? post.getWriter().getNickname() : "알 수 없는 사용자";
        Long writerId = post.getWriter() != null ? post.getWriter().getId() : null;

        List<FileDto> imageDtos = files != null ? files.stream()
                .map(file -> new FileDto(file.getId(), file.getOriginalName(), file.getUrl()))
                .collect(Collectors.toList()) : Collections.emptyList();

        String writerIcon = null;
        if (post.getWriter() != null) {
            writerIcon = post.getWriter().getEquippedIconName();
        }

        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .writer(writerName)
                .writerId(writerId)
                .blinded(post.isBlinded())
                .blindType(post.getBlindType() != null ? post.getBlindType().name() : null)
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .hot(isHot)
                .viewCount(post.getViewCount())
                .commentCount(commentCount)
                .updatedAt(post.getUpdatedAt())
                .createdAt(post.getCreatedAt())
                .images(imageDtos)
                .writerIcon(writerIcon)
                .writerCertificates(new ArrayList<>())
                .build();
    }

    /**
     * @deprecated Use PostResponseAssembler.toDto() instead for proper certificate loading
     */
    @Deprecated
    public static PostResponseDto of(Post post, boolean likedByMe, long likeCount, boolean isHot, long commentCount) {
        return of(post, likedByMe, likeCount, isHot, commentCount, null);
    }

    /**
     * @deprecated Use PostResponseAssembler.toDtoSimple() instead for proper certificate loading
     */
    @Deprecated
    public static PostResponseDto from(Post post, boolean isHot, long commentCount) {
        String writerName = post.getWriter() != null ? post.getWriter().getNickname() : "알 수 없는 사용자";
        Long writerId = post.getWriter() != null ? post.getWriter().getId() : null;

        String writerIcon = null;
        if (post.getWriter() != null) {
            writerIcon = post.getWriter().getEquippedIconName();
        }

        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .writer(writerName)
                .writerId(writerId)
                .blinded(post.isBlinded())
                .blindType(post.getBlindType() != null ? post.getBlindType().name() : null)
                .likeCount(post.getLikeCount())
                .likedByMe(false)
                .hot(isHot)
                .viewCount(post.getViewCount())
                .commentCount(commentCount)
                .updatedAt(post.getUpdatedAt())
                .createdAt(post.getCreatedAt())
                .writerIcon(writerIcon)
                .writerCertificates(new ArrayList<>())
                .build();
    }

    /**
     * @deprecated Use PostResponseAssembler.toDtoSimple() instead for proper certificate loading
     */
    @Deprecated
    public static PostResponseDto from(Post post) {
        return from(post, false, 0);
    }
}
