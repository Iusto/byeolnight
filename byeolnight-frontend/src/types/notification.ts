export namespace NotificationDto {
  export interface Response {
    id: number;
    type: 'MESSAGE' | 'COMMENT' | 'REPLY';
    title: string;
    content: string;
    referenceId: number;
    isRead: boolean;
    createdAt: string;
  }

  export interface Settings {
    messageNotification: boolean;
    commentNotification: boolean;
    replyNotification: boolean;
  }
}