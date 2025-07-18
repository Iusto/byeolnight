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
    private Long writerId;
    private boolean blinded;
    private String blindType; // ADMIN_BLIND 또는 REPORT_BLIND
    private long likeCount;
    private boolean likedByMe;
    private boolean hot;
    private long viewCount;
    private long commentCount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime updatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;
    private java.util.List<FileDto> images;
    private String writerIcon;
    private java.util.List<String> writerCertificates;
    
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class FileDto {
        private Long id;
        private String originalName;
        private String url;
    }

    public static PostResponseDto of(Post post, boolean likedByMe, long likeCount, boolean isHot, long commentCount, java.util.List<com.byeolnight.domain.entity.file.File> files) {
        // writer null 체크
        String writerName = (post.getWriter() != null) ? post.getWriter().getNickname() : "알 수 없는 사용자";
        Long writerId = (post.getWriter() != null) ? post.getWriter().getId() : null;
        
        // 이미지 목록 변환
        java.util.List<FileDto> imageDtos = files != null ? files.stream()
                .map(file -> new FileDto(file.getId(), file.getOriginalName(), file.getUrl()))
                .collect(java.util.stream.Collectors.toList()) : java.util.Collections.emptyList();
        
        // 사용자 아이콘 및 인증서 정보
        String writerIcon = null;
        java.util.List<String> writerCertificates = new java.util.ArrayList<>();
        
        if (post.getWriter() != null) {
            writerIcon = post.getWriter().getEquippedIconName();
            
            // 대표 인증서 조회
            try {
                com.byeolnight.service.certificate.CertificateService certificateService = 
                    com.byeolnight.infrastructure.config.ApplicationContextProvider
                        .getBean(com.byeolnight.service.certificate.CertificateService.class);
                com.byeolnight.domain.entity.certificate.UserCertificate repCert = 
                    certificateService.getRepresentativeCertificate(post.getWriter());
                if (repCert != null) {
                    writerCertificates.add(repCert.getCertificateType().getName());
                }
            } catch (Exception e) {
                // 인증서 조회 실패 시 무시
            }
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
                .writerCertificates(writerCertificates)
                .build();
    }
    
    public static PostResponseDto of(Post post, boolean likedByMe, long likeCount, boolean isHot, long commentCount) {
        return of(post, likedByMe, likeCount, isHot, commentCount, null);
    }

    public static PostResponseDto from(Post post, boolean isHot, long commentCount) {
        // writer null 체크
        String writerName = (post.getWriter() != null) ? post.getWriter().getNickname() : "알 수 없는 사용자";
        Long writerId = (post.getWriter() != null) ? post.getWriter().getId() : null;
        
        // 사용자 아이콘 및 인증서 정보
        String writerIcon = null;
        java.util.List<String> writerCertificates = new java.util.ArrayList<>();
        
        if (post.getWriter() != null) {
            writerIcon = post.getWriter().getEquippedIconName();
            
            // 대표 인증서 조회
            try {
                com.byeolnight.service.certificate.CertificateService certificateService = 
                    com.byeolnight.infrastructure.config.ApplicationContextProvider
                        .getBean(com.byeolnight.service.certificate.CertificateService.class);
                com.byeolnight.domain.entity.certificate.UserCertificate repCert = 
                    certificateService.getRepresentativeCertificate(post.getWriter());
                if (repCert != null) {
                    writerCertificates.add(repCert.getCertificateType().getName());
                }
            } catch (Exception e) {
                // 인증서 조회 실패 시 무시
            }
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
                .writerCertificates(writerCertificates)
                .build();
    }

    public static PostResponseDto from(Post post) {
        return from(post, false, 0);
    }
}
