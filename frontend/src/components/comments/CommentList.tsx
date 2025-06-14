import { Comment } from "@/types/comment";
import CommentItem from "./CommentItem";

interface Props {
  comments: Comment[];
  onReplySubmit: (content: string, parentId?: number) => void;
  depth?: number;
}

export default function CommentList({ comments, onReplySubmit, depth = 0 }: Props) {
  return (
    <div className={depth > 0 ? "pl-6 border-l" : ""}>
      {comments.map((comment) => (
        <div key={comment.id} className="mb-4">
          <CommentItem
            comment={comment}
            onReplySubmit={onReplySubmit}
          />
          {comment.children && comment.children.length > 0 && (
            <CommentList
              comments={comment.children}
              onReplySubmit={onReplySubmit}
              depth={depth + 1}
            />
          )}
        </div>
      ))}
    </div>
  );
}
