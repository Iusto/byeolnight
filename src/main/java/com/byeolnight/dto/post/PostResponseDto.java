package com.byeolnight.dto.post;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 상세 응답 DTO
 * - 추천 수, 내가 추천했는지, 블라인드 여부 포함
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

}
