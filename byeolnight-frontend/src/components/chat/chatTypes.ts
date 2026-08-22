export interface ChatMessage {
  id?: string;
  sender: string;
  senderIcon?: string;
  message: string;
  timestamp: string;
  isBlinded?: boolean;
}

export interface BanStatus {
  banned: boolean;
  reason?: string;
  duration?: number;
  bannedUntil?: string;
}
