package com.byeolnight.controller.post;

import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.service.post.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberPostController.class)
@DisplayName("MemberPostController 테스트")
@Disabled("테스트 컨텍스트 로딩 문제로 임시 비활성화")
class MemberPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("test@user.com").nickname("testUser").build();
    }

    @Test
    @WithMockUser
    @DisplayName("게시글 생성 - 성공")
    void createPost_Success_ReturnsPostId() throws Exception {
        // Given
        PostRequestDto requestDto = PostRequestDto.builder().title("새 게시글").content("내용").category(Post.Category.FREE).build();
        given(postService.createPost(any(PostRequestDto.class), any(User.class))).willReturn(1L);

        // When & Then
        mockMvc.perform(post("/api/member/posts")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(1L));
    }

    @Test
    @WithMockUser
    @DisplayName("게시글 수정 - 성공")
    void updatePost_Success_ReturnsOk() throws Exception {
        // Given
        Long postId = 1L;
        PostRequestDto requestDto = PostRequestDto.builder().title("수정된 게시글").content("수정된 내용").category(Post.Category.FREE).build();
        doNothing().when(postService).updatePost(eq(postId), any(PostRequestDto.class), any(User.class));

        // When & Then
        mockMvc.perform(put("/api/member/posts/{id}", postId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("게시글 삭제 - 성공")
    void deletePost_Success_ReturnsOk() throws Exception {
        // Given
        Long postId = 1L;
        doNothing().when(postService).deletePost(eq(postId), any(User.class));

        // When & Then
        mockMvc.perform(delete("/api/member/posts/{id}", postId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("게시글 추천 - 성공")
    void likePost_Success_ReturnsOk() throws Exception {
        // Given
        Long postId = 1L;
        doNothing().when(postService).likePost(any(), eq(postId));

        // When & Then
        mockMvc.perform(post("/api/member/posts/{postId}/like", postId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("추천 완료"));
    }
}