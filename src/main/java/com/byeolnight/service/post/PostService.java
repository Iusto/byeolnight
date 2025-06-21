package com.byeolnight.service.post;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.PostLike;
import com.byeolnight.domain.entity.post.PostReport;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.PostLikeRepository;
import com.byeolnight.domain.repository.PostReportRepository;
import com.byeolnight.domain.repository.PostRepository;
import com.byeolnight.domain.repository.UserRepository;
import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.infrastructure.exception.InvalidRequestException;
import com.byeolnight.infrastructure.exception.NotFoundException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final PostReportRepository postReportRepository;

    @Transactional
    public Long createPost(PostRequestDto dto, User user) {
        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory())
                .writer(user)
                .build();
        return postRepository.save(post).getId();
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPostById(Long postId, @Nullable User viewer) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));

        long likeCount = postLikeRepository.countByPost(post);

        boolean likedByMe = false;
        if (viewer != null) {
            likedByMe = postLikeRepository.existsByUserAndPost(viewer, post);
        }

        return PostResponseDto.of(post, likedByMe, likeCount);
    }



    @Transactional(readOnly = true)
    public Page<PostResponseDto> getAllPosts(Pageable pageable) {
        return postRepository.findAllByIsDeletedFalse(pageable)
                .map(PostResponseDto::from);
    }

    @Transactional
    public void updatePost(Long id, PostRequestDto dto, User user) {
        Post post = postRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        if (!post.getWriter().equals(user)) {
            throw new IllegalArgumentException("게시글 수정 권한이 없습니다.");
        }
        post.update(dto.getTitle(), dto.getContent(), dto.getCategory());
    }

    @Transactional
    public void softDeletePost(Long id, User user) {
        Post post = postRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
        if (!post.getWriter().equals(user)) {
            throw new IllegalArgumentException("게시글 삭제 권한이 없습니다.");
        }
        post.softDelete();
    }

    @Transactional
    public void likePost(Long userId, Long postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));

        if (postLikeRepository.existsByUserAndPost(user, post)) {
            throw new IllegalStateException("이미 추천한 게시글입니다.");
        }

        PostLike like = PostLike.of(user, post);
        postLikeRepository.save(like);
    }

    @Transactional
    public void reportPost(Long userId, Long postId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));

        // 중복 신고 방지
        if (postReportRepository.existsByUserAndPost(user, post)) {
            throw new IllegalStateException("이미 신고한 게시글입니다.");
        }

        // 신고 내역 저장
        PostReport report = PostReport.of(user, post, reason);
        postReportRepository.save(report);

        // ✅ 신고 수가 5회 이상이면 자동 블라인드 처리
        long reportCount = postReportRepository.countByPost(post);
        if (reportCount >= 5 && !post.isBlinded()) {
            post.blind(); // post.setBlinded(true)
            // (선택) 블라인드 로그를 남길 수 있음
        }
    }

    public Page<PostResponseDto> getFilteredPosts(String category, String sort, Pageable pageable) {
        Post.Category categoryEnum = null;
        if (category != null) {
            try {
                categoryEnum = Post.Category.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestException("잘못된 카테고리입니다.");
            }
        }

        // ⭐ 인기순 정렬: JPQL 기반 likeCount 기준
        if ("POPULAR".equalsIgnoreCase(sort)) {
            return postRepository.findPostsByCategoryOrderByLikeCountDesc(categoryEnum, pageable)
                    .map(PostResponseDto::from);
        }

        // ✅ 최신순 정렬
        if (categoryEnum != null) {
            return postRepository.findByIsDeletedFalseAndCategoryOrderByCreatedAtDesc(categoryEnum, pageable)
                    .map(PostResponseDto::from);
        } else {
            return postRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable)
                    .map(PostResponseDto::from);
        }
    }

}