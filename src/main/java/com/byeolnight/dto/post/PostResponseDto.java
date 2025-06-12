package com.byeolnight.dto.post;

import com.byeolnight.domain.entity.post.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 게시글 상세 응답 DTO
 * - 추천 수, 내가 추천했는지, 블라인드 여부 포함
 * - 리스트와 상세 조회에 따라 두 가지 팩토리 메서드 지원
 */
@Getter
@Builder
@AllArgsConstructor
public class PostResponseDto {

    private Long id;                  // 게시글 ID
    private String title;             // 제목
    private String content;           // 본문
    private String category;          // 카테고리 (enum → 문자열)
    private String writer;            // 작성자 닉네임
    private boolean blinded;          // 블라인드 여부
    private long likeCount;           // 추천 수
    private boolean likedByMe;        // 현재 유저가 추천했는지 여부

    /**
     * 상세 조회용 팩토리 메서드
     * @param post Post 엔티티
     * @param likedByMe 현재 사용자가 추천했는지 여부
     * @param likeCount 총 추천 수
     * @return PostResponseDto
     */
    public static PostResponseDto of(Post post, boolean likedByMe, long likeCount) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .writer(post.getWriter().getNickname())
                .blinded(post.isBlinded())
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .build();
    }

    /**
     * 게시글 리스트용 요약 팩토리 메서드
     * - 추천 수, 추천 여부는 포함하지 않음
     * @param post Post 엔티티
     * @return PostResponseDto
     */
    public static PostResponseDto from(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .writer(post.getWriter().getNickname())
                .blinded(post.isBlinded())
                .likeCount(0)      // 기본값 (리스트용)
                .likedByMe(false)  // 기본값 (리스트용)
                .build();
    }
}
