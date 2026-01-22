package com.byeolnight.service.post;

import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.comment.CommentRepository;
import com.byeolnight.repository.file.FileRepository;
import com.byeolnight.repository.post.PostLikeRepository;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.repository.user.UserRepository;
import com.byeolnight.service.assembler.PostResponseAssembler;
import com.byeolnight.service.certificate.CertificateService;
import com.byeolnight.service.file.S3Service;
import com.byeolnight.service.log.DeleteLogService;
import com.byeolnight.service.notification.NotificationService;
import com.byeolnight.service.user.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService 블라인드 게시글 처리 테스트")
class PostServiceBlindTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private CertificateService certificateService;

    @Mock
    private PointService pointService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DeleteLogService deleteLogService;

    @Mock
    private PostResponseAssembler postResponseAssembler;

    @InjectMocks
    private PostService postService;

    private User normalUser;
    private User adminUser;
    private Post normalPost;
    private Post blindedPost;

    @BeforeEach
    void setUp() {
        // 일반 사용자
        normalUser = User.builder()
                .email("user@test.com")
                .nickname("일반사용자")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build();
        setUserId(normalUser, 1L);

        // 관리자
        adminUser = User.builder()
                .email("admin@test.com")
                .nickname("관리자")
                .role(User.Role.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .build();
        setUserId(adminUser, 2L);

        // 일반 게시글
        normalPost = Post.builder()
                .title("일반 게시글")
                .content("내용")
                .category(Post.Category.FREE)
                .writer(normalUser)
                .build();
        setPostId(normalPost, 1L);

        // 블라인드 게시글
        blindedPost = Post.builder()
                .title("블라인드 게시글")
                .content("내용")
                .category(Post.Category.FREE)
                .writer(normalUser)
                .build();
        setPostId(blindedPost, 2L);
        blindedPost.blindByAdmin(adminUser.getId());
    }

    private void setUserId(User user, Long id) {
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setPostId(Post post, Long id) {
        try {
            java.lang.reflect.Field idField = Post.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(post, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("일반 사용자가 게시글 목록 조회 시 블라인드 게시글이 포함되지 않아야 함")
    void getFilteredPosts_WhenNormalUser_ShouldExcludeBlindedPosts() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> posts = List.of(normalPost); // 블라인드 제외
        Page<Post> postPage = new PageImpl<>(posts, pageable, posts.size());

        PostResponseDto normalPostDto = PostResponseDto.builder()
                .id(1L)
                .title("일반 게시글")
                .blinded(false)
                .build();

        when(postRepository.findByIsDeletedFalseAndCategoryOrderByCreatedAtDesc(
                eq(Post.Category.FREE), eq(pageable))).thenReturn(postPage);
        when(postRepository.findHotPosts(any(), any(), anyInt(), anyInt(), eq(false))).thenReturn(List.of());
        when(postLikeRepository.countByPost(any())).thenReturn(0L);
        when(commentRepository.countByPostId(any())).thenReturn(0L);
        when(postResponseAssembler.toDto(eq(normalPost), anyBoolean(), anyLong(), anyBoolean(), anyLong()))
                .thenReturn(normalPostDto);

        // when
        Page<PostResponseDto> result = postService.getFilteredPosts("FREE", "recent", null, null, pageable, normalUser);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("일반 게시글");
        assertThat(result.getContent().stream().noneMatch(PostResponseDto::isBlinded)).isTrue();
    }

    @Test
    @DisplayName("관리자가 게시글 목록 조회 시 블라인드 게시글이 포함되어야 함")
    void getFilteredPosts_WhenAdmin_ShouldIncludeBlindedPosts() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> posts = List.of(normalPost, blindedPost); // 블라인드 포함
        Page<Post> postPage = new PageImpl<>(posts, pageable, posts.size());

        PostResponseDto normalPostDto = PostResponseDto.builder()
                .id(1L)
                .title("일반 게시글")
                .blinded(false)
                .build();
        PostResponseDto blindedPostDto = PostResponseDto.builder()
                .id(2L)
                .title("블라인드 게시글")
                .blinded(true)
                .build();

        when(postRepository.findByIsDeletedFalseAndCategoryOrderByCreatedAtDesc(
                eq(Post.Category.FREE), eq(pageable))).thenReturn(postPage);
        when(postRepository.findHotPosts(any(), any(), anyInt(), anyInt(), eq(true))).thenReturn(List.of());
        when(postLikeRepository.countByPost(any())).thenReturn(0L);
        when(commentRepository.countByPostId(any())).thenReturn(0L);
        when(postResponseAssembler.toDto(eq(normalPost), anyBoolean(), anyLong(), anyBoolean(), anyLong()))
                .thenReturn(normalPostDto);
        when(postResponseAssembler.toDto(eq(blindedPost), anyBoolean(), anyLong(), anyBoolean(), anyLong()))
                .thenReturn(blindedPostDto);

        // when
        Page<PostResponseDto> result = postService.getFilteredPosts("FREE", "recent", null, null, pageable, adminUser);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().stream().anyMatch(PostResponseDto::isBlinded)).isTrue();
    }

    @Test
    @DisplayName("비로그인 사용자가 게시글 목록 조회 시 블라인드 게시글이 포함되지 않아야 함")
    void getFilteredPosts_WhenNotLoggedIn_ShouldExcludeBlindedPosts() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> posts = List.of(normalPost); // 블라인드 제외
        Page<Post> postPage = new PageImpl<>(posts, pageable, posts.size());

        PostResponseDto normalPostDto = PostResponseDto.builder()
                .id(1L)
                .title("일반 게시글")
                .blinded(false)
                .build();

        when(postRepository.findByIsDeletedFalseAndCategoryOrderByCreatedAtDesc(
                eq(Post.Category.FREE), eq(pageable))).thenReturn(postPage);
        when(postRepository.findHotPosts(any(), any(), anyInt(), anyInt(), eq(false))).thenReturn(List.of());
        when(postLikeRepository.countByPost(any())).thenReturn(0L);
        when(commentRepository.countByPostId(any())).thenReturn(0L);
        when(postResponseAssembler.toDto(eq(normalPost), anyBoolean(), anyLong(), anyBoolean(), anyLong()))
                .thenReturn(normalPostDto);

        // when
        Page<PostResponseDto> result = postService.getFilteredPosts("FREE", "recent", null, null, pageable, null);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("일반 게시글");
        assertThat(result.getContent().stream().noneMatch(PostResponseDto::isBlinded)).isTrue();
    }
}
