package com.byeolnight.service.post;

import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.post.PostBlindLog;
import com.byeolnight.entity.post.PostReport;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.post.PostBlindLogRepository;
import com.byeolnight.repository.post.PostReportRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.UserRepository;
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
        
        // Post 엔티티의 신고수 증가
        post.increaseReportCount();
        postRepository.save(post);

        // 신고 수 5개 이상이면 자동 블라인드 (포인트 지급은 관리자 승인 후)
        if (post.getReportCount() >= 5 && !post.isBlinded()) {
            post.blind();
            System.out.println("신고 자동 블라인드 처리: postId=" + postId + ", blindType=" + post.getBlindType());
            if (!postBlindLogRepository.existsByPostId(post.getId())) {
                postBlindLogRepository.save(PostBlindLog.of(post, PostBlindLog.Reason.REPORT));
            }
        }
        
        postRepository.save(post);
    }
}
