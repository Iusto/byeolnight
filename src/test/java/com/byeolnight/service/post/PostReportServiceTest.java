package com.byeolnight.service.post;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.post.*;
import com.byeolnight.domain.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostReportServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PostRepository postRepository;
    @Mock private PostReportRepository postReportRepository;
    @Mock private PostBlindLogRepository postBlindLogRepository;

    @InjectMocks
    private PostReportService postReportService;

    @Test
    @DisplayName("신고 10개 도달 시 블라인드 처리")
    void reportPost_becomesBlinded() {
        // given
        User reporter = User.builder()
                .nickname("신고자")
                .email("reporter@example.com")
                .password("pass")
                .phone("01012345678")
                .role(User.Role.USER)
                .build();
        ReflectionTestUtils.setField(reporter, "id", 2L);

        Post post = Post.builder()
                .title("테스트 게시글")
                .content("내용")
                .category(Post.Category.DISCUSSION)
                .writer(reporter)
                .build();
        ReflectionTestUtils.setField(post, "id", 1L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(reporter));
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postReportRepository.existsByUserAndPost(reporter, post)).thenReturn(false);
        when(postReportRepository.countByPost(post)).thenReturn(10L); // ✅ 기준 도달
        when(postBlindLogRepository.existsByPostId(post.getId())).thenReturn(false); // ✅ 메서드 수정

        // when
        postReportService.reportPost(2L, 1L, "도배");

        // then
        assertThat(post.isBlinded()).isTrue();
        verify(postBlindLogRepository).save(any());        // 블라인드 로그 저장 확인
        verify(postReportRepository).save(any());          // 신고 저장 확인
    }
}
