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
@RequestMapping("/api/member/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 생성", description = "게시글에 댓글을 작성합니다.")
    @ApiResponse(responseCode = "200", description = "생성된 댓글 ID 반환")
    @PostMapping
    public ResponseEntity<CommonResponse<Long>> create(@Valid @RequestBody CommentRequestDto dto,
                                                       @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        System.out.println("CommentController.create 호출");
        System.out.println("요청 데이터: " + dto.getPostId() + ", " + dto.getContent());
        System.out.println("인증된 사용자: " + (user != null ? user.getNickname() + "(ID: " + user.getId() + ")" : "null"));
        
        if (user == null) {
            System.err.println("사용자 인증 실패!");
            return ResponseEntity.status(401).body(CommonResponse.error("로그인이 필요합니다."));
        }
        
        return ResponseEntity.ok(CommonResponse.success(commentService.create(dto, user)));
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
