import { useState } from "react";
import { useAuth } from "../contexts/AuthContext";

type Props = {
  postId: number;
  parentId: number | null;
  onSubmit: () => void;
};

export default function CommentForm({ postId, parentId, onSubmit }: Props) {
  const { user } = useAuth();

  if (!user) return null;

  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) return;
    
    if (content.length > 2000) {
      setError("댓글은 2000자를 초과할 수 없습니다.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const res = await fetch("/api/member/comments", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
        },
        body: JSON.stringify({ postId, parentId, content }),
      });

      if (!res.ok) {
        const errorData = await res.json();
        throw new Error(errorData.message || "댓글 작성 실패");
      }

      setContent("");
      onSubmit(); // reload comment list
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="mt-2 flex flex-col gap-2">
      <div className="relative">
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={2}
          maxLength={2000}
          className="w-full p-2 rounded bg-[#1a1a2e] text-sm"
          placeholder={parentId ? "대댓글을 입력하세요..." : "댓글을 입력하세요..."}
        />
        <div className={`text-xs mt-1 ${content.length > 1800 ? 'text-red-400' : 'text-gray-400'}`}>
          {content.length}/2000
        </div>
      </div>
      {error && <div className="text-red-500 text-sm">{error}</div>}
      <button
        type="submit"
        disabled={loading}
        className="self-end bg-indigo-600 hover:bg-indigo-700 text-white text-sm px-3 py-1 rounded"
      >
        {loading ? "작성 중..." : "작성"}
      </button>
    </form>
  );
}
