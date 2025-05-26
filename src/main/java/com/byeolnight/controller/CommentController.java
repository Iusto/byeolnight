package com.byeolnight.controller;

import com.byeolnight.application.comment.CommentService;
import com.byeolnight.dto.comment.CommentRequestDto;
import com.byeolnight.dto.comment.CommentResponseDto;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.infrastructure.common.CommonResponse;
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

    @PostMapping
    public ResponseEntity<CommonResponse<Long>> create(@Valid @RequestBody CommentRequestDto dto,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(CommonResponse.success(commentService.create(dto, user)));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<CommonResponse<List<CommentResponseDto>>> getByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(CommonResponse.success(commentService.getByPostId(postId)));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommonResponse<Void>> update(@PathVariable Long commentId,
                                                       @Valid @RequestBody CommentRequestDto dto,
                                                       @AuthenticationPrincipal User user) {
        commentService.update(commentId, dto, user);
        return ResponseEntity.ok(CommonResponse.success());
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable Long commentId,
                                                       @AuthenticationPrincipal User user) {
        commentService.delete(commentId, user);
        return ResponseEntity.ok(CommonResponse.success());
    }
}