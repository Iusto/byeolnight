export namespace MessageDto {
  export interface Request {
    receiverId: number;
    title: string;
    content: string;
  }

  export interface Response {
    id: number;
    senderId: number;
    senderNickname: string;
    receiverId: number;
    receiverNickname: string;
    title: string;
    content: string;
    isRead: boolean;
    createdAt: string;
  }

  export interface Summary {
    id: number;
    senderNickname: string;
    title: string;
    isRead: boolean;
    createdAt: string;
  }
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}