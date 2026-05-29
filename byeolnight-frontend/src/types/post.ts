// 게시글 관련 공용 타입 정의 (단일 출처)
//
// 주의: 백엔드 응답이 용도에 따라 두 가지 형태로 내려온다.
//  - Post: 공개/목록/상세 API (`writer`, `blinded`)
//  - ActivityPost: 내 활동 요약 API (`writerNickname`, `isBlinded`)
// 필드명이 서로 다르므로 의도적으로 분리해 둔다.

/** 공개/목록 게시글 (public/posts, posts/hot 등) */
export interface Post {
  id: number;
  title: string;
  content: string;
  category?: string;
  writer: string;
  writerId?: number;
  writerIcon?: string;
  blinded?: boolean;
  blindType?: string;
  likeCount: number;
  likedByMe?: boolean;
  hot?: boolean;
  commentCount: number;
  viewCount: number;
  createdAt?: string;
  updatedAt?: string;
}

/** 게시글 상세 (posts/:id) — 공개 게시글에 인증서/이미지 정보가 추가됨 */
export interface PostDetail extends Post {
  writerCertificates?: string[];
  images?: Array<{
    id: number;
    originalName: string;
    url: string;
  }>;
}

/** 내 활동 요약에 포함되는 게시글 (member/users/my-activity) */
export interface ActivityPost {
  id: number;
  title: string;
  content: string;
  category: string;
  writerNickname: string;
  likeCount: number;
  commentCount: number;
  viewCount: number;
  isBlinded: boolean;
  createdAt: string;
}
