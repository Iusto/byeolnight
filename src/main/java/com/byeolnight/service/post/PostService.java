package com.byeolnight.service.post;

import com.byeolnight.domain.entity.file.File;
import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.Post.Category;
import com.byeolnight.domain.entity.post.PostLike;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.post.FileRepository;
import com.byeolnight.domain.repository.post.PostRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.domain.repository.post.PostLikeRepository;
import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.service.file.S3Service;
import com.byeolnight.service.user.PointService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final com.byeolnight.service.certificate.CertificateService certificateService;
    private final PointService pointService;
    private final com.byeolnight.domain.repository.CommentRepository commentRepository;
    private final com.byeolnight.service.ImageModerationService imageModerationService;

    @Transactional
    public Long createPost(PostRequestDto dto, User user) {
        validateAdminCategoryWrite(dto.getCategory(), user);

        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory())
                .writer(user)
                .build();
        postRepository.save(post);

        // 이미지 검증 및 저장
        dto.getImages().forEach(image -> {
            System.out.println("이미지 검증 완료: " + image.originalName());
            File file = File.of(post, image.originalName(), image.s3Key(), image.url());
            fileRepository.save(file);
        });

        // 게시글 작성 인증서 발급 체크
        certificateService.checkAndIssueCertificates(user, com.byeolnight.service.certificate.CertificateService.CertificateCheckType.POST_WRITE);
        
        // IMAGE 카테고리 게시글 작성 시 별 관측 매니아 인증서 체크
        if (dto.getCategory() == Post.Category.IMAGE) {
            certificateService.checkAndIssueCertificates(user, com.byeolnight.service.certificate.CertificateService.CertificateCheckType.IMAGE_VIEW);
        }

        // 게시글 작성 포인트 지급
        pointService.awardPostWritePoints(user, post.getId(), dto.getContent());

        return post.getId();
    }

    @Transactional
    public void updatePost(Long postId, PostRequestDto dto, User user) {
        Post post = getPostOrThrow(postId);

        if (!post.getWriter().equals(user)) {
            throw new IllegalArgumentException("본인이 작성한 글만 수정할 수 있습니다.");
        }

        validateAdminCategoryWrite(dto.getCategory(), user);

        post.update(dto.getTitle(), dto.getContent(), dto.getCategory());

        // S3 이미지 삭제 + DB 삭제
        List<File> oldFiles = fileRepository.findAllByPost(post);
        oldFiles.forEach(file -> s3Service.deleteObject(file.getS3Key()));
        fileRepository.deleteAllByPost(post);

        // 새 이미지 저장
        dto.getImages().forEach(image -> {
            File file = File.of(post, image.originalName(), image.s3Key(), image.url());
            fileRepository.save(file);
        });
    }

    @Transactional
    public PostResponseDto getPostById(Long postId, User currentUser) {
        if (postId == null || postId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 게시글 ID입니다.");
        }
        
        Post post = postRepository.findWithWriterById(postId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글입니다."));

        if (post.isDeleted() || post.isBlinded()) {
            throw new NotFoundException("삭제되었거나 블라인드 처리된 게시글입니다.");
        }

        post.increaseViewCount();

        // 비로그인 사용자도 게시글 조회 가능
        boolean likedByMe = currentUser != null && postLikeRepository.existsByUserAndPost(currentUser, post);
        long likeCount = postLikeRepository.countByPost(post);
        long commentCount = commentRepository.countByPostId(postId);

        return PostResponseDto.of(post, likedByMe, likeCount, false, commentCount);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getFilteredPosts(String category, String sortParam, String searchType, String search, Pageable pageable) {
        System.out.println("게시글 조회 요청 - 카테고리: " + category + ", 정렬: " + sortParam + ", 검색타입: " + searchType + ", 검색어: " + search);
        
        // 검색 기능이 있는 경우
        if (search != null && !search.trim().isEmpty()) {
            System.out.println("검색 모드로 전환");
            return searchPosts(category, searchType, search.trim(), pageable);
        }
        
        System.out.println("일반 조회 모드");
        return getFilteredPosts(category, sortParam, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getFilteredPosts(String category, String sortParam, Pageable pageable) {
        Category categoryEnum = parseCategory(category);
        if (categoryEnum == null) throw new IllegalArgumentException("카테고리 누락");

        Post.SortType sort = Post.SortType.from(sortParam);
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);

        switch (sort) {
            case RECENT -> {
                // ✅ 한 달 이내 추천수 5 이상인 게시글 중 추천순으로 4개 조회 (HOT)
                List<Post> hotPosts = postRepository.findTopHotPostsByCategory(categoryEnum, threshold, PageRequest.of(0, 4));
                Page<Post> recentPosts = postRepository.findByIsDeletedFalseAndBlindedFalseAndCategoryOrderByCreatedAtDesc(categoryEnum, pageable);

                Set<Long> hotIds = hotPosts.stream().map(Post::getId).collect(Collectors.toSet());

                List<PostResponseDto> combined = new ArrayList<>();

                // HOT 게시글은 HOT 플래그 true (안전 처리)
                hotPosts.forEach(p -> {
                    try {
                        long actualLikeCount = postLikeRepository.countByPost(p);
                        long commentCount = commentRepository.countByPostId(p.getId());
                        combined.add(PostResponseDto.of(p, false, actualLikeCount, true, commentCount));
                    } catch (Exception e) {
                        System.err.println("게시글 처리 실패 (HOT): " + p.getId() + ", 오류: " + e.getMessage());
                    }
                });

                // 나머지 일반 게시글은 HOT 플래그 false (안전 처리)
                recentPosts.getContent().stream()
                        .filter(p -> !hotIds.contains(p.getId()))
                        .forEach(p -> {
                            try {
                                long actualLikeCount = postLikeRepository.countByPost(p);
                                long commentCount = commentRepository.countByPostId(p.getId());
                                combined.add(PostResponseDto.of(p, false, actualLikeCount, false, commentCount));
                            } catch (Exception e) {
                                System.err.println("게시글 처리 실패 (RECENT): " + p.getId() + ", 오류: " + e.getMessage());
                            }
                        });

                return new PageImpl<>(combined, pageable, combined.size());
            }

            case POPULAR -> {
                // ✅ 추천순 정렬: 날짜 무관, HOT 여부 무관, 추천 수 높은 게시글 30개
                Page<Post> popularPosts = postRepository.findByIsDeletedFalseAndBlindedFalseAndCategoryOrderByLikeCountDesc(categoryEnum, pageable);
                List<PostResponseDto> dtos = popularPosts.getContent().stream()
                        .map(p -> {
                            long actualLikeCount = postLikeRepository.countByPost(p);
                            long commentCount = commentRepository.countByPostId(p.getId());
                            return PostResponseDto.of(p, false, actualLikeCount, false, commentCount);
                        })
                        .toList();

                return new PageImpl<>(dtos, pageable, popularPosts.getTotalElements());
            }
        }

        throw new IllegalArgumentException("지원하지 않는 정렬 방식입니다.");
    }
    
    @Transactional(readOnly = true)
    public Page<PostResponseDto> searchPosts(String category, String searchType, String keyword, Pageable pageable) {
        Category categoryEnum = parseCategory(category);
        if (categoryEnum == null) throw new IllegalArgumentException("카테고리 누락");
        
        System.out.println("검색 실행: 카테고리=" + categoryEnum + ", 타입=" + searchType + ", 키워드=" + keyword);
        
        Page<Post> searchResults;
        
        switch (searchType) {
            case "title" -> searchResults = postRepository.findByTitleContainingAndCategoryAndIsDeletedFalseAndBlindedFalse(keyword, categoryEnum, pageable);
            case "content" -> searchResults = postRepository.findByContentContainingAndCategoryAndIsDeletedFalseAndBlindedFalse(keyword, categoryEnum, pageable);
            case "titleAndContent" -> searchResults = postRepository.findByTitleOrContentContainingAndCategory(keyword, categoryEnum, pageable);
            case "writer" -> searchResults = postRepository.findByWriterNicknameContainingAndCategory(keyword, categoryEnum, pageable);
            default -> throw new IllegalArgumentException("지원하지 않는 검색 타입입니다.");
        }
        
        System.out.println("검색 결과 수: " + searchResults.getTotalElements());
        
        List<PostResponseDto> dtos = searchResults.getContent().stream()
                .map(p -> {
                    try {
                        long actualLikeCount = postLikeRepository.countByPost(p);
                        long commentCount = commentRepository.countByPostId(p.getId());
                        return PostResponseDto.of(p, false, actualLikeCount, false, commentCount);
                    } catch (Exception e) {
                        System.err.println("검색 결과 처리 실패: " + p.getId() + ", 오류: " + e.getMessage());
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, searchResults.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getTopHotPostsAcrossAllCategories(int size) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        Pageable pageable = PageRequest.of(0, size);

        List<Post> hotPosts = postRepository.findTopHotPostsAcrossAllCategories(threshold, pageable);

        return hotPosts.stream()
                .map(p -> {
                    // 실제 PostLike 테이블에서 추천수 계산
                    long actualLikeCount = postLikeRepository.countByPost(p);
                    long commentCount = commentRepository.countByPostId(p.getId());
                    return PostResponseDto.of(p, false, actualLikeCount, true, commentCount);
                })
                .toList();
    }

    @Transactional
    public void deletePost(Long postId, User user) {
        Post post = getPostOrThrow(postId);

        if (!post.getWriter().equals(user)) {
            throw new IllegalArgumentException("본인이 작성한 글만 삭제할 수 있습니다.");
        }

        validateAdminCategoryWrite(post.getCategory(), user);

        // 이미지 S3 삭제 + DB 삭제
        List<File> files = fileRepository.findAllByPost(post);
        files.forEach(file -> s3Service.deleteObject(file.getS3Key()));
        fileRepository.deleteAllByPost(post);

        post.softDelete();
    }

    @Transactional
    public void likePost(Long userId, Long postId) {
        Post post = getPostOrThrow(postId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (postLikeRepository.existsByUserAndPost(user, post)) {
            throw new IllegalArgumentException("이미 추천한 글입니다.");
        }

        PostLike like = PostLike.of(user, post);
        postLikeRepository.save(like);
        
        // Post 엔티티의 likeCount 필드 업데이트
        post.increaseLikeCount();
        postRepository.save(post);

        // 포인트 지급
        pointService.awardGiveLikePoints(user, postId.toString()); // 추천하는 사용자에게
        pointService.awardReceiveLikePoints(post.getWriter(), postId.toString()); // 글 작성자에게
    }

    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("해당 게시글을 찾을 수 없습니다."));
    }

    private Category parseCategory(String category) {
        if (category == null || category.isBlank()) return null;
        try {
            return Category.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 카테고리입니다. (NEWS, DISCUSSION, IMAGE, EVENT, REVIEW, FREE, NOTICE 중 선택)");
        }
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getBlindedPosts() {
        return postRepository.findByIsDeletedFalseAndBlindedTrueOrderByCreatedAtDesc().stream()
                .map(p -> {
                    long commentCount = commentRepository.countByPostId(p.getId());
                    return PostResponseDto.from(p, false, commentCount);
                })
                .toList();
    }

    @Transactional
    public void blindPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        post.blind();
        
        // 규정 위반 페널티 적용
        pointService.applyPenalty(post.getWriter(), "게시글 블라인드 처리", postId.toString());
    }

    @Transactional
    public void unblindPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        post.unblind();
    }

    @Transactional
    public void deletePostPermanently(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        
        // 규정 위반 페널티 적용 (삭제 전에)
        pointService.applyPenalty(post.getWriter(), "게시글 삭제", postId.toString());
        
        // 이미지 S3 삭제 + DB 삭제
        List<File> files = fileRepository.findAllByPost(post);
        files.forEach(file -> s3Service.deleteObject(file.getS3Key()));
        fileRepository.deleteAllByPost(post);
        
        // 게시글 완전 삭제
        postRepository.delete(post);
    }

    private void validateAdminCategoryWrite(Post.Category category, User user) {
        if ((category == Post.Category.NEWS || category == Post.Category.EVENT || category == Post.Category.NOTICE)
                && user.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("해당 카테고리의 게시글은 관리자만 작성, 수정, 삭제할 수 있습니다.");
        }
    }

    @Transactional
    public void createNewsPost(String title, String content, Post.Category category, User writer) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .category(category)
                .writer(writer)
                .build();
        // createdAt, updatedAt은 @CreationTimestamp/@UpdateTimestamp로 자동 설정
        postRepository.save(post);
    }
    
    @Transactional
    public void createEventPost(String title, String content, User writer) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .category(Post.Category.EVENT)
                .writer(writer)
                .build();
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getReportedPosts() {
        return postRepository.findReportedPosts().stream()
                .map(p -> {
                    long commentCount = commentRepository.countByPostId(p.getId());
                    return PostResponseDto.from(p, false, commentCount);
                })
                .toList();
    }
}
