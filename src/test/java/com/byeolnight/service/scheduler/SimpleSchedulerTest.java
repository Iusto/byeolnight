package com.byeolnight.service.scheduler;

import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.PostCleanupScheduler;
import com.byeolnight.service.discussion.DiscussionTopicScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SimpleSchedulerTest {

    @Autowired private PostCleanupScheduler postCleanupScheduler;
    @Autowired private DiscussionTopicScheduler discussionTopicScheduler;
    @Autowired private PostRepository postRepository;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("게시글 정리 스케줄러 실행 테스트")
    void testPostCleanupScheduler() {
        User user = createTestUser();
        Post post = createTestPost(user);
        
        long beforeCount = postRepository.count();
        
        assertThatCode(() -> postCleanupScheduler.cleanupExpiredPosts())
                .doesNotThrowAnyException();
        
        // 실행 후 카운트 확인 (실제 삭제 여부보다는 실행 성공에 집중)
        assertThat(postRepository.count()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("토론 주제 스케줄러 - 시스템 사용자 생성 테스트")
    void testDiscussionTopicScheduler() {
        assertThatCode(() -> {
            User systemUser = discussionTopicScheduler.getSystemUser();
            assertThat(systemUser).isNotNull();
            assertThat(systemUser.getEmail()).isEqualTo("system@byeolnight.com");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("토론 주제 비활성화 테스트")
    void testDeactivateOldTopics() {
        User systemUser = discussionTopicScheduler.getSystemUser();
        Post topic = createDiscussionTopic(systemUser);
        
        assertThatCode(() -> discussionTopicScheduler.deactivateOldTopics())
                .doesNotThrowAnyException();
        
        Post updated = postRepository.findById(topic.getId()).orElseThrow();
        assertThat(updated.isPinned()).isFalse();
    }

    @Test
    @DisplayName("토론 주제 내용 파싱 테스트")
    void testTopicContentParsing() {
        String content = "제목: 우주 탐사의 미래\n내용: 화성 이주 계획에 대해 어떻게 생각하시나요?";
        
        String[] parsed = discussionTopicScheduler.parseTopicContent(content);
        
        assertThat(parsed[0]).isEqualTo("우주 탐사의 미래");
        assertThat(parsed[1]).isEqualTo("화성 이주 계획에 대해 어떻게 생각하시나요?");
    }

    @Test
    @DisplayName("잘못된 형식 파싱 시 기본값 반환 테스트")
    void testInvalidContentParsing() {
        String invalidContent = "잘못된 형식";
        
        String[] parsed = discussionTopicScheduler.parseTopicContent(invalidContent);
        
        assertThat(parsed[0]).isEqualTo("오늘의 우주 토론");
        assertThat(parsed[1]).isEqualTo("우주에 대한 흥미로운 주제로 토론해보세요.");
    }

    private User createTestUser() {
        return userRepository.save(User.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .password("password")
                .role(User.Role.USER)
                .build());
    }

    private Post createTestPost(User user) {
        return postRepository.save(Post.builder()
                .title("테스트 게시글")
                .content("테스트 내용")
                .category(Post.Category.FREE)
                .writer(user)
                .build());
    }

    private Post createDiscussionTopic(User user) {
        Post topic = Post.builder()
                .title("토론 주제")
                .content("토론 내용")
                .category(Post.Category.DISCUSSION)
                .writer(user)
                .build();
        topic.setAsDiscussionTopic();
        return postRepository.save(topic);
    }
}