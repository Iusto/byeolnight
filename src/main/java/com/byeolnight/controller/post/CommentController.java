package com.byeolnight.controller.post;

import com.byeolnight.service.comment.CommentService;
import com.byeolnight.dto.comment.CommentRequestDto;
import com.byeolnight.dto.comment.CommentResponseDto;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.infrastructure.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 생성", description = "게시글에 댓글을 작성합니다.")
    @ApiResponse(responseCode = "200", description = "생성된 댓글 ID 반환")
    @PostMapping
    public ResponseEntity<CommonResponse<Long>> create(@Valid @RequestBody CommentRequestDto dto,
                                                       @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(CommonResponse.success(commentService.create(dto, user)));
    }

    @Operation(summary = "게시글 댓글 목록 조회", description = "특정 게시글에 달린 모든 댓글을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "댓글 목록 반환")
    @GetMapping("/post/{postId}")
    public ResponseEntity<CommonResponse<List<CommentResponseDto>>> getByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(CommonResponse.success(commentService.getByPostId(postId)));
    }

    @Operation(summary = "댓글 수정", description = "본인이 작성한 댓글을 수정합니다.")
    @PutMapping("/{commentId}")
    public ResponseEntity<CommonResponse<Void>> update(@PathVariable Long commentId,
                                                       @Valid @RequestBody CommentRequestDto dto,
                                                       @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        commentService.update(commentId, dto, user);
        return ResponseEntity.ok(CommonResponse.success());
    }

    @Operation(summary = "댓글 삭제", description = "본인이 작성한 댓글을 삭제합니다.")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable Long commentId,
                                                       @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        commentService.delete(commentId, user);
        return ResponseEntity.ok(CommonResponse.success());
    }
}
