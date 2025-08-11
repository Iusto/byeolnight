package com.byeolnight.controller.message;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.dto.message.MessageDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.service.message.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/messages")
@RequiredArgsConstructor
@Tag(name = "💌 회원 API - 쪽지", description = "1:1 개인 메시지 시스템 API")
@SecurityRequirement(name = "bearerAuth")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @Operation(summary = "쪽지 전송", description = "특정 사용자에게 새로운 쪽지를 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (받는 사람 없음 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<CommonResponse<MessageDto.Response>> sendMessage(
            @RequestBody @Valid MessageDto.SendRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        MessageDto.Response response = messageService.sendMessage(user.getId(), request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @GetMapping("/received")
    @Operation(summary = "받은 쪽지함", description = "받은 쪽지 목록을 페이징으로 조회합니다.")
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
            @Parameter(name = "size", description = "페이지 크기 (최대 50)", example = "20")
    })
    public ResponseEntity<CommonResponse<MessageDto.ListResponse>> getReceivedMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        MessageDto.ListResponse response = messageService.getReceivedMessages(user.getId(), pageable);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @GetMapping("/sent")
    @Operation(summary = "보낸 쪽지함", description = "보낸 쪽지 목록을 페이징으로 조회합니다.")
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
            @Parameter(name = "size", description = "페이지 크기 (최대 50)", example = "20")
    })
    public ResponseEntity<CommonResponse<MessageDto.ListResponse>> getSentMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        MessageDto.ListResponse response = messageService.getSentMessages(user.getId(), pageable);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "쪽지 상세 조회", description = "특정 쪽지의 상세 정보를 조회하고 자동으로 읽음 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인 쪽지가 아님)"),
            @ApiResponse(responseCode = "404", description = "쪽지 없음")
    })
    public ResponseEntity<CommonResponse<MessageDto.Response>> getMessage(
            @Parameter(description = "쪽지 ID", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        MessageDto.Response response = messageService.getMessage(id, user.getId());
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "쪽지 읽음 처리", description = "쪽지를 읽음 상태로 변경합니다. (상세 조회 시 자동 처리되므로 별도 호출 불필요)")
    public ResponseEntity<CommonResponse<Void>> markAsRead(
            @Parameter(description = "쪽지 ID", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        messageService.getMessage(id, user.getId()); // 읽음 처리는 getMessage에서 자동 처리
        return ResponseEntity.ok(CommonResponse.success());
    }

    @GetMapping("/unread/count")
    @Operation(summary = "읽지 않은 쪽지 개수", description = "읽지 않은 쪽지의 개수를 조회합니다.")
    public ResponseEntity<CommonResponse<MessageDto.UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal User user
    ) {
        long count = messageService.getUnreadCount(user.getId());
        
        MessageDto.UnreadCountResponse response = MessageDto.UnreadCountResponse.builder()
                .count(count)
                .build();
        
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "쪽지 삭제", description = "쪽지를 소프트 삭제합니다. (완전 삭제가 아닌 숨김 처리)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "쪽지 없음")
    })
    public ResponseEntity<CommonResponse<Void>> deleteMessage(
            @Parameter(description = "쪽지 ID", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        messageService.deleteMessage(id, user.getId());
        return ResponseEntity.ok(CommonResponse.success());
    }
}