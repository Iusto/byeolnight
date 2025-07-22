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
@Tag(name = "💬 토론 API", description = "일일 토론 시스템 API")
public class DiscussionController {

    private final DiscussionService discussionService;

    @Operation(summary = "오늘의 토론 주제 조회", description = "오늘의 토론 주제를 조회합니다.")
    @GetMapping("/today")
    public ResponseEntity<PostResponseDto> getTodayDiscussionTopic() {
        PostResponseDto todayTopic = discussionService.getTodayDiscussionTopic();
        return ResponseEntity.ok(todayTopic);
    }

    @Operation(summary = "토론 의견글 목록 조회", description = "특정 토론 주제에 대한 의견글 목록을 조회합니다.")
    @GetMapping("/{topicId}/opinions")
    public ResponseEntity<Page<PostResponseDto>> getOpinionPosts(
            @PathVariable Long topicId,
            Pageable pageable) {
        Page<PostResponseDto> opinions = discussionService.getOpinionPosts(topicId, pageable);
        return ResponseEntity.ok(opinions);
    }

    @Operation(summary = "토론 게시판 목록 조회", description = "토론 게시판의 전체 목록을 조회합니다. (오늘의 주제 + 일반 토론글)")
    @GetMapping
    public ResponseEntity<Page<PostResponseDto>> getDiscussionPosts(Pageable pageable) {
        Page<PostResponseDto> discussions = discussionService.getDiscussionPosts(pageable);
        return ResponseEntity.ok(discussions);
    }
}