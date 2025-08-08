package com.byeolnight.controller.post;

import com.byeolnight.service.comment.CommentService;
import com.byeolnight.service.comment.CommentReportService;
import com.byeolnight.dto.comment.CommentRequestDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/comments")
@RequiredArgsConstructor
@Tag(name = "📝 게시글 API - 댓글", description = "댓글 작성, 수정, 삭제 API")
public class CommentController {

    private final CommentService commentService;
    private final CommentReportService commentReportService;

    @Operation(summary = "댓글 생성", description = "게시글에 댓글을 작성합니다.")
    @ApiResponse(responseCode = "200", description = "생성된 댓글 ID 반환")
    @PostMapping
    public ResponseEntity<CommonResponse<Long>> create(@Valid @RequestBody CommentRequestDto dto,
                                                       @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        if (user == null) {
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
    
    @Operation(summary = "댓글 좋아요/취소", description = "댓글에 좋아요를 누르거나 취소합니다.")
    @PostMapping("/{commentId}/like")
    public ResponseEntity<CommonResponse<Boolean>> toggleLike(@PathVariable Long commentId,
                                                              @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        boolean liked = commentService.toggleCommentLike(commentId, user);
        return ResponseEntity.ok(CommonResponse.success(liked));
    }
    
    @Operation(summary = "댓글 신고", description = "부적절한 댓글을 신고합니다.")
    @PostMapping("/{commentId}/report")
    public ResponseEntity<CommonResponse<Void>> report(@PathVariable Long commentId,
                                                       @RequestParam String reason,
                                                       @RequestParam(required = false) String description,
                                                       @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(CommonResponse.error("로그인이 필요합니다."));
        }
        
        try {
            // 사용자 정보 로깅
            System.out.println("Controller - User ID: " + user.getId() + ", Email: " + user.getEmail() + ", Nickname: " + user.getNickname());
            
            // 사용자 ID를 전달하는 방식으로 변경
            commentReportService.reportCommentById(commentId, user.getId(), reason, description);
            return ResponseEntity.ok(CommonResponse.success());
        } catch (com.byeolnight.infrastructure.exception.NotFoundException e) {
            // 사용자 또는 댓글을 찾을 수 없는 경우
            return ResponseEntity.status(404).body(CommonResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            // 중복 신고 등 유효성 검사 실패
            return ResponseEntity.status(400).body(CommonResponse.error(e.getMessage()));
        } catch (RuntimeException e) {
            // RuntimeException 처리 추가
            if (e.getMessage() != null && e.getMessage().contains("이미 신고한")) {
                return ResponseEntity.status(400).body(CommonResponse.error(e.getMessage()));
            }
            e.printStackTrace();
            return ResponseEntity.status(500).body(CommonResponse.error("댓글 신고 처리 중 오류가 발생했습니다: " + e.getMessage()));
        } catch (Exception e) {
            // 예외 정보를 더 자세히 로깅
            e.printStackTrace();
            return ResponseEntity.status(500).body(CommonResponse.error("댓글 신고 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
