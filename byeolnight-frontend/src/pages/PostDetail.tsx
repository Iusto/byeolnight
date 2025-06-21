import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import CommentList from "../components/CommentList";

type Post = {
  id: number;
  title: string;
  content: string;
  author: string;
  createdAt: string;
  category: "NEWS" | "DISCUSSION" | "IMAGE";
};

export default function PostDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [post, setPost] = useState<Post | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    fetch(`/api/public/posts/${id}`)
      .then((res) => {
        if (!res.ok) throw new Error("게시글을 불러오지 못했습니다.");
        return res.json();
      })
      .then((data) => setPost(data))
      .catch((err) => setError(err.message));
  }, [id]);

  const handleDelete = async () => {
    if (!confirm("정말 삭제하시겠습니까?")) return;
    try {
      const res = await fetch(`/api/member/posts/${id}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
        },
      });
      if (!res.ok) throw new Error("삭제 실패");
      navigate("/posts");
    } catch (err: any) {
      alert(err.message);
    }
  };

  if (error) return <div className="text-red-500 mt-10 text-center">{error}</div>;
  if (!post) return <div className="text-center mt-10">로딩 중...</div>;

  const isAuthor = user?.nickname === post.author;

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <div className="flex justify-between items-center mb-2">
        <span className="text-sm text-purple-400">{post.category}</span>
        <span className="text-xs text-gray-400">
          {new Date(post.createdAt).toLocaleString()}
        </span>
      </div>
      <h1 className="text-2xl font-bold mb-2">{post.title}</h1>
      <p className="text-sm text-gray-300 mb-6">by {post.author}</p>
      <div className="whitespace-pre-line mb-8">{post.content}</div>

      {isAuthor && (
        <div className="flex gap-2">
          <button
            onClick={() => navigate(`/posts/edit/${post.id}`)}
            className="bg-blue-600 hover:bg-blue-700 text-white py-2 px-4 rounded"
          >
            수정
          </button>
          <button
            onClick={handleDelete}
            className="bg-red-600 hover:bg-red-700 text-white py-2 px-4 rounded"
          >
            삭제
          </button>
        </div>
      )}

      <CommentList />
    </div>
  );
}
