import axios from '../axios';
import type { ApiResponse } from '../../types/api';
import type {
  Message,
  MessageListResponse,
  SendMessageRequest,
  UnreadCountResponse,
} from '../../types/message';

export type { Message, MessageListResponse } from '../../types/message';

export const getReceivedMessages = async (params: {
  page?: number;
  size?: number;
}): Promise<MessageListResponse> => {
  const response = await axios.get<ApiResponse<MessageListResponse>>(
    '/member/messages/received',
    { params },
  );
  return response.data.data;
};

export const getSentMessages = async (params: {
  page?: number;
  size?: number;
}): Promise<MessageListResponse> => {
  const response = await axios.get<ApiResponse<MessageListResponse>>(
    '/member/messages/sent',
    { params },
  );
  return response.data.data;
};

export const getMessage = async (messageId: number): Promise<Message> => {
  const response = await axios.get<ApiResponse<Message>>(`/member/messages/${messageId}`);
  return response.data.data;
};

export const markMessageAsRead = async (messageId: number): Promise<void> => {
  await axios.patch(`/member/messages/${messageId}/read`);
};

export const sendMessage = async (data: SendMessageRequest): Promise<Message> => {
  const response = await axios.post<ApiResponse<Message>>('/member/messages', data);
  return response.data.data;
};

export const deleteMessage = async (messageId: number): Promise<void> => {
  await axios.delete(`/member/messages/${messageId}`);
};

export const getUnreadMessageCount = async (): Promise<number> => {
  const response = await axios.get<ApiResponse<UnreadCountResponse>>(
    '/member/messages/unread/count',
  );
  return response.data.data.count;
};
