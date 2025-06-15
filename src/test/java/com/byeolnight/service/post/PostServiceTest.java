
package com.byeolnight.service.post;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.PostLikeRepository;
import com.byeolnight.domain.repository.PostRepository;
import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.dto.post.PostResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostServiceTest {

    @Mock
    private PostRepository repository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @InjectMocks
    private PostService service;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = User.builder()
                .nickname("tester")
                .email("tester@example.com")
                .password("pass")
                .phone("01012345678")
                .role(User.Role.USER)
                .build();

        when(postLikeRepository.existsByUserAndPost(any(), any())).thenReturn(false);
    }

    @Test
    @DisplayName("게시글을 생성한다")
    void testCreatePost() {
        PostRequestDto dto = new PostRequestDto("제목", "내용", Post.Category.NEWS);

        Post mockPost = mock(Post.class);
        when(mockPost.getId()).thenReturn(10L);
        when(repository.save(any(Post.class))).thenReturn(mockPost);

        Long postId = service.createPost(dto, user);

        assertThat(postId).isNotNull();
        assertThat(postId).isEqualTo(10L);
        verify(repository).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 단건 조회 - 존재")
    void testGetPostById() {
        Post post = Post.builder().title("제목").content("내용").category(Post.Category.IMAGE).writer(user).build();
        when(repository.findById(1L)).thenReturn(Optional.of(post));

        PostResponseDto response = service.getPostById(1L, user);

        assertThat(response.getTitle()).isEqualTo("제목");
        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("게시글 단건 조회 - 삭제됨")
    void testGetPostById_deleted() {
        Post post = Post.builder().title("제목").content("내용").category(Post.Category.IMAGE).writer(user).build();
        post.softDelete();
        when(repository.findById(1L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.getPostById(1L, user))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("게시글 목록 조회")
    void testGetAllPosts() {
        Post post = Post.builder().title("제목").content("내용").category(Post.Category.IMAGE).writer(user).build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);

        when(repository.findAllByIsDeletedFalse(pageable)).thenReturn(page);

        Page<PostResponseDto> result = service.getAllPosts(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
