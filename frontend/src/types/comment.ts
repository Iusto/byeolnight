export interface Comment {
  id: number;
  content: string;
  writer: string;
  createdAt: string;
  children: Comment[];
  parentId?: number;
}
