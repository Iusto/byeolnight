import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import CommentForm from "./CommentForm";
import { useAuth } from "../contexts/AuthContext";

export type Comment = {
  id: number;
  content: string;
  author: string;
  createdAt: string;
  parentId: number | null;
};

export default function CommentList() {
  const { id: postId } = useParams();
  const { user } = useAuth();
  const [comments, setComments] = useState<Comment[]>([]);
  const [error, setError] = useState("");

  const fetchComments = () => {
    fetch(`/api/comments/post/${postId}`)
      .then((res) => {
        if (!res.ok) throw new Error("댓글을 불러오지 못했습니다.");
        return res.json();
      })
      .then((data) => setComments(data))
      .catch((err) => setError(err.message));
  };

  useEffect(() => {
    fetchComments();
  }, [postId]);

  const nestedComments = (parentId: number | null) =>
    comments
      .filter((c) => c.parentId === parentId)
      .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());

  const renderComments = (parentId: number | null = null, depth: number = 0) =>
    nestedComments(parentId).map((comment) => (
      <div key={comment.id} className="mb-2 ml-[${depth * 20}px]">
        <div className="p-2 bg-[#1e1e3f] rounded">
          <div className="text-sm text-gray-300">{comment.content}</div>
          <div className="text-xs text-gray-500 mt-1">
            {comment.author} · {new Date(comment.createdAt).toLocaleString()}
          </div>
        </div>
        <CommentForm parentId={comment.id} postId={Number(postId)} onSubmit={fetchComments} />
        {renderComments(comment.id, depth + 1)}
      </div>
    ));

  if (error) return <div className="text-red-500 mt-6">{error}</div>;

  return (
    <div className="mt-8">
      <h2 className="text-lg font-bold mb-2">💬 댓글</h2>
      <CommentForm parentId={null} postId={Number(postId)} onSubmit={fetchComments} />
      <div className="mt-4">{renderComments()}</div>
    </div>
  );
}
