export interface SendMessageRequest {
  receiverId: number;
  title: string;
  content: string;
}

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

export interface UnreadCountResponse {
  count: number;
}
