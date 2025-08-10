import axios from '../axios';
import type { Notification, NotificationListResponse } from '../../types/notification';

// 알림 목록 조회
export const getNotifications = async (params: {
  page?: number;
  size?: number;
}): Promise<NotificationListResponse> => {
  const response = await axios.get('/member/notifications', { params });
  
  // axios 인터셉터가 이미 data를 추출했는지 확인
  let result;
  if (response.data && response.data.notifications) {
    result = response.data;
  } else if (response.data && response.data.data && response.data.data.notifications) {
    result = response.data.data;
  } else {
    result = { notifications: [], totalCount: 0, currentPage: 0, totalPages: 0, hasNext: false, hasPrevious: false };
  }
  
  return result;
};

// 읽지 않은 알림 조회
export const getUnreadNotifications = async (): Promise<Notification[]> => {
  const response = await axios.get('/member/notifications/unread');
  
  // axios 인터셉터가 이미 data를 추출했는지 확인
  let result;
  if (Array.isArray(response.data)) {
    result = response.data;
  } else if (response.data && response.data.data) {
    result = response.data.data;
  } else {
    result = [];
  }
  
  return result || [];
};

// 읽지 않은 알림 개수
export const getUnreadCount = async (): Promise<number> => {
  const response = await axios.get('/member/notifications/unread/count');
  
  let count;
  if (typeof response.data === 'object' && response.data.count !== undefined) {
    count = response.data.count;
  } else if (response.data && response.data.data && response.data.data.count !== undefined) {
    count = response.data.data.count;
  } else {
    count = 0;
  }
  
  return count || 0;
};

// 알림 읽음 처리
export const markAsRead = async (id: number): Promise<void> => {
  await axios.put(`/member/notifications/${id}/read`);
};

// 모든 알림 읽음 처리
export const markAllAsRead = async (): Promise<void> => {
  await axios.put('/member/notifications/read-all');
};

// 알림 삭제
export const deleteNotification = async (id: number): Promise<void> => {
  await axios.delete(`/member/notifications/${id}`);
};