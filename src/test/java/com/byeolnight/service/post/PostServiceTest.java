package com.byeolnight.service.post;

import com.byeolnight.domain.entity.post.Post;
import com.byeolnight.domain.entity.post.Post.Category;
import com.byeolnight.domain.entity.post.PostLike;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.post.FileRepository;
import com.byeolnight.domain.repository.post.PostLikeRepository;
import com.byeolnight.domain.repository.post.PostRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.infrastructure.exception.NotFoundException;
import com.byeolnight.service.file.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private FileRepository fileRepository;
    @Mock private UserRepository userRepository;
    @Mock private S3Service s3Service;

    @InjectMocks private PostService postService;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("nickname")
                .build();

        post = Post.builder()
                .title("제목")
                .content("내용")
                .category(Category.NEWS)
                .writer(user)
                .build();
    }

    @Test
    @DisplayName("게시글 작성 성공")
    void createPost_success() {
        // given
        User writer = User.builder().id(1L).nickname("tester").email("tester@byeol.com").build();
        PostRequestDto dto = PostRequestDto.builder()
                .title("제목")
                .content("내용")
                .category(Category.NEWS)
                .images(List.of())
                .build();

        // save 호출 시, 전달 객체에 ID 주입해서 리턴
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        // when
        Long savedId = postService.createPost(dto, writer);

        // then
        assertThat(savedId).isEqualTo(1L);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_success() {
        PostRequestDto dto = new PostRequestDto("수정된 제목", "수정된 내용", Category.IMAGE, new ArrayList<>());
        given(postRepository.findById(anyLong())).willReturn(Optional.of(post));

        postService.updatePost(1L, dto, user);

        assertThat(post.getTitle()).isEqualTo("수정된 제목");
        assertThat(post.getCategory()).isEqualTo(Category.IMAGE);
    }

    @Test
    @DisplayName("게시글 수정 실패 - 작성자 불일치")
    void updatePost_fail_notAuthor() {
        User other = User.builder().id(2L).email("other@example.com").build();
        given(postRepository.findById(anyLong())).willReturn(Optional.of(post));

        PostRequestDto dto = new PostRequestDto("제목", "내용", Category.NEWS, new ArrayList<>());
        assertThrows(IllegalArgumentException.class, () -> postService.updatePost(1L, dto, other));
    }

    @Test
    @DisplayName("게시글 조회 실패 - 존재하지 않음")
    void getPostById_notFound() {
        given(postRepository.findWithWriterById(anyLong())).willReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> postService.getPostById(1L, null));
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_success() {
        given(postRepository.findById(anyLong())).willReturn(Optional.of(post));

        postService.deletePost(1L, user);

        assertThat(post.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 작성자 불일치")
    void deletePost_fail_notAuthor() {
        User other = User.builder().id(3L).email("x@x.com").build();
        given(postRepository.findById(anyLong())).willReturn(Optional.of(post));

        assertThrows(IllegalArgumentException.class, () -> postService.deletePost(1L, other));
    }

    @Test
    @DisplayName("게시글 추천 성공")
    void likePost_success() {
        given(postRepository.findById(anyLong())).willReturn(Optional.of(post));
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(postLikeRepository.existsByUserAndPost(any(), any())).willReturn(false);

        postService.likePost(1L, 1L);

        then(postLikeRepository).should().save(any(PostLike.class));
    }

    @Test
    @DisplayName("게시글 추천 실패 - 이미 추천")
    void likePost_fail_alreadyLiked() {
        given(postRepository.findById(anyLong())).willReturn(Optional.of(post));
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(postLikeRepository.existsByUserAndPost(user, post)).willReturn(true);

        assertThrows(IllegalArgumentException.class, () -> postService.likePost(1L, 1L));
    }

    @Test
    @DisplayName("블라인드 게시글 목록 조회")
    void getBlindedPosts_success() {
        List<Post> posts = List.of(post);
        given(postRepository.findByIsDeletedFalseAndBlindedTrueOrderByCreatedAtDesc()).willReturn(posts);

        List<PostResponseDto> result = postService.getBlindedPosts();

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("게시글 블라인드 처리 성공")
    void blindPost_success() {
        given(postRepository.findById(anyLong())).willReturn(Optional.of(post));

        postService.blindPost(1L);

        assertThat(post.isBlinded()).isTrue();
    }
}
