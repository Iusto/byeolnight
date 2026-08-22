package com.byeolnight.service.post;

import com.byeolnight.dto.post.PostDto;
import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.entity.file.File;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.repository.file.FileRepository;
import com.byeolnight.repository.post.PostLikeRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.assembler.PostResponseAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 게시글 상세·목록·검색 등 읽기 모델 조립을 전담한다. */
@Service
@RequiredArgsConstructor
public class PostQueryService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostResponseAssembler postResponseAssembler;

    @Transactional
    public PostResponseDto getPostById(Long postId, User currentUser) {
        if (postId == null || postId <= 0) {
            throw new IllegalArgumentException("올바른 게시글 ID가 아닙니다.");
        }
        Post post = postRepository.findWithWriterById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));
        boolean isAdmin = currentUser != null && currentUser.getRole() == User.Role.ADMIN;
        if (post.isDeleted() || (post.isBlinded() && !isAdmin)) {
            throw new NotFoundException("게시글을 찾을 수 없습니다.");
        }

        post.increaseViewCount();
        boolean likedByMe = currentUser != null && postLikeRepository.existsByUserAndPost(currentUser, post);
        long likeCount = postLikeRepository.countByPost(post);
        long commentCount = commentRepository.countByPostId(postId);
        List<File> files = fileRepository.findAllByPost(post);
        return postResponseAssembler.toDto(post, likedByMe, likeCount, false, commentCount, files);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getFilteredPosts(String category, String sortParam,
            String searchType, String search, Pageable pageable, User currentUser) {
        if (search != null && !search.trim().isEmpty()) {
            return searchPosts(category, searchType, search.trim(), pageable, currentUser);
        }
        return getFilteredPosts(category, sortParam, pageable, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getFilteredPosts(
            String category, String sortParam, Pageable pageable, User currentUser) {
        Post.Category parsedCategory = parseCategory(category);
        boolean isAdmin = currentUser != null && currentUser.getRole() == User.Role.ADMIN;
        Post.SortType sort = Post.SortType.from(sortParam);
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);

        if (sort == Post.SortType.RECENT) {
            List<Post> hotPosts = postRepository.findHotPosts(parsedCategory, threshold, 5, 4, isAdmin);
            Page<Post> recentPosts = postRepository
                    .findByIsDeletedFalseAndCategoryOrderByCreatedAtDesc(parsedCategory, pageable);
            Set<Long> hotIds = hotPosts.stream().map(Post::getId).collect(Collectors.toSet());
            List<Post> combined = new ArrayList<>(hotPosts);
            recentPosts.getContent().stream()
                    .filter(post -> !hotIds.contains(post.getId()))
                    .forEach(combined::add);
            Map<Long, Long> counts = batchCommentCounts(combined.stream().map(Post::getId).toList());
            return new PageImpl<>(postResponseAssembler.toDtoList(combined, Map.of(), counts, hotIds),
                    pageable, combined.size());
        }

        Page<Post> popular = postRepository
                .findByIsDeletedFalseAndCategoryOrderByLikeCountDesc(parsedCategory, pageable);
        Map<Long, Long> counts = batchCommentCounts(popular.getContent().stream().map(Post::getId).toList());
        return new PageImpl<>(postResponseAssembler.toDtoList(
                popular.getContent(), Map.of(), counts, Set.of()), pageable, popular.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> searchPosts(String category, String searchType,
            String keyword, Pageable pageable, User currentUser) {
        Page<Post> results = postRepository.searchPosts(
                keyword, parseCategory(category), searchType, pageable);
        Map<Long, Long> counts = batchCommentCounts(results.getContent().stream().map(Post::getId).toList());
        return new PageImpl<>(postResponseAssembler.toDtoList(
                results.getContent(), Map.of(), counts, Set.of()), pageable, results.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getTopHotPostsAcrossAllCategories(int size) {
        List<Post> posts = postRepository.findHotPosts(
                null, LocalDateTime.now().minusDays(30), 5, size, false);
        Set<Long> hotIds = posts.stream().map(Post::getId).collect(Collectors.toSet());
        Map<Long, Long> counts = batchCommentCounts(posts.stream().map(Post::getId).toList());
        return postResponseAssembler.toDtoList(posts, Map.of(), counts, hotIds);
    }

    @Transactional(readOnly = true)
    public Page<PostDto.Response> getMyPosts(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        Page<Post> posts = postRepository.findByWriterAndIsDeletedFalseOrderByCreatedAtDesc(user, pageable);
        Map<Long, Long> counts = batchCommentCounts(posts.getContent().stream().map(Post::getId).toList());
        List<PostDto.Response> responses = posts.getContent().stream()
                .map(post -> PostDto.Response.from(
                        post, post.getLikeCount(), counts.getOrDefault(post.getId(), 0L)))
                .toList();
        return new PageImpl<>(responses, pageable, posts.getTotalElements());
    }

    private Post.Category parseCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("카테고리는 필수입니다.");
        }
        try {
            return Post.Category.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("올바르지 않은 카테고리입니다.");
        }
    }

    private Map<Long, Long> batchCommentCounts(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        return commentRepository.countByPostIdIn(postIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
