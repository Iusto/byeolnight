package com.byeolnight.service.post;

import com.byeolnight.domain.entity.post.PopularPost;
import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.PostLike;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.post.PopularPostRepository;
import com.byeolnight.domain.repository.post.PostLikeRepository;
import com.byeolnight.domain.repository.post.PostRepository;
import com.byeolnight.infrastructure.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final PopularPostRepository popularPostRepository;

    private static final int POPULAR_THRESHOLD = 10;

    @Transactional
    public void likePost(Long postId, User user) {
        Post post = getPostOrThrow(postId);

        validateNotAlreadyLiked(user, post);

        postLikeRepository.save(PostLike.of(user, post));

        registerPopularPostIfNeeded(post);
    }

    @Transactional
    public void unlikePost(Long postId, User user) {
        Post post = getPostOrThrow(postId);

        PostLike like = postLikeRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new NotFoundException("추천 정보가 존재하지 않습니다."));

        postLikeRepository.delete(like);

        unregisterPopularPostIfNeeded(post);
    }

    // --- 내부 유틸 메서드들 ---

    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글이 존재하지 않습니다."));
    }

    private void validateNotAlreadyLiked(User user, Post post) {
        if (postLikeRepository.existsByUserAndPost(user, post)) {
            throw new IllegalStateException("이미 추천한 게시글입니다.");
        }
    }

    private void registerPopularPostIfNeeded(Post post) {
        long likeCount = postLikeRepository.countByPost(post);
        if (likeCount >= POPULAR_THRESHOLD && !popularPostRepository.existsByPost(post)) {
            popularPostRepository.save(PopularPost.of(post));
        }
    }

    private void unregisterPopularPostIfNeeded(Post post) {
        long likeCount = postLikeRepository.countByPost(post);
        if (likeCount < POPULAR_THRESHOLD && popularPostRepository.existsByPost(post)) {
            popularPostRepository.deleteByPost(post);
        }
    }
}
