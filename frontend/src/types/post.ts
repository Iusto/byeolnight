// src/types/post.ts
export type PostDetail = {
  id: number;
  title: string;
  content: string;
  writer: string;
  createdAt: string;
  category: string;
  likeCount: number;
  likedByMe: boolean;
  blinded: boolean;
};

export const __FIX_EXPORT__ = null;
