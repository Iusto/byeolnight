package com.byeolnight.controller;

import com.byeolnight.application.post.PostService;
import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.infrastructure.common.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<CommonResponse<Long>> create(@Valid @RequestBody PostRequestDto requestDto,
                                                       @AuthenticationPrincipal User user) {
        Long postId = postService.createPost(requestDto, user);
        return ResponseEntity.ok(CommonResponse.success(postId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<PostResponseDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(CommonResponse.success(postService.getPostById(id)));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<PostResponseDto>>> list(Pageable pageable) {
        return ResponseEntity.ok(CommonResponse.success(postService.getAllPosts(pageable)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> update(@PathVariable Long id,
                                                       @Valid @RequestBody PostRequestDto requestDto,
                                                       @AuthenticationPrincipal User user) {
        postService.updatePost(id, requestDto, user);
        return ResponseEntity.ok(CommonResponse.success());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable Long id,
                                                       @AuthenticationPrincipal User user) {
        postService.softDeletePost(id, user);
        return ResponseEntity.ok(CommonResponse.success());
    }
}