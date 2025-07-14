package com.byeolnight.service.notification;

import com.byeolnight.domain.entity.Notification;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.domain.repository.NotificationRepository;
import com.byeolnight.domain.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 서비스 테스트")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private NotificationService notificationService;

    private User user;
    private Notification notification;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .nickname("testuser")
                .password("password")
                .phone("01012345678")
                .role(User.Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        notification = Notification.builder()
                .user(user)
                .title("테스트 알림")
                .message("테스트 메시지")
                .type(Notification.NotificationType.COMMENT)
                .build();
        ReflectionTestUtils.setField(notification, "id", 1L);
    }

    @Test
    @DisplayName("알림 생성 성공")
    void createNotification_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.createNotification(1L, "제목", "내용", Notification.NotificationType.COMMENT, null);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("사용자 알림 목록 조회")
    void getUserNotifications_success() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification));
        
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageRequest)).thenReturn(page);

        Page<Notification> result = notificationService.getUserNotifications(1L, pageRequest);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("테스트 알림");
    }

    @Test
    @DisplayName("읽지 않은 알림 개수 조회")
    void getUnreadCount_success() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(1L);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("알림 읽음 처리")
    void markAsRead_success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L, 1L);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("모든 알림 읽음 처리")
    void markAllAsRead_success() {
        notificationService.markAllAsRead(1L);

        verify(notificationRepository).markAllAsReadByUserId(1L);
    }
}