package com.byeolnight.controller.post;

import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.service.post.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<List<PostResponseDto>>> getBlindedPosts() {
        List<PostResponseDto> result = postService.getBlindedPosts();
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success(result));
    }


    @Operation(summary = "게시글 블라인드 처리", description = "관리자가 문제가 있는 게시글을 블라인드 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "블라인드 처리 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{postId}/blind")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> blindPost(@PathVariable Long postId) {
        postService.blindPost(postId);
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success("블라인드 처리가 완료되었습니다."));
    }

    @Operation(summary = "게시글 블라인드 해제", description = "관리자가 블라인드 처리된 게시글을 다시 공개로 전환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "블라인드 해제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{postId}/unblind")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> unblindPost(@PathVariable Long postId) {
        postService.unblindPost(postId);
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success("블라인드 해제가 완료되었습니다."));
    }

    @Operation(summary = "게시글 완전 삭제", description = "관리자가 게시글을 완전히 삭제합니다. (되돌릴 수 없음)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{postId}")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> deletePostPermanently(@PathVariable Long postId) {
        postService.deletePostPermanently(postId);
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success("게시글이 완전히 삭제되었습니다."));
    }

    @Operation(summary = "신고된 게시글 조회", description = "관리자가 신고된 게시글 목록을 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reported")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<List<PostResponseDto>>> getReportedPosts() {
        List<PostResponseDto> result = postService.getReportedPosts();
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success(result));
    }
}
