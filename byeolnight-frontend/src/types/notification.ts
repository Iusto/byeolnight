// 알림 관련 타입 정의
export interface Notification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  targetUrl?: string;
  relatedId?: number;
  isRead: boolean;
  createdAt: string;
}

export type NotificationType = 
  | 'COMMENT_ON_POST'    // 내 게시글에 댓글
  | 'REPLY_ON_COMMENT'   // 내 댓글에 답글
  | 'NEW_MESSAGE'        // 새 쪽지
  | 'NEW_NOTICE';        // 새 공지사항

export interface NotificationListResponse {
  notifications: Notification[];
  totalCount: number;
  currentPage: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export const NOTIFICATION_TYPES = {
  COMMENT_ON_POST: '댓글 알림',
  REPLY_ON_COMMENT: '답글 알림',
  NEW_MESSAGE: '쪽지 알림',
  NEW_NOTICE: '공지사항'
} as const;

export const NOTIFICATION_ICONS = {
  COMMENT_ON_POST: '💬',
  REPLY_ON_COMMENT: '↩️',
  NEW_MESSAGE: '📩',
  NEW_NOTICE: '📢'
} as const;