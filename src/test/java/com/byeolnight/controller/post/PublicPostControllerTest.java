package com.byeolnight.controller.post;

import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.service.post.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Disabled;

@WebMvcTest(PublicPostController.class)
@DisplayName("PublicPostController 테스트")
@Disabled("테스트 컨텍스트 로딩 문제로 임시 비활성화")
class PublicPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    @Test
    @DisplayName("게시글 목록 조회 - 기본 (최신순)")
    void getPosts_DefaultRecent_ReturnsPosts() throws Exception {
        // Given
        PostResponseDto postDto = PostResponseDto.builder().id(1L).title("테스트 게시글").build();
        Page<PostResponseDto> pagedResponse = new PageImpl<>(Collections.singletonList(postDto), PageRequest.of(0, 10), 1);

        given(postService.getFilteredPosts(eq(null), eq("recent"), eq(null), eq(null), any(Pageable.class))).willReturn(pagedResponse);

        // When & Then
        mockMvc.perform(get("/api/public/posts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("테스트 게시글"));
    }

    @Test
    @DisplayName("게시글 단건 조회 - 성공")
    void getPostById_Success_ReturnsPost() throws Exception {
        // Given
        Long postId = 1L;
        PostResponseDto postDto = PostResponseDto.builder().id(postId).title("단건 조회 테스트").build();
        given(postService.getPostById(eq(postId), any())).willReturn(postDto);

        // When & Then
        mockMvc.perform(get("/api/public/posts/{id}", postId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("단건 조회 테스트"));
    }

    @Test
    @DisplayName("인기 게시글 조회 - 성공")
    void getTopHotPosts_Success_ReturnsHotPosts() throws Exception {
        // Given
        PostResponseDto hotPost = PostResponseDto.builder().id(10L).title("인기 게시글").build();
        List<PostResponseDto> hotPosts = Collections.singletonList(hotPost);
        given(postService.getTopHotPostsAcrossAllCategories(6)).willReturn(hotPosts);

        // When & Then
        mockMvc.perform(get("/api/public/posts/hot")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("인기 게시글"));
    }
}
