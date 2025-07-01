package com.byeolnight.controller.post;

import com.byeolnight.service.comment.CommentService;
import com.byeolnight.dto.comment.CommentResponseDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/comments")
@RequiredArgsConstructor
public class PublicCommentController {

    private final CommentService commentService;

    @Operation(summary = "게시글 댓글 목록 조회", description = "특정 게시글에 달린 모든 댓글을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "댓글 목록 반환")
    @GetMapping("/post/{postId}")
    public ResponseEntity<CommonResponse<List<CommentResponseDto>>> getByPost(@PathVariable Long postId) {
        System.out.println("PublicCommentController.getByPost 호출 - postId: " + postId);
        List<CommentResponseDto> comments = commentService.getByPostId(postId);
        System.out.println("반환할 댓글 수: " + comments.size());
        return ResponseEntity.ok(CommonResponse.success(comments));
    }
}