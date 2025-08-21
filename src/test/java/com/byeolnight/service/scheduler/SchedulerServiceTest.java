package com.byeolnight.service.scheduler;

import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.PostCleanupScheduler;
import com.byeolnight.service.discussion.DiscussionTopicScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    
    @InjectMocks private PostCleanupScheduler postCleanupScheduler;
    @InjectMocks private DiscussionTopicScheduler discussionTopicScheduler;

    @Test
    @DisplayName("게시글 정리 스케줄러 - 만료된 게시글 없을 때")
    void testPostCleanupWithNoExpiredPosts() {
        // Given
        when(postRepository.findExpiredDeletedPosts(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        
        // When & Then
        assertThatCode(() -> postCleanupScheduler.cleanupExpiredPosts())
                .doesNotThrowAnyException();
        
        verify(postRepository).findExpiredDeletedPosts(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("토론 주제 스케줄러 - 시스템 사용자 생성")
    void testSystemUserCreation() {
        // Given
        User systemUser = User.builder()
                .email("system@byeolnight.com")
                .nickname("별 헤는 밤")
                .password("password")
                .role(User.Role.ADMIN)
                .build();
        
        when(userRepository.findByEmail("system@byeolnight.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenReturn(systemUser);
        
        // When
        User result = discussionTopicScheduler.getSystemUser();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("system@byeolnight.com");
        assertThat(result.getNickname()).isEqualTo("별 헤는 밤");
        assertThat(result.getRole()).isEqualTo(User.Role.ADMIN);
        
        verify(userRepository).findByEmail("system@byeolnight.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("토론 주제 스케줄러 - 기존 시스템 사용자 조회")
    void testExistingSystemUserRetrieval() {
        // Given
        User existingUser = User.builder()
                .email("system@byeolnight.com")
                .nickname("별 헤는 밤")
                .password("password")
                .role(User.Role.ADMIN)
                .build();
        
        when(userRepository.findByEmail("system@byeolnight.com"))
                .thenReturn(Optional.of(existingUser));
        
        // When
        User result = discussionTopicScheduler.getSystemUser();
        
        // Then
        assertThat(result).isEqualTo(existingUser);
        verify(userRepository).findByEmail("system@byeolnight.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("토론 주제 비활성화 - 빈 목록 처리")
    void testDeactivateOldTopicsWithEmptyList() {
        // Given
        when(postRepository.findByDiscussionTopicTrueAndPinnedTrue())
                .thenReturn(Collections.emptyList());
        
        // When & Then
        assertThatCode(() -> discussionTopicScheduler.deactivateOldTopics())
                .doesNotThrowAnyException();
        
        verify(postRepository).findByDiscussionTopicTrueAndPinnedTrue();
        verify(postRepository).saveAll(Collections.emptyList());
    }

    @Test
    @DisplayName("토론 주제 내용 파싱 - 정상 케이스")
    void testTopicContentParsing() {
        // Given
        String content = "제목: 우주 탐사의 미래\n내용: 화성 이주 계획에 대해 어떻게 생각하시나요?";
        
        // When
        String[] result = discussionTopicScheduler.parseTopicContent(content);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result[0]).isEqualTo("우주 탐사의 미래");
        assertThat(result[1]).isEqualTo("화성 이주 계획에 대해 어떻게 생각하시나요?");
    }

    @Test
    @DisplayName("토론 주제 내용 파싱 - 실패 시 기본값")
    void testTopicContentParsingFallback() {
        // Given
        String invalidContent = "잘못된 형식";
        
        // When
        String[] result = discussionTopicScheduler.parseTopicContent(invalidContent);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result[0]).isEqualTo("오늘의 우주 토론");
        assertThat(result[1]).isEqualTo("우주에 대한 흥미로운 주제로 토론해보세요.");
    }

    @Test
    @DisplayName("토론 주제 내용 파싱 - 길이 제한")
    void testTopicContentLengthLimit() {
        // Given
        String longTitle = "a".repeat(50);
        String longContent = "b".repeat(300);
        String content = "제목: " + longTitle + "\n내용: " + longContent;
        
        // When
        String[] result = discussionTopicScheduler.parseTopicContent(content);
        
        // Then
        assertThat(result[0]).hasSize(30);
        assertThat(result[0]).endsWith("...");
        assertThat(result[1]).hasSize(200);
        assertThat(result[1]).endsWith("...");
    }

    private Post createMockPost(String title) {
        Post post = mock(Post.class);
        when(post.getTitle()).thenReturn(title);
        return post;
    }
}