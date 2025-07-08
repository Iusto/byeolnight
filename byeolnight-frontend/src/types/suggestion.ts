// 건의사항 관련 타입 정의
export interface Suggestion {
  id: number;
  title: string;
  content: string;
  category: SuggestionCategory;
  status: SuggestionStatus;
  authorId: number;
  authorNickname: string;
  authorIcon?: string;
  createdAt: string;
  updatedAt: string;
  adminResponse?: string;
  adminResponseAt?: string;
  adminNickname?: string;
}

export type SuggestionCategory = 
  | 'FEATURE'      // 기능 개선
  | 'BUG'          // 버그 신고
  | 'UI_UX'        // UI/UX 개선
  | 'CONTENT'      // 콘텐츠 관련
  | 'OTHER';       // 기타

export type SuggestionStatus = 
  | 'PENDING'      // 검토 중
  | 'IN_PROGRESS'  // 진행 중
  | 'COMPLETED'    // 완료
  | 'REJECTED';    // 거절

export interface CreateSuggestionRequest {
  title: string;
  content: string;
  category: SuggestionCategory;
}

export interface SuggestionListResponse {
  suggestions: Suggestion[];
  totalCount: number;
  currentPage: number;
  totalPages: number;
}

export const SUGGESTION_CATEGORIES = {
  FEATURE: '기능 개선',
  BUG: '버그 신고',
  UI_UX: 'UI/UX 개선',
  CONTENT: '콘텐츠 관련',
  OTHER: '기타'
} as const;

export const SUGGESTION_STATUS = {
  PENDING: '검토 중',
  IN_PROGRESS: '진행 중',
  COMPLETED: '완료',
  REJECTED: '거절'
} as const;