import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { fetchPostDetail } from "@/api/postApi";
import { fetchComments, submitComment } from "@/api/commentApi";
import CommentList from "@/components/comments/CommentList";
import { PostDetail } from "@/types/post";
import { Comment } from "@/types/comment";

export default function PostDetailPage() {
  const { id } = useParams();
  const postId = Number(id);

  const [post, setPost] = useState<PostDetail | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [newComment, setNewComment] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!postId) return;
    fetchPost();
    loadComments();
  }, [postId]);

  const fetchPost = async () => {
    try {
      const data = await fetchPostDetail(postId);
      setPost(data);
    } catch (err) {
      console.error("게시글 불러오기 실패", err);
      alert("게시글 조회에 실패했습니다.");
    }
  };

  const loadComments = async () => {
    try {
      setLoading(true);
      const data = await fetchComments(postId);
      setComments(data);
    } catch (err) {
      console.error("댓글 불러오기 실패", err);
      alert("댓글 불러오기에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleNewComment = async () => {
    if (!newComment.trim()) return;
    try {
      await submitComment({ postId, content: newComment });
      setComments(prev => [...prev, {
        postId,
        content: newComment,
        id: Date.now(),
        createdAt: new Date().toISOString(),
        author: "me",
      }]);
      setNewComment("");
    } catch (err) {
      console.error("댓글 등록 실패", err);
      alert("댓글 등록에 실패했습니다.");
    }
  };

  const handleReplySubmit = async (content: string, parentId: number) => {
    if (!content.trim()) return;
    try {
      await submitComment({ postId, content, parentId });
      await loadComments();
    } catch (err) {
      console.error("대댓글 등록 실패", err);
      alert("대댓글 등록에 실패했습니다.");
    }
  };

  return (
    <div className="p-6">
      {/* ✅ 게시글 내용 영역 */}
      {post && (
        <div className="mb-8">
          <h1 className="text-3xl font-bold mb-2">{post.title}</h1>
          <div className="text-sm text-gray-600 mb-4">
            {post.writer} · {new Date(post.createdAt).toLocaleString()}
          </div>
          <div className="border p-4 rounded bg-gray-50 whitespace-pre-wrap">{post.content}</div>
        </div>
      )}

      {/* ✅ 댓글 영역 */}
      <h2 className="text-xl font-bold mb-2">댓글</h2>
      <textarea
        value={newComment}
        onChange={(e) => setNewComment(e.target.value)}
        className="w-full border p-2 mb-2"
        placeholder="댓글을 입력하세요"
      />
      <button
        onClick={handleNewComment}
        className="bg-blue-500 text-white px-4 py-1 rounded mb-4"
      >
        댓글 등록
      </button>

      {loading ? (
        <p>댓글 불러오는 중...</p>
      ) : (
        <CommentList comments={comments} onReplySubmit={handleReplySubmit} />
      )}
    </div>
  );
}
