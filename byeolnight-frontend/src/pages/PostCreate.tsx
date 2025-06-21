import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

type Category = "NEWS" | "DISCUSSION" | "IMAGE";

export default function PostCreate() {
  const { user } = useAuth();
  const navigate = useNavigate();

  if (!user) {
    navigate('/login');
    return null;
  }

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [category, setCategory] = useState<Category>("NEWS");
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      const res = await fetch("/api/member/posts", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
        },
        body: JSON.stringify({ title, content, category }),
      });

      if (!res.ok) {
        throw new Error("게시글 작성에 실패했습니다.");
      }

      const data = await res.json();
      navigate(`/posts/${data.id}`);
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold mb-4">📝 게시글 작성</h1>
      {error && <div className="text-red-500 mb-4">{error}</div>}
      <form onSubmit={handleSubmit} className="grid gap-4">
        <div>
          <label className="block mb-1">카테고리</label>
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value as Category)}
            className="w-full p-2 bg-[#1b1b3a] rounded"
          >
            <option value="NEWS">뉴스</option>
            <option value="DISCUSSION">토론</option>
            <option value="IMAGE">이미지</option>
          </select>
        </div>
        <div>
          <label className="block mb-1">제목</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            className="w-full p-2 rounded bg-[#1b1b3a]"
          />
        </div>
        <div>
          <label className="block mb-1">내용</label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            required
            rows={8}
            className="w-full p-2 rounded bg-[#1b1b3a]"
          />
        </div>
        <button
          type="submit"
          className="bg-purple-600 hover:bg-purple-700 text-white py-2 rounded"
        >
          작성 완료
        </button>
      </form>
    </div>
  );
}
