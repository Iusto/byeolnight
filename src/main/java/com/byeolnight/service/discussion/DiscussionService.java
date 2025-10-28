package com.byeolnight.service.discussion;

import com.byeolnight.dto.admin.DiscussionStatusDto;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.dto.post.PostResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscussionService {

    private final DiscussionTopicScheduler discussionTopicScheduler;

    private final PostRepository postRepository;

    /**
     * 오늘의 토론 주제 조회
     */
    public PostResponseDto getTodayDiscussionTopic() {
        Post todayTopic = postRepository.findTodayDiscussionTopic()
                .orElse(null);

        if (todayTopic == null) {
            return null;
        }

        return PostResponseDto.of(todayTopic, false, todayTopic.getLikeCount(), false, 0);
    }

    /**
     * 특정 토론 주제에 대한 의견글 목록 조회
     */
    public Page<PostResponseDto> getOpinionPosts(Long topicId, Pageable pageable) {
        Page<Post> opinionPosts = postRepository.findRelatedOpinionPosts(topicId, pageable);
        
        return opinionPosts.map(post -> 
            PostResponseDto.of(post, false, post.getLikeCount(), false, 0)
        );
    }

    /**
     * 토론 게시판 전체 목록 조회 (일반 토론글만, 오늘의 주제는 별도 조회)
     */
    public Page<PostResponseDto> getDiscussionPosts(Pageable pageable) {
        // 토론 카테고리이면서 AI 생성 주제가 아닌 일반 사용자 글만 조회
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Post> discussionPosts = postRepository.findByCategoryAndDiscussionTopicFalseAndIsDeletedFalse(
            Post.Category.DISCUSSION, sortedPageable
        );

        return discussionPosts.map(post -> 
            PostResponseDto.of(post, false, post.getLikeCount(), false, 0)
        );
    }

    /**
     * 토론 시스템 상태 조회
     */
    public DiscussionStatusDto getDiscussionStatus() {
        Post todayTopic = postRepository.findTodayDiscussionTopic().orElse(null);
        long totalDiscussionPosts = postRepository.countByCategoryAndIsDeletedFalse(Post.Category.DISCUSSION);
        
        return DiscussionStatusDto.builder()
            .todayTopicExists(todayTopic != null)
            .todayTopicTitle(todayTopic != null ? todayTopic.getTitle() : null)
            .totalDiscussionPosts(totalDiscussionPosts)
            .lastUpdated(java.time.LocalDateTime.now())
            .build();
    }

    /**
     * 관리자 수동 토론 주제 생성
     */
    @Transactional
    public void generateDiscussionTopicManually(User admin) {
        discussionTopicScheduler.generateDailyDiscussionTopic();
    }
}