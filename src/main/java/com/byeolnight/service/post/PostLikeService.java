package com.byeolnight.service.post;

import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.post.PostLike;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.repository.post.PostLikeRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.user.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 추천 생성과 양쪽 사용자 포인트 지급을 하나의 정책으로 처리한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final PointService pointService;

    @Transactional
    public void likePost(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        try {
            postLikeRepository.save(PostLike.of(user, post));
            post.increaseLikeCount();
            pointService.awardGiveLikePoints(user, postId.toString());
            pointService.awardReceiveLikePoints(post.getWriter(), postId.toString());
        } catch (DataIntegrityViolationException exception) {
            log.debug("중복 게시글 추천 시도: userId={}, postId={}", userId, postId);
            throw new IllegalArgumentException("이미 추천한 글입니다.");
        }
    }
}
