import { useState } from "react"
import { useNavigate } from "react-router-dom"
import axiosInstance from "@/lib/axiosInstance"

export default function PostCreate() {
  const [title, setTitle] = useState("")
  const [content, setContent] = useState("")
  const navigate = useNavigate()

  const fetchPosts = async () => {
    try {
      const res = await fetch("/api/posts");
      const contentType = res.headers.get("content-type");

      if (!res.ok) {
        const text = await res.text();
        console.error("❌ 서버 오류 응답 본문:", text);
        throw new Error("서버 오류: 게시글 목록을 가져올 수 없습니다.");
      }

      if (contentType && contentType.includes("application/json")) {
        const data = await res.json();
        setPosts(data);
      } else {
        const text = await res.text();
        console.error("❌ 응답이 JSON이 아님:", text);
      }
    } catch (err) {
      console.error("💥 게시글 불러오기 실패:", err);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await axiosInstance.post("/posts", { title, content, category })
      alert("게시글이 등록되었습니다.")
      navigate("/board")
    } catch (err: any) {
      alert(err.response?.data?.message || "글쓰기 실패")
    }
  }

  const [category, setCategory] = useState("NEWS") // 기본값 설정

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {/* 제목 */}
      <input
        type="text"
        placeholder="제목을 입력하세요"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        className="w-full border p-2 rounded"
        required
      />

      {/* 카테고리 선택 */}
      <select
        value={category}
        onChange={(e) => setCategory(e.target.value)}
        className="w-full border p-2 rounded"
        required
      >
        <option value="NEWS">뉴스</option>
        <option value="DISCUSSION">토론</option>
        <option value="IMAGE">이미지</option>
      </select>

      {/* 내용 */}
      <textarea
        placeholder="내용을 입력하세요"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        className="w-full border p-2 rounded h-40"
        required
      />

      {/* 버튼 */}
      <div className="flex justify-end gap-4">
        <button type="submit" className="bg-blue-600 text-white px-4 py-2 rounded">
          등록
        </button>
        <button type="button" onClick={() => navigate("/board")} className="bg-gray-300 px-4 py-2 rounded">
          취소
        </button>
      </div>
    </form>
  )
}
