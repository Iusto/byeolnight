package com.byeolnight.service.post;

import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.post.PostRepository;
import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.dto.post.PostResponseDto;
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

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService 테스트")
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    
    @InjectMocks
    private PostService postService;
    
    private User testUser;
    private Post testPost;
    private PostRequestDto postRequestDto;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("password")
                .nickname("테스트유저")
                .phone("01012345678")
                .role(User.Role.USER)
                .build();
                
        testPost = Post.builder()
                .title("테스트 게시글")
                .content("테스트 내용입니다.")
                .category(Post.Category.FREE)
                .writer(testUser)
                .build();
                
        postRequestDto = PostRequestDto.builder()
                .title("새 게시글")
                .content("새 게시글 내용")
                .category(Post.Category.FREE)
                .build();
    }
    
    @Test
    @DisplayName("게시글 생성 성공 테스트")
    void 게시글_생성_성공() {
        // Given
        given(postRepository.save(any(Post.class))).willReturn(testPost);
        
        // When
        Long savedPostId = postService.createPost(postRequestDto, testUser);
        
        // Then
        assertThat(savedPostId).isNotNull();
        verify(postRepository).save(any(Post.class));
    }
    
    @Test
    @DisplayName("게시글 목록 조회 성공 테스트")
    void 게시글_목록_조회_성공() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> postPage = new PageImpl<>(Arrays.asList(testPost));
        given(postRepository.findAll(pageable)).willReturn(postPage);
        
        // When
        Page<PostResponseDto> result = postService.getFilteredPosts("FREE", "recent", pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("테스트 게시글");
    }
    
    @Test
    @DisplayName("게시글 상세 조회 성공 테스트")
    void 게시글_상세_조회_성공() {
        // Given
        given(postRepository.findById(anyLong())).willReturn(Optional.of(testPost));
        
        // When
        PostResponseDto foundPost = postService.getPostById(1L, testUser);
        
        // Then
        assertThat(foundPost.getTitle()).isEqualTo("테스트 게시글");
        assertThat(foundPost.getContent()).isEqualTo("테스트 내용입니다.");
    }
    
    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 예외 발생")
    void 존재하지_않는_게시글_조회_실패() {
        // Given
        given(postRepository.findById(anyLong())).willReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> postService.getPostById(999L, testUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("게시글 삭제 성공 테스트")
    void 게시글_삭제_성공() {
        // Given
        given(postRepository.findById(anyLong())).willReturn(Optional.of(testPost));
        
        // When
        postService.deletePost(1L, testUser);
        
        // Then
        verify(postRepository).findById(1L);
    }
    
    @Test
    @DisplayName("권한 없는 사용자의 게시글 삭제 시 예외 발생")
    void 권한_없는_게시글_삭제_실패() {
        // Given
        User otherUser = User.builder()
                .email("other@example.com")
                .password("password")
                .nickname("다른유저")
                .phone("01087654321")
                .role(User.Role.USER)
                .build();
                
        given(postRepository.findById(anyLong())).willReturn(Optional.of(testPost));
        
        // When & Then
        assertThatThrownBy(() -> postService.deletePost(1L, otherUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("삭제 권한이 없습니다");
    }
}