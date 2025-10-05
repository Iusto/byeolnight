package com.byeolnight.service.post;

import com.byeolnight.entity.file.File;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.post.Post.Category;
import com.byeolnight.entity.post.PostLike;
import com.byeolnight.entity.user.User;
import com.byeolnight.entity.comment.Comment;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.repository.file.FileRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.repository.post.PostLikeRepository;
import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.dto.post.PostDto;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.repository.post.PostReportRepository;
import com.byeolnight.service.log.file.S3Service;
import com.byeolnight.service.user.PointService;
import com.byeolnight.service.log.DeleteLogService;
import com.byeolnight.entity.log.DeleteLog;
import com.byeolnight.infrastructure.lock.DistributedLockService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final PostReportRepository postReportRepository;
    private final S3Service s3Service;
    private final com.byeolnight.service.certificate.CertificateService certificateService;
    private final PointService pointService;
    private final CommentRepository commentRepository;
    private final com.byeolnight.service.notification.NotificationService notificationService;
    private final DeleteLogService deleteLogService;
    private final DistributedLockService distributedLockService;

    @Transactional
    public Long createPost(PostRequestDto dto, User user) {
        String lockKey = "post_create:" + user.getId() + ":" + dto.getTitle().hashCode();
        
        return distributedLockService.executeWithLock(lockKey, 5, 10, () -> {
            validateAdminCategoryWrite(dto.getCategory(), user);

            // HTML 엔티티 디코딩 처리
            String decodedTitle = HtmlUtils.htmlUnescape(dto.getTitle());
            String decodedContent = HtmlUtils.htmlUnescape(dto.getContent());
            
            Post post = Post.builder()
                    .title(decodedTitle)
                    .content(decodedContent)
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
                log.warn("포인트 인증서 발급 실패: {}", e.getMessage());
            }
            
            if (dto.getCategory() == Post.Category.NOTICE) {
                try {
                    notificationService.notifyNewNotice(post.getId(), dto.getTitle());
                } catch (Exception e) {
                log.warn("공지사항 알림 전송 실패: {}", e.getMessage());
                }
            }

            return post.getId();
        });
    }

    @Transactional
    public void updatePost(Long postId, PostRequestDto dto, User user) {
        Post post = getPostOrThrow(postId);

        if (!post.getWriter().equals(user)) {
            throw new IllegalArgumentException("본인이 작성한 글만 수정할 수 있습니다.");
        }

        validateAdminCategoryWrite(dto.getCategory(), user);
        
        // HTML 엔티티 디코딩 처리
        String decodedTitle = HtmlUtils.htmlUnescape(dto.getTitle());
        String decodedContent = HtmlUtils.htmlUnescape(dto.getContent());
        
        post.update(decodedTitle, decodedContent, dto.getCategory());

        // 기존 파일 목록 조회
        List<File> oldFiles = fileRepository.findAllByPost(post);
        Set<String> newImageUrls = dto.getImages().stream()
                .map(img -> img.url())
                .collect(Collectors.toSet());
        
        // 더 이상 사용되지 않는 파일만 삭제
        List<File> filesToDelete = oldFiles.stream()
                .filter(file -> !newImageUrls.contains(file.getUrl()))
                .toList();
        
        filesToDelete.forEach(file -> {
            s3Service.deleteObject(file.getS3Key());
            fileRepository.delete(file);
        });
        
        // 기존 파일 URL 목록
        Set<String> existingUrls = oldFiles.stream()
                .map(File::getUrl)
                .collect(Collectors.toSet());
        
        // 새로운 이미지만 추가
        dto.getImages().stream()
                .filter(image -> !existingUrls.contains(image.url()))
                .forEach(image -> {
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

        // 삭제된 게시글은 관리자도 접근 불가
        if (post.isDeleted()) {
            throw new NotFoundException("삭제된 게시글입니다.");
        }
        
        // 블라인드 처리된 게시글은 관리자만 접근 가능
        if (post.isBlinded() && (currentUser == null || currentUser.getRole() != User.Role.ADMIN)) {
            throw new NotFoundException("블라인드 처리된 게시글입니다.");
        }

        post.increaseViewCount();

        boolean likedByMe = currentUser != null && postLikeRepository.existsByUserAndPost(currentUser, post);
        long likeCount = postLikeRepository.countByPost(post);
        long commentCount = commentRepository.countByPostId(postId);

        PostResponseDto responseDto = PostResponseDto.of(post, likedByMe, likeCount, false, commentCount);
        
        return responseDto;
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
                List<Post> hotPosts = postRepository.findHotPosts(categoryEnum, threshold, 5, 4);
                Page<Post> recentPosts = postRepository.findByIsDeletedFalseAndCategoryOrderByCreatedAtDesc(categoryEnum, pageable);

                Set<Long> hotIds = hotPosts.stream().map(Post::getId).collect(Collectors.toSet());
                List<PostResponseDto> combined = new ArrayList<>();

                // HOT 게시글 처리 (이미 작성자 존재 확인된 데이터)
                hotPosts.forEach(p -> {
                    long actualLikeCount = postLikeRepository.countByPost(p);
                    long commentCount = commentRepository.countByPostId(p.getId());
                    combined.add(PostResponseDto.of(p, false, actualLikeCount, true, commentCount));
                });

                // 최신 게시글 처리 (이미 작성자 존재 확인된 데이터)
                recentPosts.getContent().stream()
                        .filter(p -> !hotIds.contains(p.getId()))
                        .forEach(p -> {
                            long actualLikeCount = postLikeRepository.countByPost(p);
                            long commentCount = commentRepository.countByPostId(p.getId());
                            combined.add(PostResponseDto.of(p, false, actualLikeCount, false, commentCount));
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
        
        // QueryDSL 동적 검색 사용
        Page<Post> searchResults = postRepository.searchPosts(keyword, categoryEnum, searchType, pageable);
        
        List<PostResponseDto> dtos = searchResults.getContent().stream()
                .map(p -> {
                    long actualLikeCount = postLikeRepository.countByPost(p);
                    long commentCount = commentRepository.countByPostId(p.getId());
                    return PostResponseDto.of(p, false, actualLikeCount, false, commentCount);
                })
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, searchResults.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getTopHotPostsAcrossAllCategories(int size) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        Pageable pageable = PageRequest.of(0, size);

        List<Post> hotPosts = postRepository.findHotPosts(null, threshold, 5, size);

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

        // 삭제 로그 기록
        deleteLogService.logDeletion(
            postId,
            DeleteLog.TargetType.POST,
            DeleteLog.ActionType.SOFT_DELETE,
            user.getId(),
            "사용자 삭제",
            post.getTitle() + ": " + post.getContent()
        );

        List<File> files = fileRepository.findAllByPost(post);
        files.forEach(file -> s3Service.deleteObject(file.getS3Key()));
        fileRepository.deleteAllByPost(post);

        post.softDelete();
    }

    @Transactional
    public void likePost(Long userId, Long postId) {
        String lockKey = "post_like:" + userId + ":" + postId;
        
        distributedLockService.executeWithLock(lockKey, 5, 10, () -> {
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
        });
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
    public void blindPostByAdmin(Long postId, Long adminId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        
        // 관리자 블라인드 로그 기록
        deleteLogService.logDeletion(
            postId,
            DeleteLog.TargetType.POST,
            DeleteLog.ActionType.BLIND,
            adminId,
            "관리자 직접 블라인드",
            post.getTitle() + ": " + post.getContent()
        );
        
        post.blindByAdmin(adminId);
        postRepository.save(post);
        log.info("관리자 블라인드 처리: postId={}, blindType={}", postId, post.getBlindType());
        pointService.applyPenalty(post.getWriter(), "관리자 블라인드 처리", postId.toString());
    }

    @Transactional
    public void unblindPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        post.unblind();
    }

    private void validateAdminCategoryWrite(Post.Category category, User user) {
        if ((category == Post.Category.NEWS || category == Post.Category.NOTICE)
                && user.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("해당 카테고리의 게시글은 관리자만 작성할 수 있습니다.");
        }
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

    @Transactional
    public void restorePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));
        post.restore();
    }

    @Transactional
    public void restoreComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
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