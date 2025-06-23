package com.byeolnight.service.post;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.post.PostRepository;
import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.service.file.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private S3Service s3Service;

    @InjectMocks
    private PostService postService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = User.builder()
                .id(1L)
                .email("tester@example.com")
                .nickname("tester")
                .password("pass")
                .phone("01012345678")
                .role(User.Role.USER)
                .build();
    }

    @Test
    @DisplayName("게시글 생성 - 제목 필드 검증")
    void createPost_success() {
        // given
        PostRequestDto dto = new PostRequestDto("제목", "내용", Post.Category.NEWS, "tester");

        // when
        postService.createPost(dto, user, Collections.emptyList());

        // then
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());

        Post saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("제목");
        assertThat(saved.getContent()).isEqualTo("내용");
        assertThat(saved.getCategory()).isEqualTo(Post.Category.NEWS);
        assertThat(saved.getWriter()).isEqualTo(user);
    }
}
