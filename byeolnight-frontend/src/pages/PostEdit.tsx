import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

type Category = "NEWS" | "DISCUSSION" | "IMAGE";

export default function PostEdit() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [category, setCategory] = useState<Category>("NEWS");
  const [author, setAuthor] = useState("");
  const [error, setError] = useState("");

  if (!user) {
    navigate('/login');
    return null;
  }

  useEffect(() => {
    fetch(`/api/public/posts/${id}`)
      .then((res) => {
        if (!res.ok) throw new Error("게시글 정보를 불러올 수 없습니다.");
        return res.json();
      })
      .then((data) => {
        setTitle(data.title);
        setContent(data.content);
        setCategory(data.category);
        setAuthor(data.author);
      })
      .catch((err) => setError(err.message));
  }, [id]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      const res = await fetch(`/api/member/posts/${id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
        },
        body: JSON.stringify({ title, content, category }),
      });

      if (!res.ok) throw new Error("수정에 실패했습니다.");

      navigate(`/posts/${id}`);
    } catch (err: any) {
      setError(err.message);
    }
  };

  if (user.nickname !== author) {
    return <div className="text-center text-red-500 mt-10">본인만 수정할 수 있습니다.</div>;
  }

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold mb-4">✏️ 게시글 수정</h1>
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
          className="bg-blue-600 hover:bg-blue-700 text-white py-2 rounded"
        >
          수정 완료
        </button>
      </form>
    </div>
  );
}
