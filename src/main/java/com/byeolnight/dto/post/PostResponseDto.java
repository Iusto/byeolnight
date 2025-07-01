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
    private long commentCount; // 댓글 수
    private String dDay; // D-Day 표시용
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static PostResponseDto of(Post post, boolean likedByMe, long likeCount, boolean isHot, long commentCount) {
        // writer null 체크
        String writerName = (post.getWriter() != null) ? post.getWriter().getNickname() : "알 수 없는 사용자";
        
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .writer(writerName)
                .blinded(post.isBlinded())
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .hot(isHot)
                .viewCount(post.getViewCount())
                .commentCount(commentCount)
                .dDay(calculateDDay(post))
                .updatedAt(post.getUpdatedAt())
                .createdAt(post.getCreatedAt())
                .build();
    }

    public static PostResponseDto from(Post post, boolean isHot, long commentCount) {
        // writer null 체크
        String writerName = (post.getWriter() != null) ? post.getWriter().getNickname() : "알 수 없는 사용자";
        
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .writer(writerName)
                .blinded(post.isBlinded())
                .likeCount(post.getLikeCount())
                .likedByMe(false)
                .hot(isHot)
                .viewCount(post.getViewCount())
                .commentCount(commentCount)
                .dDay(calculateDDay(post))
                .updatedAt(post.getUpdatedAt())
                .createdAt(post.getCreatedAt())
                .build();
    }

    public static PostResponseDto from(Post post) {
        return from(post, false, 0);
    }

    private static String calculateDDay(Post post) {
        // EVENT 카테고리이고 제목에 날짜 정보가 있는 경우만 D-Day 계산
        if (post.getCategory() != Post.Category.EVENT) {
            return null;
        }
        
        try {
            // 콘텐츠에서 날짜 추출 (예: "2024-12-21" 형식)
            String content = post.getContent();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\*\\*\ub2e4\uc74c \uacac\ud559\uc77c\\*\\*: (\\d{4}-\\d{2}-\\d{2})");
            java.util.regex.Matcher matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                String dateStr = matcher.group(1);
                java.time.LocalDate eventDate = java.time.LocalDate.parse(dateStr);
                java.time.LocalDate today = java.time.LocalDate.now();
                
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(today, eventDate);
                
                if (daysBetween > 0) {
                    return "D-" + daysBetween;
                } else if (daysBetween == 0) {
                    return "D-Day";
                } else {
                    return "종료";
                }
            }
        } catch (Exception e) {
            // 날짜 파싱 실패 시 null 반환
        }
        
        return null;
    }
}
