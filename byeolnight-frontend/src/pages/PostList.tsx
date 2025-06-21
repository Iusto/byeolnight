import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

type Post = {
  id: number;
  title: string;
  author: string;
  createdAt: string;
  category: "NEWS" | "DISCUSSION" | "IMAGE";
};

type PostPage = {
  content: Post[];
  totalPages: number;
  number: number;
};

export default function PostList() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PostPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    setLoading(true);
    fetch(`/api/public/posts?page=${page}&size=10`)
      .then((res) => {
        if (!res.ok) throw new Error("게시글 목록을 불러오지 못했습니다.");
        return res.json();
      })
      .then((json) => setData(json))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [page]);

  if (loading) return <div className="text-center mt-10">로딩 중...</div>;
  if (error) return <div className="text-red-500 text-center mt-10">{error}</div>;
  if (!data) return null;

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-4">📚 게시글 목록</h1>
      <div className="grid gap-4 mb-6">
        {data.content.map((post) => (
          <Link
            to={`/posts/${post.id}`}
            key={post.id}
            className="block p-4 border rounded-xl bg-[#1b1b3a] hover:bg-[#2a2a50]"
          >
            <div className="flex justify-between items-center">
              <span className="text-sm text-purple-400">{post.category}</span>
              <span className="text-xs text-gray-400">
                {new Date(post.createdAt).toLocaleDateString()}
              </span>
            </div>
            <h2 className="text-lg font-semibold mt-1">{post.title}</h2>
            <p className="text-sm text-gray-300 mt-1">by {post.author}</p>
          </Link>
        ))}
      </div>
      <div className="flex justify-center gap-2">
        <button
          disabled={page === 0}
          onClick={() => setPage((prev) => prev - 1)}
          className="px-4 py-1 bg-gray-600 hover:bg-gray-700 text-white rounded disabled:opacity-50"
        >
          이전
        </button>
        <span className="text-white px-2 pt-1">Page {data.number + 1}</span>
        <button
          disabled={page >= data.totalPages - 1}
          onClick={() => setPage((prev) => prev + 1)}
          className="px-4 py-1 bg-gray-600 hover:bg-gray-700 text-white rounded disabled:opacity-50"
        >
          다음
        </button>
      </div>
    </div>
  );
}
