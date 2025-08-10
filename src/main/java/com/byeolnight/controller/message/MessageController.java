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
@Tag(name = "쪽지", description = "쪽지 관련 API")
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @Operation(summary = "쪽지 전송", description = "새로운 쪽지를 전송합니다.")
    public ResponseEntity<CommonResponse<MessageDto.Response>> sendMessage(
            @RequestBody MessageDto.SendRequest request,
            @AuthenticationPrincipal User user
    ) {
        MessageDto.Response response = messageService.sendMessage(user.getId(), request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @GetMapping("/received")
    @Operation(summary = "받은 쪽지함", description = "받은 쪽지 목록을 조회합니다.")
    public ResponseEntity<CommonResponse<MessageDto.ListResponse>> getReceivedMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        MessageDto.ListResponse response = messageService.getReceivedMessages(user.getId(), pageable);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @GetMapping("/sent")
    @Operation(summary = "보낸 쪽지함", description = "보낸 쪽지 목록을 조회합니다.")
    public ResponseEntity<CommonResponse<MessageDto.ListResponse>> getSentMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        MessageDto.ListResponse response = messageService.getSentMessages(user.getId(), pageable);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "쪽지 상세 조회", description = "특정 쪽지의 상세 정보를 조회합니다.")
    public ResponseEntity<CommonResponse<MessageDto.Response>> getMessage(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        MessageDto.Response response = messageService.getMessage(id, user.getId());
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "쪽지 읽음 처리", description = "쪽지를 읽음 상태로 변경합니다.")
    public ResponseEntity<CommonResponse<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
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
    @Operation(summary = "쪽지 삭제", description = "쪽지를 삭제합니다.")
    public ResponseEntity<CommonResponse<Void>> deleteMessage(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        messageService.deleteMessage(id, user.getId());
        return ResponseEntity.ok(CommonResponse.success());
    }
}