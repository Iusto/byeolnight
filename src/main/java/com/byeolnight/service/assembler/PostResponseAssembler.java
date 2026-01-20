package com.byeolnight.service.assembler;

import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.entity.certificate.UserCertificate;
import com.byeolnight.entity.file.File;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.service.certificate.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Post -> PostResponseDto 변환을 담당하는 Assembler
 * - DTO에서 서비스 의존성을 제거하고 명시적인 의존성 주입 사용
 * - 배치 조회를 통한 N+1 문제 방지
 */
@Component
@RequiredArgsConstructor
public class PostResponseAssembler {

    private final CertificateService certificateService;

    /**
     * 단일 게시글 변환 (상세 조회용)
     */
    public PostResponseDto toDto(Post post, boolean likedByMe, long likeCount, boolean isHot, long commentCount) {
        return toDto(post, likedByMe, likeCount, isHot, commentCount, null);
    }

    /**
     * 단일 게시글 변환 (상세 조회용, 파일 포함)
     */
    public PostResponseDto toDto(Post post, boolean likedByMe, long likeCount, boolean isHot,
                                  long commentCount, List<File> files) {
        String writerName = getWriterName(post);
        Long writerId = post.getWriter() != null ? post.getWriter().getId() : null;

        List<PostResponseDto.FileDto> imageDtos = convertFiles(files);

        String writerIcon = null;
        List<String> writerCertificates = new ArrayList<>();

        if (post.getWriter() != null) {
            writerIcon = post.getWriter().getEquippedIconName();

            try {
                UserCertificate repCert = certificateService.getRepresentativeCertificate(post.getWriter());
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

    /**
     * 단일 게시글 변환 (목록 조회용, 간소화)
     */
    public PostResponseDto toDtoSimple(Post post, boolean isHot, long commentCount) {
        String writerName = getWriterName(post);
        Long writerId = post.getWriter() != null ? post.getWriter().getId() : null;

        String writerIcon = null;
        List<String> writerCertificates = new ArrayList<>();

        if (post.getWriter() != null) {
            writerIcon = post.getWriter().getEquippedIconName();

            try {
                UserCertificate repCert = certificateService.getRepresentativeCertificate(post.getWriter());
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

    /**
     * 게시글 목록 변환 (배치 조회로 N+1 방지)
     */
    public List<PostResponseDto> toDtoList(List<Post> posts, Map<Long, Long> likeCountMap,
                                            Map<Long, Long> commentCountMap, Set<Long> hotPostIds) {
        if (posts.isEmpty()) {
            return new ArrayList<>();
        }

        // 모든 작성자의 대표 인증서를 한 번에 조회
        Set<User> writers = posts.stream()
                .map(Post::getWriter)
                .filter(writer -> writer != null)
                .collect(Collectors.toSet());

        Map<Long, UserCertificate> certMap = certificateService.getRepresentativeCertificatesForUsers(writers);

        return posts.stream()
                .map(post -> toDtoWithCertMap(post, likeCountMap, commentCountMap, hotPostIds, certMap))
                .collect(Collectors.toList());
    }

    private PostResponseDto toDtoWithCertMap(Post post, Map<Long, Long> likeCountMap,
                                              Map<Long, Long> commentCountMap, Set<Long> hotPostIds,
                                              Map<Long, UserCertificate> certMap) {
        String writerName = getWriterName(post);
        Long writerId = post.getWriter() != null ? post.getWriter().getId() : null;

        String writerIcon = null;
        List<String> writerCertificates = new ArrayList<>();

        if (post.getWriter() != null) {
            writerIcon = post.getWriter().getEquippedIconName();

            UserCertificate repCert = certMap.get(post.getWriter().getId());
            if (repCert != null) {
                writerCertificates.add(repCert.getCertificateType().getName());
            }
        }

        long likeCount = likeCountMap.getOrDefault(post.getId(), (long) post.getLikeCount());
        long commentCount = commentCountMap.getOrDefault(post.getId(), 0L);
        boolean isHot = hotPostIds != null && hotPostIds.contains(post.getId());

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

    private String getWriterName(Post post) {
        return post.getWriter() != null ? post.getWriter().getNickname() : "알 수 없는 사용자";
    }

    private List<PostResponseDto.FileDto> convertFiles(List<File> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        return files.stream()
                .map(file -> new PostResponseDto.FileDto(file.getId(), file.getOriginalName(), file.getUrl()))
                .collect(Collectors.toList());
    }
}