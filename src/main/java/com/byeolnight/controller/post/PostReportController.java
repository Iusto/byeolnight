package com.byeolnight.controller.post;

import com.byeolnight.dto.post.PostReportRequestDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.post.PostReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/posts")
@Tag(name = "📝 게시글 API - 신고", description = "게시글 신고 및 신고 처리 API")
public class PostReportController {

    private final PostReportService postReportService;

    /**
     * 게시글 신고 API
     */
    @Operation(summary = "게시글 신고", description = "게시글을 신고합니다. 중복 신고는 불가하며, 일정 횟수 초과 시 블라인드 처리됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고 성공",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "이미 신고한 게시글",
                    content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{postId}/report")
    public ResponseEntity<CommonResponse<?>> reportPost(
            @PathVariable Long postId,
            @RequestBody PostReportRequestDto dto,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        try {
            postReportService.reportPost(user.getId(), postId, dto.getReason(), dto.getDescription());
            return ResponseEntity.ok(CommonResponse.success("신고가 접수되었습니다."));
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("이미 신고한")) {
                return ResponseEntity.status(409).body(CommonResponse.error("이미 신고한 게시글입니다."));
            }
            throw e;
        }
    }
}
