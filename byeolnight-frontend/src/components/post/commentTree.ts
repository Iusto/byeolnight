export interface PostComment {
  id: number;
  content: string;
  writer: string;
  writerId?: number;
  createdAt: string;
  likeCount: number;
  reportCount: number;
  isPopular: boolean;
  blinded: boolean;
  deleted: boolean;
  writerIcon?: string;
  writerCertificates?: string[];
  parentId?: number;
  parentWriter?: string;
  children?: PostComment[];
}

/** 여러 단계의 답글을 루트 댓글별 평면 목록으로 묶어 현재 UI 구조에 맞춘다. */
export function organizeComments(comments: PostComment[]): PostComment[] {
  const roots = comments.filter(comment => !comment.parentId);
  const replies = comments.filter(comment => comment.parentId);
  const byId = new Map(comments.map(comment => [comment.id, comment]));

  const findRootId = (comment: PostComment): number => {
    let current = comment;
    const visited = new Set<number>();
    while (current.parentId && !visited.has(current.id)) {
      visited.add(current.id);
      const parent = byId.get(current.parentId);
      if (!parent) break;
      current = parent;
    }
    return current.id;
  };

  return roots.map(root => ({
    ...root,
    children: replies.filter(reply => findRootId(reply) === root.id),
  }));
}
