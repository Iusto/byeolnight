package com.byeolnight.controller.admin;

import com.byeolnight.dto.admin.DiscussionStatusDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.discussion.DiscussionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/discussions")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "👮 관리자 API - 토론", description = "토론 주제 관리 API")
public class AdminDiscussionController {

    private final DiscussionService discussionService;

    @Operation(summary = "토론 시스템 상태 조회", description = "토론 시스템의 현재 상태를 조회합니다.")
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<DiscussionStatusDto>> getDiscussionStatus() {
        return ResponseEntity.ok(CommonResponse.success(discussionService.getDiscussionStatus()));
    }

    @Operation(summary = "토론 주제 수동 생성", description = "관리자가 수동으로 토론 주제를 생성합니다.")
    @PostMapping("/generate-topic")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<String>> generateDiscussionTopic(
            @AuthenticationPrincipal User admin
    ) {
        discussionService.generateDiscussionTopicManually(admin);
        return ResponseEntity.ok(CommonResponse.success("토론 주제가 성공적으로 생성되었습니다."));
    }
}