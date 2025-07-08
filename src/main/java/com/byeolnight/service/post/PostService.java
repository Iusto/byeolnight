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
import com.byeolnight.dto.post.PostDto;
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
    private final com.byeolnight.domain.repository.post.PostReportRepository postReportRepository;
    private final S3Service s3Service;
    private final com.byeolnight.service.certificate.CertificateService certificateService;
    private final PointService pointService;
    private final com.byeolnight.domain.repository.CommentRepository commentRepository;
    private final com.byeolnight.service.notification.NotificationService notificationService;

    @Transactional
    public Long createPost(PostRequestDto dto, User user) {
        validateAdminCategoryWrite(dto.getCategory(), user);

        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory())
                .writer(user)
                .build();
        
        if (dto.getOriginTopicId() != null) {
            Post originTopic = postRepository.findById(dto.getOriginTopicId())
                    .orElseThrow(() -> new NotFoundException("원본 토론 주제를 찾을 수 없습니다."));
            if (!originTopic.isDiscussionTopic()) {
                throw new IllegalArgumentException("유효하지 않은 토론 주제입니다.");
            }
            post.setOriginTopicId(dto.getOriginTopicId());
        }
        
        postRepository.save(post);

        dto.getImages().forEach(image -> {
            File file = File.of(post, image.originalName(), image.s3Key(), image.url());
            fileRepository.save(file);
        });

        certificateService.checkAndIssueCertificates(user, com.byeolnight.service.certificate.CertificateService.CertificateCheckType.POST_WRITE);
        
        if (dto.getCategory() == Post.Category.IMAGE) {
            certificateService.checkAndIssueCertificates(user, com.byeolnight.service.certificate.CertificateService.CertificateCheckType.IMAGE_UPLOAD);
        }

        pointService.awardPostWritePoints(user, post.getId(), dto.getContent());
        
        // 포인트 달성 인증서 체크
        try {
            certificateService.checkAndIssueCertificates(user, com.byeolnight.service.certificate.CertificateService.CertificateCheckType.POINT_ACHIEVEMENT);
        } catch (Exception e) {
            System.err.println("포인트 인증서 발급 실패: " + e.getMessage());
        }
        
        if (dto.getCategory() == Post.Category.NOTICE) {
            try {
                notificationService.createNotificationForAllUsers(
                    com.byeolnight.domain.entity.Notification.NotificationType.NEW_NOTICE,
                    "새 공지사항이 등록되었습니다",
                    dto.getTitle(),
                    "/posts/" + post.getId(),
                    post.getId()
                );
            } catch (Exception e) {
                System.err.println("공지사항 알림 전송 실패: " + e.getMessage());
            }
        }

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

        List<File> oldFiles = fileRepository.findAllByPost(post);
        oldFiles.forEach(file -> s3Service.deleteObject(file.getS3Key()));
        fileRepository.deleteAllByPost(post);

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

        boolean likedByMe = currentUser != null && postLikeRepository.existsByUserAndPost(currentUser, post);
        long likeCount = postLikeRepository.countByPost(post);
        long commentCount = commentRepository.countByPostId(postId);

        return PostResponseDto.of(post, likedByMe, likeCount, false, commentCount);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getFilteredPosts(String category, String sortParam, String searchType, String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return searchPosts(category, searchType, search.trim(), pageable);
        }
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
                List<Post> hotPosts = postRepository.findTopHotPostsByCategory(categoryEnum, threshold, PageRequest.of(0, 4));
                Page<Post> recentPosts = postRepository.findByIsDeletedFalseAndCategoryOrderByCreatedAtDesc(categoryEnum, pageable);

                Set<Long> hotIds = hotPosts.stream().map(Post::getId).collect(Collectors.toSet());
                List<PostResponseDto> combined = new ArrayList<>();

                hotPosts.forEach(p -> {
                    try {
                        long actualLikeCount = postLikeRepository.countByPost(p);
                        long commentCount = commentRepository.countByPostId(p.getId());
                        combined.add(PostResponseDto.of(p, false, actualLikeCount, true, commentCount));
                    } catch (Exception e) {
                        System.err.println("게시글 처리 실패 (HOT): " + p.getId());
                    }
                });

                recentPosts.getContent().stream()
                        .filter(p -> !hotIds.contains(p.getId()))
                        .forEach(p -> {
                            try {
                                long actualLikeCount = postLikeRepository.countByPost(p);
                                long commentCount = commentRepository.countByPostId(p.getId());
                                combined.add(PostResponseDto.of(p, false, actualLikeCount, false, commentCount));
                            } catch (Exception e) {
                                System.err.println("게시글 처리 실패 (RECENT): " + p.getId());
                            }
                        });

                return new PageImpl<>(combined, pageable, combined.size());
            }

            case POPULAR -> {
                Page<Post> popularPosts = postRepository.findByIsDeletedFalseAndCategoryOrderByLikeCountDesc(categoryEnum, pageable);
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
        
        Page<Post> searchResults;
        
        switch (searchType) {
            case "title" -> searchResults = postRepository.findByTitleContainingAndCategoryAndIsDeletedFalse(keyword, categoryEnum, pageable);
            case "content" -> searchResults = postRepository.findByContentContainingAndCategoryAndIsDeletedFalse(keyword, categoryEnum, pageable);
            case "titleAndContent" -> searchResults = postRepository.findByTitleOrContentContainingAndCategoryAndIsDeletedFalse(keyword, categoryEnum, pageable);
            case "writer" -> searchResults = postRepository.findByWriterNicknameContainingAndCategoryAndIsDeletedFalse(keyword, categoryEnum, pageable);
            default -> throw new IllegalArgumentException("지원하지 않는 검색 타입입니다.");
        }
        
        List<PostResponseDto> dtos = searchResults.getContent().stream()
                .map(p -> {
                    try {
                        long actualLikeCount = postLikeRepository.countByPost(p);
                        long commentCount = commentRepository.countByPostId(p.getId());
                        return PostResponseDto.of(p, false, actualLikeCount, false, commentCount);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
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
        
        post.increaseLikeCount();
        postRepository.save(post);

        pointService.awardGiveLikePoints(user, postId.toString());
        pointService.awardReceiveLikePoints(post.getWriter(), postId.toString());
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
            throw new IllegalArgumentException("잘못된 카테고리입니다.");
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
        pointService.applyPenalty(post.getWriter(), "게시글 블라인드 처리", postId.toString());
    }

    @Transactional
    public void blindPostByAdmin(Long postId, Long adminId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        post.blindByAdmin(adminId);
        postRepository.save(post);
        System.out.println("관리자 블라인드 처리: postId=" + postId + ", blindType=" + post.getBlindType());
        pointService.applyPenalty(post.getWriter(), "관리자 블라인드 처리", postId.toString());
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
        
        pointService.applyPenalty(post.getWriter(), "게시글 삭제", postId.toString());
        
        List<File> files = fileRepository.findAllByPost(post);
        files.forEach(file -> s3Service.deleteObject(file.getS3Key()));
        fileRepository.deleteAllByPost(post);
        
        postRepository.delete(post);
    }

    private void validateAdminCategoryWrite(Post.Category category, User user) {
        if ((category == Post.Category.NEWS || category == Post.Category.NOTICE)
                && user.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("해당 카테고리의 게시글은 관리자만 작성할 수 있습니다.");
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
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public List<com.byeolnight.dto.admin.ReportedPostDetailDto> getReportedPosts() {
        return postRepository.findReportedPosts().stream()
                .map(p -> {
                    long reportCount = postReportRepository.countByPost(p);
                    List<String> reportReasons = postReportRepository.findReasonsByPost(p);
                    List<com.byeolnight.domain.entity.post.PostReport> reportDetails = postReportRepository.findDetailsByPost(p);
                    return com.byeolnight.dto.admin.ReportedPostDetailDto.of(p, reportCount, reportReasons, reportDetails);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostDto.Response> getMyPosts(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        
        Page<Post> posts = postRepository.findByWriterAndIsDeletedFalseOrderByCreatedAtDesc(user, pageable);
        
        return posts.getContent().stream()
                .map(post -> {
                    long likeCount = postLikeRepository.countByPost(post);
                    long commentCount = commentRepository.countByPostId(post.getId());
                    return PostDto.Response.from(post, likeCount, commentCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.byeolnight.dto.comment.CommentAdminDto> getBlindedComments() {
        return commentRepository.findByBlindedTrueOrderByCreatedAtDesc().stream()
                .map(comment -> com.byeolnight.dto.comment.CommentAdminDto.builder()
                        .id(comment.getId())
                        .content(comment.getContent())
                        .originalContent(comment.getOriginalContent())
                        .writer(comment.getWriter().getNickname())
                        .postTitle(comment.getPost().getTitle())
                        .postId(comment.getPost().getId())
                        .blinded(comment.isBlinded())
                        .deleted(comment.isDeleted())
                        .createdAt(comment.getCreatedAt())
                        .blindedAt(comment.getBlindedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.byeolnight.dto.post.PostAdminDto> getDeletedPosts() {
        return postRepository.findByIsDeletedTrueOrderByCreatedAtDesc().stream()
                .map(post -> com.byeolnight.dto.post.PostAdminDto.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .content(post.getContent())
                        .writer(post.getWriter().getNickname())
                        .category(post.getCategory().name())
                        .blinded(post.isBlinded())
                        .deleted(post.isDeleted())
                        .viewCount(post.getViewCount())
                        .likeCount(post.getLikeCount())
                        .commentCount((int) commentRepository.countByPostId(post.getId()))
                        .createdAt(post.getCreatedAt())
                        .deletedAt(post.getDeletedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.byeolnight.dto.comment.CommentAdminDto> getDeletedComments() {
        return commentRepository.findByDeletedTrueOrderByCreatedAtDesc().stream()
                .map(comment -> com.byeolnight.dto.comment.CommentAdminDto.builder()
                        .id(comment.getId())
                        .content(comment.getContent())
                        .originalContent(comment.getOriginalContent())
                        .writer(comment.getWriter().getNickname())
                        .postTitle(comment.getPost().getTitle())
                        .postId(comment.getPost().getId())
                        .blinded(comment.isBlinded())
                        .deleted(comment.isDeleted())
                        .createdAt(comment.getCreatedAt())
                        .deletedAt(comment.getDeletedAt())
                        .blindedAt(comment.getBlindedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void blindComment(Long commentId) {
        com.byeolnight.domain.entity.comment.Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다."));
        comment.blind();
        pointService.applyPenalty(comment.getWriter(), "댓글 블라인드 처리", commentId.toString());
    }

    @Transactional
    public void unblindComment(Long commentId) {
        com.byeolnight.domain.entity.comment.Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다."));
        comment.unblind();
    }

    @Transactional
    public void restorePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));
        post.restore();
    }

    @Transactional
    public void restoreComment(Long commentId) {
        com.byeolnight.domain.entity.comment.Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다."));
        comment.restore();
    }

    @Transactional
    public void movePostsCategory(java.util.List<Long> postIds, String targetCategory) {
        Post.Category category;
        try {
            category = Post.Category.valueOf(targetCategory.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 카테고리입니다.");
        }
        
        for (Long postId : postIds) {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다: " + postId));
            post.update(post.getTitle(), post.getContent(), category);
        }
    }
}