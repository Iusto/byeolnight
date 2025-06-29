import { NotificationDto } from '../../types/notification';
import { PageResponse } from '../../types/message';

const API_BASE = '/api/notifications';

export const notificationApi = {
  // 알림 목록 조회
  getNotifications: async (page = 0, size = 10): Promise<PageResponse<NotificationDto.Response>> => {
    const response = await fetch(`${API_BASE}?page=${page}&size=${size}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    if (!response.ok) throw new Error('알림 목록 조회 실패');
    return response.json();
  },

  // 알림 읽음 처리
  markAsRead: async (notificationId: number): Promise<void> => {
    const response = await fetch(`${API_BASE}/${notificationId}/read`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    if (!response.ok) throw new Error('알림 읽음 처리 실패');
  },

  // 알림 삭제
  deleteNotification: async (notificationId: number): Promise<void> => {
    const response = await fetch(`${API_BASE}/${notificationId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    if (!response.ok) throw new Error('알림 삭제 실패');
  },

  // 모든 알림 삭제
  clearAllNotifications: async (): Promise<void> => {
    const response = await fetch(`${API_BASE}/clear`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    if (!response.ok) throw new Error('알림 전체 삭제 실패');
  },

  // 읽지 않은 알림 수
  getUnreadCount: async (): Promise<number> => {
    const response = await fetch(`${API_BASE}/unread-count`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    if (!response.ok) throw new Error('읽지 않은 알림 수 조회 실패');
    return response.json();
  }
};