package com.byeolnight.controller.comment;

import com.byeolnight.service.comment.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/comments")
@SecurityRequirement(name = "BearerAuth")
public class AdminCommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 블라인드 처리", description = "관리자가 부적절한 댓글을 블라인드 처리합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{commentId}/blind")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> blindComment(@PathVariable Long commentId) {
        commentService.blindComment(commentId);
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success("댓글이 블라인드 처리되었습니다."));
    }

    @Operation(summary = "댓글 블라인드 해제", description = "관리자가 블라인드된 댓글을 해제합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{commentId}/unblind")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> unblindComment(@PathVariable Long commentId) {
        commentService.unblindComment(commentId);
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success("댓글 블라인드가 해제되었습니다."));
    }

    @Operation(summary = "댓글 완전 삭제", description = "관리자가 댓글을 완전히 삭제합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<com.byeolnight.infrastructure.common.CommonResponse<String>> deleteComment(@PathVariable Long commentId) {
        commentService.deleteCommentPermanently(commentId);
        return ResponseEntity.ok(com.byeolnight.infrastructure.common.CommonResponse.success("댓글이 완전히 삭제되었습니다."));
    }
}