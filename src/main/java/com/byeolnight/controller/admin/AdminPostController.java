package com.byeolnight.controller.admin;

import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.service.post.PostService;
import com.byeolnight.service.admin.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/posts")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "👮 관리자 API - 게시글", description = "게시글 관리 및 블라인드 처리 API")
public class AdminPostController {

    private final PostService postService;
    private final AdminReportService adminReportService;

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
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> blindPost(
            @PathVariable Long postId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.byeolnight.domain.entity.user.User admin
    ) {
        postService.blindPostByAdmin(postId, admin.getId());
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

    @Operation(summary = "신고된 게시글 조회", description = "관리자가 신고된 게시글 목록을 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reported")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<List<com.byeolnight.dto.admin.ReportedPostDetailDto>>> getReportedPosts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "title") String searchType,
            Pageable pageable
    ) {
        var result = adminReportService.getReportedPosts(search, searchType, pageable);
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success(result.getContent()));
    }

    @Operation(summary = "게시글 카테고리 이동", description = "관리자가 선택한 게시글들을 다른 카테고리로 이동합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/move-category")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> movePostsCategory(
            @RequestBody MoveCategoryRequest request
    ) {
        postService.movePostsCategory(request.getPostIds(), request.getTargetCategory());
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success("카테고리 이동이 완료되었습니다."));
    }

    public static class MoveCategoryRequest {
        private java.util.List<Long> postIds;
        private String targetCategory;
        
        public java.util.List<Long> getPostIds() { return postIds; }
        public void setPostIds(java.util.List<Long> postIds) { this.postIds = postIds; }
        public String getTargetCategory() { return targetCategory; }
        public void setTargetCategory(String targetCategory) { this.targetCategory = targetCategory; }
    }
}
