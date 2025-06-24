package com.byeolnight.controller;

import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.service.post.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/posts")
@SecurityRequirement(name = "BearerAuth")
public class AdminPostController {

    private final PostService postService;

    @Operation(summary = "블라인드 게시글 전체 조회", description = "관리자가 블라인드 처리된 게시글 목록을 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/blinded")
    public ResponseEntity<List<PostResponseDto>> getBlindedPosts() {
        List<PostResponseDto> result = postService.getBlindedPosts();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "게시글 블라인드 처리", description = "관리자가 특정 게시글을 블라인드 처리합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{postId}/blind")
    public ResponseEntity<Void> blindPost(@PathVariable Long postId) {
        postService.blindPost(postId);
        return ResponseEntity.ok().build();
    }
}
