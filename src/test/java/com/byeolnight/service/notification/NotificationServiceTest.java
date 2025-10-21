package com.byeolnight.service.notification;

import com.byeolnight.dto.notification.NotificationDto;
import com.byeolnight.entity.Notification;
import com.byeolnight.entity.user.User;
import com.byeolnight.repository.NotificationRepository;
import com.byeolnight.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("알림 목록 조회")
    void getNotifications() {
        User user = User.builder().email("test@test.com").build();
        Notification noti = Notification.builder()
                .user(user)
                .type(Notification.NotificationType.COMMENT_ON_POST)
                .title("test")
                .message("test message")
                .build();
        Page<Notification> page = new PageImpl<>(Arrays.asList(noti));
        
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(notificationRepository.findByUserOrderByCreatedAtDesc(any(), any())).willReturn(page);

        NotificationDto.ListResponse result = notificationService.getNotifications(1L, PageRequest.of(0, 10));

        assertThat(result.getNotifications()).hasSize(1);
        assertThat(result.getTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("읽지 않은 알림 조회")
    void getUnreadNotifications() {
        User user = User.builder().email("test@test.com").build();
        Notification noti = Notification.builder()
                .user(user)
                .type(Notification.NotificationType.NEW_MESSAGE)
                .title("test")
                .build();
        
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(any()))
                .willReturn(Arrays.asList(noti));

        var result = notificationService.getUnreadNotifications(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("읽지 않은 알림 개수")
    void getUnreadCount() {
        User user = User.builder().email("test@test.com").build();
        
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(notificationRepository.countByUserAndIsReadFalse(any())).willReturn(3L);

        long result = notificationService.getUnreadCount(1L);

        assertThat(result).isEqualTo(3L);
    }

    @Test
    @DisplayName("알림 읽음 처리")
    void markAsRead() {
        User user = User.builder().email("test@test.com").build();
        
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        notificationService.markAsRead(1L, 1L);

        verify(notificationRepository).markAsReadByIdAndUser(1L, user);
    }

    @Test
    @DisplayName("모든 알림 읽음 처리")
    void markAllAsRead() {
        User user = User.builder().email("test@test.com").build();
        
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        notificationService.markAllAsRead(1L);

        verify(notificationRepository).markAllAsReadByUser(user);
    }

    @Test
    @DisplayName("알림 생성")
    void createNotification() {
        User user = User.builder().email("test@test.com").build();
        Notification noti = Notification.builder()
                .user(user)
                .type(Notification.NotificationType.COMMENT_ON_POST)
                .title("test")
                .build();
        
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(notificationRepository.save(any())).willReturn(noti);

        notificationService.createNotification(1L, Notification.NotificationType.COMMENT_ON_POST, 
                "test", "message", "/posts/1", 1L);

        verify(notificationRepository).save(any());
    }
}
