export interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  writerIcon?: string;
  blinded: boolean;
  blindType?: string;
  likeCount: number;
  likedByMe: boolean;
  hot: boolean;
  commentCount: number;
  viewCount: number;
  createdAt: string;
  updatedAt: string;
  dDay?: string;
  thumbnailUrl?: string;
}
