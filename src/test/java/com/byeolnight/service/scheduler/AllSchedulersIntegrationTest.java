package com.byeolnight.service.scheduler;

import com.byeolnight.entity.Message;
import com.byeolnight.entity.comment.Comment;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.MessageRepository;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.PostCleanupScheduler;
import com.byeolnight.service.auth.SocialAccountCleanupService;
import com.byeolnight.service.crawler.SpaceNewsScheduler;
import com.byeolnight.service.discussion.DiscussionTopicScheduler;
import com.byeolnight.service.message.MessageCleanupService;
import com.byeolnight.service.user.WithdrawnUserCleanupService;
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
class AllSchedulersIntegrationTest {

    @Autowired private PostCleanupScheduler postCleanupScheduler;
    @Autowired private SpaceNewsScheduler spaceNewsScheduler;
    @Autowired private DiscussionTopicScheduler discussionTopicScheduler;
    @Autowired private MessageCleanupService messageCleanupService;
    @Autowired private SocialAccountCleanupService socialAccountCleanupService;
    @Autowired private WithdrawnUserCleanupService withdrawnUserCleanupService;
    
    @Autowired private PostRepository postRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("게시글 정리 스케줄러 - 만료된 게시글/댓글 삭제")
    void testPostCleanupScheduler() {
        User user = createTestUser();
        
        // 만료된 게시글
        Post expiredPost = createExpiredPost(user);
        Comment expiredComment = createExpiredComment(user, expiredPost);
        
        // 최근 게시글 (삭제되지 않아야 함)
        Post recentPost = createRecentPost(user);
        
        long postsBefore = postRepository.count();
        long commentsBefore = commentRepository.count();
        
        postCleanupScheduler.cleanupExpiredPosts();
        
        assertThat(postRepository.count()).isEqualTo(postsBefore - 1);
        assertThat(commentRepository.count()).isEqualTo(commentsBefore - 1);
        assertThat(postRepository.existsById(recentPost.getId())).isTrue();
    }

    @Test
    @DisplayName("토론 주제 스케줄러 - 기존 토론 비활성화")
    void testDiscussionTopicScheduler() {
        User systemUser = discussionTopicScheduler.getSystemUser();
        Post oldTopic = createActiveTopic(systemUser);
        
        discussionTopicScheduler.deactivateOldTopics();
        
        Post updated = postRepository.findById(oldTopic.getId()).orElseThrow();
        assertThat(updated.isPinned()).isFalse();
    }

    @Test
    @DisplayName("쪽지 정리 스케줄러 - 만료된 쪽지 삭제")
    void testMessageCleanupScheduler() {
        User sender = createTestUser();
        User receiver = createTestUser("receiver@test.com", "수신자");
        Message expiredMessage = createExpiredMessage(sender, receiver);
        
        long messagesBefore = messageRepository.count();
        
        messageCleanupService.cleanupOldMessages();
        
        assertThat(messageRepository.count()).isLessThan(messagesBefore);
    }

    @Test
    @DisplayName("소셜 계정 정리 스케줄러 - 30일 경과 개인정보 마스킹")
    void testSocialAccountCleanupScheduler() {
        User socialUser = createWithdrawnSocialUser();
        String originalEmail = socialUser.getEmail();
        
        socialAccountCleanupService.maskPersonalInfoAfterThirtyDays();
        
        User updated = userRepository.findById(socialUser.getId()).orElseThrow();
        assertThat(updated.getEmail()).isNotEqualTo(originalEmail);
        assertThat(updated.getEmail()).startsWith("deleted_");
    }

    @Test
    @DisplayName("탈퇴 회원 정리 스케줄러 - 5년 경과 계정 정리")
    void testWithdrawnUserCleanupScheduler() {
        User oldWithdrawnUser = createOldWithdrawnUser();
        String originalEmail = oldWithdrawnUser.getEmail();
        
        withdrawnUserCleanupService.cleanupWithdrawnUsers();
        
        User updated = userRepository.findById(oldWithdrawnUser.getId()).orElseThrow();
        assertThat(updated.getEmail()).isNotEqualTo(originalEmail);
    }

    @Test
    @DisplayName("뉴스 수집 스케줄러 - 실행 오류 없음 확인")
    void testSpaceNewsScheduler() {
        assertThatCode(() -> spaceNewsScheduler.scheduleSpaceNewsCollection())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("모든 스케줄러 동시 실행 테스트")
    void testAllSchedulersExecution() {
        assertThatCode(() -> {
            postCleanupScheduler.cleanupExpiredPosts();
            discussionTopicScheduler.deactivateOldTopics();
            messageCleanupService.cleanupOldMessages();
            socialAccountCleanupService.maskPersonalInfoAfterThirtyDays();
            withdrawnUserCleanupService.cleanupWithdrawnUsers();
        }).doesNotThrowAnyException();
    }

    // Helper methods
    private User createTestUser() {
        return createTestUser("test@example.com", "테스트유저");
    }

    private User createTestUser(String email, String nickname) {
        return userRepository.save(User.builder()
                .email(email)
                .nickname(nickname)
                .password("password")
                .role(User.Role.USER)
                .build());
    }

    private Post createExpiredPost(User user) {
        Post post = Post.builder()
                .title("만료된 게시글")
                .content("내용")
                .category(Post.Category.FREE)
                .writer(user)
                .build();
        post.softDelete();
        // Reflection으로 deletedAt 설정
        try {
            var field = Post.class.getDeclaredField("deletedAt");
            field.setAccessible(true);
            field.set(post, LocalDateTime.now().minusDays(31));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return postRepository.save(post);
    }

    private Post createRecentPost(User user) {
        Post post = Post.builder()
                .title("최근 게시글")
                .content("내용")
                .category(Post.Category.FREE)
                .writer(user)
                .build();
        post.softDelete();
        return postRepository.save(post);
    }

    private Comment createExpiredComment(User user, Post post) {
        Comment comment = Comment.builder()
                .content("만료된 댓글")
                .writer(user)
                .post(post)
                .build();
        comment.softDelete();
        // Reflection으로 deletedAt 설정
        try {
            var field = Comment.class.getDeclaredField("deletedAt");
            field.setAccessible(true);
            field.set(comment, LocalDateTime.now().minusDays(31));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return commentRepository.save(comment);
    }

    private Post createActiveTopic(User user) {
        Post topic = Post.builder()
                .title("기존 토론 주제")
                .content("내용")
                .category(Post.Category.DISCUSSION)
                .writer(user)
                .build();
        topic.setAsDiscussionTopic();
        topic.pin();
        return postRepository.save(topic);
    }

    private Message createExpiredMessage(User sender, User receiver) {
        Message message = Message.builder()
                .title("만료된 쪽지")
                .content("내용")
                .sender(sender)
                .receiver(receiver)
                .build();
        message.deleteBySender();
        message.deleteByReceiver();
        // Reflection으로 삭제 시간 설정
        try {
            var senderField = Message.class.getDeclaredField("senderDeletedAt");
            var receiverField = Message.class.getDeclaredField("receiverDeletedAt");
            senderField.setAccessible(true);
            receiverField.setAccessible(true);
            senderField.set(message, LocalDateTime.now().minusYears(4));
            receiverField.set(message, LocalDateTime.now().minusYears(4));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return messageRepository.save(message);
    }

    private User createWithdrawnSocialUser() {
        User user = User.builder()
                .email("social@example.com")
                .nickname("소셜유저")
                .password("password")
                .role(User.Role.USER)
                .build();
        user.withdraw("테스트 탈퇴");
        // Reflection으로 withdrawnAt 설정
        try {
            var field = User.class.getDeclaredField("withdrawnAt");
            field.setAccessible(true);
            field.set(user, LocalDateTime.now().minusDays(31));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return userRepository.save(user);
    }

    private User createOldWithdrawnUser() {
        User user = User.builder()
                .email("old@example.com")
                .nickname("오래된유저")
                .password("password")
                .role(User.Role.USER)
                .build();
        user.withdraw("테스트 탈퇴");
        // Reflection으로 withdrawnAt 설정
        try {
            var field = User.class.getDeclaredField("withdrawnAt");
            field.setAccessible(true);
            field.set(user, LocalDateTime.now().minusYears(6));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return userRepository.save(user);
    }
}