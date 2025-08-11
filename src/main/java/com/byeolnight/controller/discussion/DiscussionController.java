package com.byeolnight.controller.discussion;

import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.service.discussion.DiscussionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/discussions")
@Tag(name = "💬 공개 API - 토론", description = "AI 기반 일일 토론 시스템 API")
public class DiscussionController {

    private final DiscussionService discussionService;

    @Operation(summary = "오늘의 토론 주제 조회", description = """
    AI가 생성한 오늘의 토론 주제를 조회합니다.
    
    🤖 매일 오전 9시에 Claude/OpenAI API를 통해 새로운 주제가 생성됩니다.
    💬 주제에 대한 의견은 게시글로 작성할 수 있습니다.
    """)
    @ApiResponse(responseCode = "200", description = "오늘의 토론 주제 조회 성공")
    @GetMapping("/today")
    public ResponseEntity<PostResponseDto> getTodayDiscussionTopic() {
        PostResponseDto todayTopic = discussionService.getTodayDiscussionTopic();
        return ResponseEntity.ok(todayTopic);
    }

    @Operation(summary = "토론 의견글 목록 조회", description = "특정 토론 주제에 대한 의견글 목록을 조회합니다.")
    @Parameter(name = "topicId", description = "토론 주제 게시글 ID", example = "123", required = true)
    @GetMapping("/{topicId}/opinions")
    public ResponseEntity<Page<PostResponseDto>> getOpinionPosts(
            @PathVariable Long topicId,
            @Parameter(hidden = true) Pageable pageable) {
        Page<PostResponseDto> opinions = discussionService.getOpinionPosts(topicId, pageable);
        return ResponseEntity.ok(opinions);
    }

    @Operation(summary = "토론 게시판 목록 조회", description = """
    토론 게시판의 전체 목록을 조회합니다.
    
    📝 포함 내용:
    - 오늘의 AI 생성 토론 주제
    - 사용자가 작성한 일반 토론 게시글
    - 토론 주제에 대한 의견글들
    """)
    @GetMapping
    public ResponseEntity<Page<PostResponseDto>> getDiscussionPosts(@Parameter(hidden = true) Pageable pageable) {
        Page<PostResponseDto> discussions = discussionService.getDiscussionPosts(pageable);
        return ResponseEntity.ok(discussions);
    }
}