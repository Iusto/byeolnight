// 관리자 기능 관련 타입 정의

export interface AdminAction {
  id: string;
  adminId: string;
  targetUserId: string;
  actionType: 'MESSAGE_BLIND' | 'CHAT_BAN' | 'USER_WARN';
  reason?: string;
  duration?: number; // 분 단위
  timestamp: string;
}

export interface ChatBanInfo {
  userId: string;
  username: string;
  bannedUntil: string;
  reason?: string;
  bannedBy: string;
}

export interface BlindedMessage {
  messageId: string;
  originalMessage: string;
  blindedBy: string;
  blindedAt: string;
  reason?: string;
}

export interface AdminChatStats {
  totalMessages: number;
  blindedMessages: number;
  bannedUsers: number;
  activeUsers: number;
}