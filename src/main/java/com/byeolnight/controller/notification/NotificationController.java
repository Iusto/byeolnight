package com.byeolnight.controller.notification;

import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.dto.notification.NotificationDto;
import com.byeolnight.entity.user.User;
import com.byeolnight.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member/notifications")
@RequiredArgsConstructor
@Tag(name = "알림", description = "알림 관련 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "사용자의 알림 목록을 조회합니다.")
    public ResponseEntity<CommonResponse<NotificationDto.ListResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user
    ) {
        System.out.println("알림 목록 조회 요청 - userId: " + user.getId());
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        NotificationDto.ListResponse response = notificationService.getNotifications(user.getId(), pageable);
        System.out.println("조회된 알림 수: " + response.getNotifications().size());
        
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @GetMapping("/unread")
    @Operation(summary = "읽지 않은 알림 조회", description = "읽지 않은 알림 목록을 조회합니다.")
    public ResponseEntity<CommonResponse<List<NotificationDto.Response>>> getUnreadNotifications(
            @AuthenticationPrincipal User user
    ) {
        System.out.println("읽지 않은 알림 조회 요청 - userId: " + user.getId());
        
        List<NotificationDto.Response> response = notificationService.getUnreadNotifications(user.getId());
        System.out.println("읽지 않은 알림 수: " + response.size());
        
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "읽지 않은 알림 개수", description = "읽지 않은 알림의 개수를 조회합니다.")
    public ResponseEntity<CommonResponse<NotificationDto.UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal User user
    ) {
        System.out.println("읽지 않은 알림 개수 조회 요청 - userId: " + user.getId());
        
        long count = notificationService.getUnreadCount(user.getId());
        System.out.println("읽지 않은 알림 개수: " + count);
        
        NotificationDto.UnreadCountResponse response = NotificationDto.UnreadCountResponse.builder()
                .count(count)
                .build();
        
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    public ResponseEntity<CommonResponse<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        System.out.println("알림 읽음 처리 요청 - notificationId: " + id + ", userId: " + user.getId());
        
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(CommonResponse.success());
    }

    @PutMapping("/read-all")
    @Operation(summary = "모든 알림 읽음 처리", description = "모든 알림을 읽음 처리합니다.")
    public ResponseEntity<CommonResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal User user
    ) {
        System.out.println("모든 알림 읽음 처리 요청 - userId: " + user.getId());
        
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(CommonResponse.success());
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.")
    public ResponseEntity<CommonResponse<Void>> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        System.out.println("알림 삭제 요청 - notificationId: " + id + ", userId: " + user.getId());
        
        notificationService.deleteNotification(id, user.getId());
        return ResponseEntity.ok(CommonResponse.success());
    }
    

}