package com.byeolnight.service.post;

import com.byeolnight.domain.entity.file.File;
import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.Post.Category;
import com.byeolnight.domain.entity.post.PostLike;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.post.FileRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.domain.repository.post.PopularPostRepository;
import com.byeolnight.domain.repository.post.PostLikeRepository;
import com.byeolnight.domain.repository.post.PostRepository;
import com.byeolnight.dto.file.FileDto;
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
    private final PopularPostRepository popularPostRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    @Transactional
    public Long createPost(PostRequestDto dto, User user) {
        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory())
                .writer(user)
                .build();
        postRepository.save(post);

        // 이미지 엔티티 저장
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

        post.update(dto.getTitle(), dto.getContent(), dto.getCategory());

        // 기존 이미지 삭제
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

        post.increaseViewCount(); // ✅ 조회수 증가

        boolean likedByMe = currentUser != null && postLikeRepository.existsByUserAndPost(currentUser, post);
        long likeCount = postLikeRepository.countByPost(post);

        return PostResponseDto.of(post, likedByMe, likeCount);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getFilteredPosts(String category, String sort, Pageable pageable) {
        Category categoryEnum = parseCategory(category);

        if (!"recent".equalsIgnoreCase(sort) && !"popular".equalsIgnoreCase(sort)) {
            throw new IllegalArgumentException("지원하지 않는 정렬 방식입니다.");
        }

        if ("popular".equalsIgnoreCase(sort)) {
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            List<Long> hotIds = popularPostRepository.findPopularPostIdsSince(threshold);
            if (hotIds.isEmpty()) return Page.empty(pageable);

            // writer 포함 조회 후 blind 필터링
            List<Post> posts = postRepository.findPostsByIds(hotIds).stream()
                    .filter(post -> !post.isBlinded())
                    .toList();

            Map<Long, Post> postMap = posts.stream()
                    .collect(Collectors.toMap(Post::getId, p -> p));

            List<Post> sorted = hotIds.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .toList();

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), sorted.size());
            if (start >= sorted.size()) return Page.empty(pageable);
            List<Post> paged = sorted.subList(start, end);

            return new PageImpl<>(
                    paged.stream().map(PostResponseDto::from).toList(),
                    pageable,
                    sorted.size()
            );
        }

        // 최신순 정렬
        if (categoryEnum != null) {
            return postRepository.findByIsDeletedFalseAndBlindedFalseAndCategoryOrderByCreatedAtDesc(categoryEnum, pageable)
                    .map(PostResponseDto::from);
        } else {
            return postRepository.findByIsDeletedFalseAndBlindedFalseOrderByCreatedAtDesc(pageable)
                    .map(PostResponseDto::from);
        }
    }

    @Transactional
    public void deletePost(Long postId, User user) {
        Post post = getPostOrThrow(postId);

        if (!post.getWriter().equals(user)) {
            throw new IllegalArgumentException("본인이 작성한 글만 삭제할 수 있습니다.");
        }

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
            throw new IllegalArgumentException("잘못된 카테고리입니다. (NEWS, DISCUSSION, IMAGE, EVENT, REVIEW 중 선택)");
        }
    }

    // 관리자 기능(블라인드 처리된 게시글 조회)
    @Transactional(readOnly = true)
    public List<PostResponseDto> getBlindedPosts() {
        return postRepository.findByIsDeletedFalseAndBlindedTrueOrderByCreatedAtDesc().stream()
                .map(PostResponseDto::from)
                .toList();
    }

    // 관리자 기능(게시글 블라인드 처리)
    @Transactional
    public void blindPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        post.blind(); // 엔티티 메서드 사용
    }
}
