package com.byeolnight.service.post;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.PostBlindLog;
import com.byeolnight.domain.entity.post.PostReport;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.post.*;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.infrastructure.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostReportService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostReportRepository postReportRepository;
    private final PostBlindLogRepository postBlindLogRepository;

    private static final int BLIND_THRESHOLD = 10;

    @Transactional
    public void reportPost(Long userId, Long postId, String reason, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));

        if (postReportRepository.existsByUserAndPost(user, post)) {
            throw new IllegalStateException("이미 신고한 게시글입니다.");
        }

        PostReport report = PostReport.of(user, post, reason, description);
        postReportRepository.save(report);

        // 신고 수 3개 이상이면 자동 블라인드
        long reportCount = postReportRepository.countByPost(post);
        if (reportCount >= 3 && !post.isBlinded()) {
            post.blind();
            if (!postBlindLogRepository.existsByPostId(post.getId())) {
                postBlindLogRepository.save(PostBlindLog.of(post, PostBlindLog.Reason.REPORT));
            }
        }
    }
}
