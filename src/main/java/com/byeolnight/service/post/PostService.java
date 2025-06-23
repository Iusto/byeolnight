package com.byeolnight.service.post;

import com.byeolnight.domain.entity.file.File;
import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.Post.Category;
import com.byeolnight.domain.entity.post.PostLike;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.domain.repository.post.FileRepository;
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

    public Long createPost(PostRequestDto dto, User user, List<FileDto> files) {
        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory())
                .writer(user)
                .build();
        postRepository.save(post);
        handleImageUpload(files, post);
        return post.getId();
    }

    @Transactional
    public void updatePost(Long postId, PostRequestDto dto, User user, List<FileDto> files) {
        Post post = getPostOrThrow(postId);

        if (!post.getWriter().equals(user)) {
            throw new IllegalArgumentException("본인이 작성한 글만 수정할 수 있습니다.");
        }

        post.update(dto.getTitle(), dto.getContent(), dto.getCategory());

        fileRepository.deleteAllByPost(post);
        handleImageUpload(files, post);
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPostById(Long postId, User currentUser) {
        Post post = postRepository.findWithWriterById(postId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 게시글입니다."));

        if (post.isDeleted()) throw new NotFoundException("삭제된 게시글입니다.");

        boolean likedByMe = currentUser != null && postLikeRepository.existsByUserAndPost(currentUser, post);
        long likeCount = postLikeRepository.countByPost(post);

        return PostResponseDto.of(post, likedByMe, likeCount);
    }

    public Page<PostResponseDto> getAllPosts(Pageable pageable) {
        return postRepository.findAllByIsDeletedFalse(pageable)
                .map(PostResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getFilteredPosts(String category, String sort, Pageable pageable) {
        Category categoryEnum = parseCategory(category);

        if (!"recent".equalsIgnoreCase(sort) && !"popular".equalsIgnoreCase(sort)) {
            throw new IllegalArgumentException("지원하지 않는 정렬 방식입니다.");
        }

        if ("popular".equalsIgnoreCase(sort)) {
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);

            // 추천수 10개 이상, 최근 30일 게시글 ID 조회
            List<Long> hotIds = popularPostRepository.findPopularPostIdsSince(threshold);
            if (hotIds.isEmpty()) return Page.empty(pageable);

            // 작성자(writer) fetch join으로 Lazy 초기화
            List<Post> posts = postRepository.findPostsByIds(hotIds);

            // ID 기준으로 Map 변환 후 hotIds 순서대로 정렬
            Map<Long, Post> postMap = posts.stream()
                    .collect(Collectors.toMap(Post::getId, p -> p));

            List<Post> sorted = hotIds.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .toList();

            // 페이지네이션 적용
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
            return postRepository.findByIsDeletedFalseAndCategoryOrderByCreatedAtDesc(categoryEnum, pageable)
                    .map(PostResponseDto::from);
        } else {
            return postRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable)
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

    private void handleImageUpload(List<FileDto> files, Post post) {
        if (files == null || files.isEmpty()) return;

        List<File> entities = files.stream()
                .map(dto -> File.of(post, dto.originalName(), dto.s3Key(), dto.url()))
                .toList();

        fileRepository.saveAll(entities);
    }
}
