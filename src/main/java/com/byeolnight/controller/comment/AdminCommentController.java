package com.byeolnight.controller.comment;

import com.byeolnight.entity.user.User;
import com.byeolnight.service.post.PostAdminService;
import com.byeolnight.service.comment.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "👮 관리자 API - 댓글", description = "댓글 관리 API")
public class AdminCommentController {

    private final PostAdminService postAdminService;
    private final CommentService commentService;

    @Operation(summary = "블라인드 댓글 목록 조회", description = "관리자가 블라인드 처리된 댓글 목록을 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/comments/blinded")
    public ResponseEntity<List<com.byeolnight.dto.comment.CommentResponseDto>> getBlindedComments(
            org.springframework.security.core.Authentication authentication) {
        User currentUser =
            (User) authentication.getPrincipal();
        List<com.byeolnight.dto.comment.CommentResponseDto> comments = commentService.getBlindedComments(currentUser);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "댓글 블라인드 처리", description = "관리자가 댓글을 블라인드 처리합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/comments/{commentId}/blind")
    public ResponseEntity<Void> blindComment(@PathVariable Long commentId) {
        commentService.blindComment(commentId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "댓글 블라인드 해제", description = "관리자가 블라인드 처리된 댓글을 해제합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/comments/{commentId}/unblind")
    public ResponseEntity<Void> unblindComment(@PathVariable Long commentId) {
        commentService.unblindComment(commentId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "게시글의 모든 댓글 조회 (관리자용)", description = "관리자가 특정 게시글의 모든 댓글(삭제/블라인드 포함)을 원본 내용으로 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<com.byeolnight.dto.comment.CommentResponseDto>> getPostCommentsForAdmin(
            @PathVariable Long postId,
            org.springframework.security.core.Authentication authentication) {
        User currentUser =
            (User) authentication.getPrincipal();
        List<com.byeolnight.dto.comment.CommentResponseDto> comments = commentService.getPostCommentsForAdmin(postId, currentUser);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "삭제된 댓글 목록 조회", description = "관리자가 삭제된 댓글 목록을 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/comments/deleted")
    public ResponseEntity<List<com.byeolnight.dto.comment.CommentResponseDto>> getDeletedComments(
            org.springframework.security.core.Authentication authentication) {
        User currentUser =
            (User) authentication.getPrincipal();
        List<com.byeolnight.dto.comment.CommentResponseDto> comments = commentService.getDeletedComments(currentUser);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "댓글 복구", description = "관리자가 삭제된 댓글을 복구합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/comments/{commentId}/restore")
    public ResponseEntity<Void> restoreComment(@PathVariable Long commentId) {
        postAdminService.restoreComment(commentId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "삭제된 게시글 목록 조회", description = "관리자가 삭제된 게시글 목록을 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/posts/deleted")
    public ResponseEntity<List<com.byeolnight.dto.post.PostAdminDto>> getDeletedPosts() {
        List<com.byeolnight.dto.post.PostAdminDto> posts = postAdminService.getDeletedPosts();
        return ResponseEntity.ok(posts);
    }

    @Operation(summary = "게시글 복구", description = "관리자가 삭제된 게시글을 복구합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/posts/{postId}/restore")
    public ResponseEntity<Void> restorePost(@PathVariable Long postId) {
        postAdminService.restorePost(postId);
        return ResponseEntity.ok().build();
    }
}
