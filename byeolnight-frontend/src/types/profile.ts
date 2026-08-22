import type { Message } from './message';
import type { ActivityPost } from './post';

export type ProfileTab =
  | 'info'
  | 'posts'
  | 'comments'
  | 'icons'
  | 'messages'
  | 'notifications';

export interface ProfileComment {
  id: number;
  postId: number;
  postTitle: string;
  content: string;
  writerNickname: string;
  parentId?: number;
  isBlinded: boolean;
  createdAt: string;
}

export interface MyActivityData {
  myPosts: ActivityPost[];
  myComments: ProfileComment[];
  receivedMessages: { messages: Message[]; totalCount: number };
  sentMessages: { messages: Message[]; totalCount: number };
  totalPostCount: number;
  totalCommentCount: number;
  totalReceivedMessageCount: number;
  totalSentMessageCount: number;
  postsCurrentPage: number;
  postsTotalPages: number;
  postsHasNext: boolean;
  postsHasPrevious: boolean;
  commentsCurrentPage: number;
  commentsTotalPages: number;
  commentsHasNext: boolean;
  commentsHasPrevious: boolean;
}

export interface UserIcon {
  id: number;
  iconId: number;
  name: string;
  iconUrl: string;
  price: number;
  purchasedAt: string;
  equipped: boolean;
}

export interface UserProfile {
  id: number;
  email: string;
  nickname: string;
  role: string;
  nicknameChanged: boolean;
  nicknameUpdatedAt?: string;
  points: number;
  attendanceCount: number;
  ownedIcons?: UserIcon[];
  equippedIcon?: UserIcon;
}

export const categoryLabels: Record<string, string> = {
  NEWS: '뉴스',
  DISCUSSION: '토론',
  IMAGE: '사진',
  EVENT: '행사',
  REVIEW: '후기',
  FREE: '자유',
  NOTICE: '공지',
};
