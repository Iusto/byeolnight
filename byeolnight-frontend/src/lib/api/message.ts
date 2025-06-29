import { MessageDto, PageResponse } from '../../types/message';

const API_BASE = '/api/messages';

export const messageApi = {
  // 쪽지 전송
  sendMessage: async (data: MessageDto.Request): Promise<MessageDto.Response> => {
    const response = await fetch(API_BASE, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify(data)
    });
    
    if (!response.ok) throw new Error('쪽지 전송 실패');
    return response.json();
  },

  // 받은 쪽지 목록
  getReceivedMessages: async (page = 0, size = 10): Promise<PageResponse<MessageDto.Summary>> => {
    const response = await fetch(`${API_BASE}/received?page=${page}&size=${size}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    if (!response.ok) throw new Error('받은 쪽지 목록 조회 실패');
    return response.json();
  },

  // 보낸 쪽지 목록
  getSentMessages: async (page = 0, size = 10): Promise<PageResponse<MessageDto.Summary>> => {
    const response = await fetch(`${API_BASE}/sent?page=${page}&size=${size}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    if (!response.ok) throw new Error('보낸 쪽지 목록 조회 실패');
    return response.json();
  },

  // 쪽지 상세 조회
  getMessage: async (messageId: number): Promise<MessageDto.Response> => {
    const response = await fetch(`${API_BASE}/${messageId}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    if (!response.ok) throw new Error('쪽지 조회 실패');
    return response.json();
  },

  // 쪽지 읽음 처리
  markAsRead: async (messageId: number): Promise<void> => {
    const response = await fetch(`${API_BASE}/${messageId}/read`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    if (!response.ok) throw new Error('쪽지 읽음 처리 실패');
  },

  // 쪽지 삭제
  deleteMessage: async (messageId: number): Promise<void> => {
    const response = await fetch(`${API_BASE}/${messageId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    if (!response.ok) throw new Error('쪽지 삭제 실패');
  },

  // 읽지 않은 쪽지 수
  getUnreadCount: async (): Promise<number> => {
    const response = await fetch(`${API_BASE}/unread-count`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    
    if (!response.ok) throw new Error('읽지 않은 쪽지 수 조회 실패');
    return response.json();
  }
};