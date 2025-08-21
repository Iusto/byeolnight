import axios from '../axios';

export interface Message {
  id: number;
  senderId: number;
  senderNickname: string;
  receiverId: number;
  receiverNickname: string;
  title: string;
  content: string;
  isRead: boolean;
  readAt?: string;
  createdAt: string;
}

export interface MessageListResponse {
  messages: Message[];
  totalCount: number;
  currentPage: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// 받은 쪽지 목록 조회
export const getReceivedMessages = async (params: {
  page?: number;
  size?: number;
}): Promise<MessageListResponse> => {
  const response = await axios.get('/member/messages/received', { params });
  console.log('getReceivedMessages response:', response.data);
  
  // axios 인터셉터가 이미 data를 추출했는지 확인
  let result;
  if (response.data && response.data.messages) {
    // 인터셉터가 이미 data를 추출한 경우
    result = response.data;
  } else if (response.data && response.data.data) {
    // 일반적인 CommonResponse 구조
    result = response.data.data;
  } else {
    result = { messages: [], totalCount: 0, currentPage: 0, totalPages: 0, hasNext: false, hasPrevious: false };
  }
  
  console.log('getReceivedMessages final result:', result);
  return result;
};

// 보낸 쪽지 목록 조회
export const getSentMessages = async (params: {
  page?: number;
  size?: number;
}): Promise<MessageListResponse> => {
  const response = await axios.get('/member/messages/sent', { params });
  console.log('getSentMessages response:', response.data);
  
  // axios 인터셉터가 이미 data를 추출했는지 확인
  let result;
  if (response.data && response.data.messages) {
    // 인터셉터가 이미 data를 추출한 경우
    result = response.data;
  } else if (response.data && response.data.data) {
    // 일반적인 CommonResponse 구조
    result = response.data.data;
  } else {
    result = { messages: [], totalCount: 0, currentPage: 0, totalPages: 0, hasNext: false, hasPrevious: false };
  }
  
  console.log('getSentMessages final result:', result);
  return result;
};

// 쪽지 상세 조회
export const getMessage = async (messageId: number): Promise<Message> => {
  const response = await axios.get(`/member/messages/${messageId}`);
  return response.data.data;
};

// 쪽지 읽음 처리
export const markMessageAsRead = async (messageId: number): Promise<void> => {
  await axios.patch(`/member/messages/${messageId}/read`);
};

// 쪽지 전송
export const sendMessage = async (data: {
  receiverId: number;
  title: string;
  content: string;
}): Promise<Message> => {
  const response = await axios.post('/member/messages', data);
  return response.data.data;
};

// 쪽지 삭제
export const deleteMessage = async (messageId: number): Promise<void> => {
  await axios.delete(`/member/messages/${messageId}`);
};

// 읽지 않은 쪽지 개수
export const getUnreadMessageCount = async (): Promise<number> => {
  const response = await axios.get('/member/messages/unread/count');
  return response.data.data?.count || 0;
};