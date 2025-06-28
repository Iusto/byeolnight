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

        dto.getImages().forEach(image -> {
            File file = File.of(post, image.originalName(), image.s3Key(), image.url());
            fileRepository.save(file);
        });

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
        Post post = postRepository.findWithWriterById(postId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글입니다."));

        if (post.isDeleted() || post.isBlinded()) {
            throw new NotFoundException("삭제되었거나 블라인드 처리된 게시글입니다.");
        }

        post.increaseViewCount();

        boolean likedByMe = currentUser != null && postLikeRepository.existsByUserAndPost(currentUser, post);
        long likeCount = postLikeRepository.countByPost(post);

        return PostResponseDto.of(post, likedByMe, likeCount, false); // 상세 조회에서는 HOT 여부 false로 간주
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

                // HOT 게시글은 HOT 플래그 true
                hotPosts.forEach(p -> combined.add(PostResponseDto.of(p, false, p.getLikeCount(), true)));

                // 나머지 일반 게시글은 HOT 플래그 false
                recentPosts.getContent().stream()
                        .filter(p -> !hotIds.contains(p.getId()))
                        .map(p -> PostResponseDto.of(p, false, p.getLikeCount(), false))
                        .forEach(combined::add);

                return new PageImpl<>(combined, pageable, combined.size());
            }

            case POPULAR -> {
                // ✅ 추천순 정렬: 날짜 무관, HOT 여부 무관, 추천 수 높은 게시글 30개
                Page<Post> popularPosts = postRepository.findByIsDeletedFalseAndBlindedFalseAndCategoryOrderByLikeCountDesc(categoryEnum, pageable);
                List<PostResponseDto> dtos = popularPosts.getContent().stream()
                        .map(p -> PostResponseDto.of(p, false, p.getLikeCount(), false)) // HOT 표시 없음
                        .toList();

                return new PageImpl<>(dtos, pageable, popularPosts.getTotalElements());
            }
        }

        throw new IllegalArgumentException("지원하지 않는 정렬 방식입니다.");
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getTopHotPostsAcrossAllCategories(int size) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        Pageable pageable = PageRequest.of(0, size);

        List<Post> hotPosts = postRepository.findTopHotPostsAcrossAllCategories(threshold, pageable);

        return hotPosts.stream()
                .map(p -> PostResponseDto.of(p, false, p.getLikeCount(), true)) // HOT 플래그 true
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

    public void likePost(Long userId, Long postId) {
        Post post = getPostOrThrow(postId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (postLikeRepository.existsByUserAndPost(user, post)) {
            throw new IllegalArgumentException("이미 추천한 게시글입니다.");
        }

        PostLike like = PostLike.of(user, post);
        postLikeRepository.save(like);
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
                .map(PostResponseDto::from)
                .toList();
    }

    @Transactional
    public void blindPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        post.blind();
    }

    private void validateAdminCategoryWrite(Post.Category category, User user) {
        if ((category == Post.Category.NEWS || category == Post.Category.EVENT || category == Post.Category.NOTICE)
                && user.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("해당 카테고리의 게시글은 관리자만 작성, 수정, 삭제할 수 있습니다.");
        }
    }
}
