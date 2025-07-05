import axios from '../axios';
import type { Notification, NotificationListResponse } from '../../types/notification';

// 알림 목록 조회
export const getNotifications = async (params: {
  page?: number;
  size?: number;
}): Promise<NotificationListResponse> => {
  const response = await axios.get('/member/notifications', { params });
  console.log('getNotifications response:', response.data);
  console.log('getNotifications response type:', typeof response.data);
  
  // axios 인터셉터가 이미 data를 추출했는지 확인
  let result;
  if (response.data && response.data.notifications) {
    // 인터셉터가 이미 data를 추출한 경우
    result = response.data;
  } else if (response.data && response.data.data && response.data.data.notifications) {
    // 일반적인 CommonResponse 구조
    result = response.data.data;
  } else {
    result = { notifications: [], totalCount: 0, currentPage: 0, totalPages: 0, hasNext: false, hasPrevious: false };
  }
  
  console.log('getNotifications final result:', result);
  return result;
};

// 읽지 않은 알림 조회
export const getUnreadNotifications = async (): Promise<Notification[]> => {
  const response = await axios.get('/member/notifications/unread');
  console.log('getUnreadNotifications response:', response.data);
  console.log('getUnreadNotifications response type:', typeof response.data);
  console.log('getUnreadNotifications response.data.data:', response.data.data);
  
  // axios 인터셉터가 이미 data를 추출했는지 확인
  let result;
  if (Array.isArray(response.data)) {
    // 인터셉터가 이미 data를 추출한 경우
    result = response.data;
  } else if (response.data && response.data.data) {
    // 일반적인 CommonResponse 구조
    result = response.data.data;
  } else {
    result = [];
  }
  
  console.log('getUnreadNotifications final result:', result);
  return result || [];
};

// 읽지 않은 알림 개수
export const getUnreadCount = async (): Promise<number> => {
  const response = await axios.get('/member/notifications/unread/count');
  console.log('getUnreadCount response:', response.data);
  console.log('getUnreadCount response type:', typeof response.data);
  
  let count;
  if (typeof response.data === 'object' && response.data.count !== undefined) {
    // 인터셉터가 이미 data를 추출한 경우
    count = response.data.count;
  } else if (response.data && response.data.data && response.data.data.count !== undefined) {
    // 일반적인 CommonResponse 구조
    count = response.data.data.count;
  } else {
    count = 0;
  }
  
  console.log('getUnreadCount final count:', count);
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