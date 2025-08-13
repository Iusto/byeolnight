package com.byeolnight.service.scheduler;

import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.PostCleanupScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchedulerPerformanceTest {

    @Autowired private PostCleanupScheduler postCleanupScheduler;
    @Autowired private PostRepository postRepository;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("대량 데이터 정리 성능 테스트")
    void testBulkCleanupPerformance() {
        // Given: 대량 테스트 데이터 생성
        User testUser = createTestUser();
        List<Post> expiredPosts = createBulkExpiredPosts(testUser, 100);
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // When: 정리 작업 실행
        postCleanupScheduler.cleanupExpiredPosts();
        
        stopWatch.stop();
        
        // Then: 성능 검증 (5초 이내)
        assertThat(stopWatch.getTotalTimeMillis()).isLessThan(5000);
        
        // 데이터 정리 확인
        for (Post post : expiredPosts) {
            assertThat(postRepository.existsById(post.getId())).isFalse();
        }
    }

    @Test
    @DisplayName("스케줄러 메모리 사용량 테스트")
    void testSchedulerMemoryUsage() {
        User testUser = createTestUser();
        createBulkExpiredPosts(testUser, 50);
        
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        postCleanupScheduler.cleanupExpiredPosts();
        
        System.gc(); // 가비지 컬렉션 강제 실행
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        // 메모리 증가량이 10MB 이내인지 확인
        long memoryIncrease = memoryAfter - memoryBefore;
        assertThat(memoryIncrease).isLessThan(10 * 1024 * 1024); // 10MB
    }

    private User createTestUser() {
        return userRepository.save(User.builder()
                .email("perf@test.com")
                .nickname("성능테스트유저")
                .password("password")
                .role(User.Role.USER)
                .build());
    }

    private List<Post> createBulkExpiredPosts(User user, int count) {
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Post post = Post.builder()
                    .title("만료된 게시글 " + i)
                    .content("내용 " + i)
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
            posts.add(post);
        }
        return postRepository.saveAll(posts);
    }
}