package com.byeolnight.controller.post;

import com.byeolnight.common.TestSecurityConfig;
import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.infrastructure.security.JwtAuthenticationFilter;
import com.byeolnight.infrastructure.security.SecurityConfig;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import com.byeolnight.service.post.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = PublicPostController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
        }
)
@Import(TestSecurityConfig.class)
@DisplayName("PublicPostController 테스트")
class PublicPostControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean PostService postService;

    @Nested
    @DisplayName("게시글 목록 조회 GET /api/public/posts")
    class GetPosts {

        @Test
        @DisplayName("비로그인 상태에서 200 반환")
        void unauthenticated_returns200() throws Exception {
            Page<PostResponseDto> emptyPage = new PageImpl<>(List.of());
            when(postService.getFilteredPosts(any(), any(), any(), any(), any(), isNull()))
                    .thenReturn(emptyPage);

            mockMvc.perform(get("/api/public/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("카테고리 필터 적용 시 200 반환")
        void withCategoryFilter_returns200() throws Exception {
            Page<PostResponseDto> emptyPage = new PageImpl<>(List.of());
            when(postService.getFilteredPosts(any(), any(), any(), any(), any(), isNull()))
                    .thenReturn(emptyPage);

            mockMvc.perform(get("/api/public/posts")
                            .param("category", "NEWS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("검색어 포함 요청 시 200 반환")
        void withSearchParam_returns200() throws Exception {
            Page<PostResponseDto> emptyPage = new PageImpl<>(List.of());
            when(postService.getFilteredPosts(any(), any(), any(), any(), any(), isNull()))
                    .thenReturn(emptyPage);

            mockMvc.perform(get("/api/public/posts")
                            .param("searchType", "title")
                            .param("search", "우주"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("게시글 단건 조회 GET /api/public/posts/{id}")
    class GetPostById {

        @Test
        @DisplayName("존재하는 게시글 조회 시 200 반환")
        void existingPost_returns200() throws Exception {
            PostResponseDto postResponse = mock(PostResponseDto.class);
            when(postService.getPostById(1L, null)).thenReturn(postResponse);

            mockMvc.perform(get("/api/public/posts/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("인기 게시글 조회 GET /api/public/posts/hot")
    class GetHotPosts {

        @Test
        @DisplayName("200과 목록 반환")
        void returns200WithList() throws Exception {
            when(postService.getTopHotPostsAcrossAllCategories(6)).thenReturn(List.of());

            mockMvc.perform(get("/api/public/posts/hot"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
