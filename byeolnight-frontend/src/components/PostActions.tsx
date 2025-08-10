import { useEffect, useState } from "react";
import { useAuth } from "../contexts/AuthContext";

type Props = {
  postId: number;
};

export default function PostActions({ postId }: Props) {
  const { user } = useAuth();

  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [reported, setReported] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    fetch(`/api/public/posts/${postId}/likes`)
      .then((res) => res.json())
      .then((data) => {
        setLikeCount(data.count);
        setLiked(data.liked);
      });
  }, [postId]);

  if (!user) return null;

  const handleLike = async () => {
    const method = liked ? "DELETE" : "POST";

    try {
      const res = await fetch(`/api/member/posts/${postId}/like`, {
        method,
        headers: {
          Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
        },
      });

      if (!res.ok) throw new Error("추천 처리 실패");

      setLiked(!liked);
      setLikeCount((prev) => (liked ? prev - 1 : prev + 1));
    } catch (err: any) {
      setError(err.message);
    }
  };

  const handleReport = async () => {
    if (!confirm("해당 게시글을 신고하시겠습니까?")) return;
    try {
      const res = await fetch(`/api/member/posts/${postId}/report`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
        },
      });
      if (!res.ok) throw new Error("신고 실패");

      setReported(true);
      alert("신고가 접수되었습니다.");
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <div className="my-6 flex items-center gap-4">
      <button
        onClick={handleLike}
        className={`flex items-center gap-1 px-3 py-1 rounded text-sm font-medium
          ${liked ? "bg-yellow-600" : "bg-gray-700 hover:bg-gray-600"} text-white`}
      >
        👍 {liked ? "추천 취소" : "추천"} ({likeCount})
      </button>
      <button
        onClick={handleReport}
        disabled={reported}
        className="px-3 py-1 rounded bg-red-700 hover:bg-red-800 text-sm text-white disabled:opacity-50"
      >
        🚩 신고
      </button>
      {error && <div className="text-red-500 text-sm">{error}</div>}
    </div>
  );
}
