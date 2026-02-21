package com.byeolnight.controller.post;

import com.byeolnight.common.TestSecurityConfig;
import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.entity.post.Post;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.security.JwtAuthenticationFilter;
import com.byeolnight.infrastructure.security.SecurityConfig;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import com.byeolnight.service.post.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = MemberPostController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
        }
)
@Import(TestSecurityConfig.class)
@DisplayName("MemberPostController 테스트")
class MemberPostControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean PostService postService;

    private final User mockUser = User.builder()
            .email("test@test.com")
            .nickname("tester")
            .password("encodedPassword")
            .role(User.Role.USER)
            .status(User.UserStatus.ACTIVE)
            .build();

    private PostRequestDto validPostRequest() {
        return PostRequestDto.builder()
                .title("테스트 제목")
                .content("테스트 내용입니다.")
                .category(Post.Category.FREE)
                .build();
    }

    @Nested
    @DisplayName("게시글 생성 POST /api/member/posts")
    class CreatePost {

        @Test
        @DisplayName("비로그인 시 401 반환")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/member/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPostRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("로그인 상태에서 게시글 작성 시 200 반환")
        void authenticated_returns200() throws Exception {
            when(postService.createPost(any(), any())).thenReturn(1L);

            mockMvc.perform(post("/api/member/posts")
                            .with(user(mockUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPostRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(1));
        }

        @Test
        @DisplayName("제목 없이 요청 시 400 반환")
        void missingTitle_returns400() throws Exception {
            String bodyWithoutTitle = "{\"content\":\"내용\",\"category\":\"FREE\"}";

            mockMvc.perform(post("/api/member/posts")
                            .with(user(mockUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithoutTitle))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("게시글 수정 PUT /api/member/posts/{id}")
    class UpdatePost {

        @Test
        @DisplayName("비로그인 시 401 반환")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(put("/api/member/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPostRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("로그인 상태에서 게시글 수정 시 200 반환")
        void authenticated_returns200() throws Exception {
            doNothing().when(postService).updatePost(eq(1L), any(), any());

            mockMvc.perform(put("/api/member/posts/1")
                            .with(user(mockUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validPostRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("게시글 삭제 DELETE /api/member/posts/{id}")
    class DeletePost {

        @Test
        @DisplayName("비로그인 시 401 반환")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete("/api/member/posts/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("로그인 상태에서 게시글 삭제 시 200 반환")
        void authenticated_returns200() throws Exception {
            doNothing().when(postService).deletePost(eq(1L), any());

            mockMvc.perform(delete("/api/member/posts/1")
                            .with(user(mockUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("게시글 추천 POST /api/member/posts/{postId}/like")
    class LikePost {

        @Test
        @DisplayName("비로그인 시 401 반환")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/member/posts/1/like"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("로그인 상태에서 추천 시 200 반환")
        void authenticated_returns200() throws Exception {
            doNothing().when(postService).likePost(any(), eq(1L));

            mockMvc.perform(post("/api/member/posts/1/like")
                            .with(user(mockUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
