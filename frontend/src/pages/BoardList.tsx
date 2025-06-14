import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import axiosInstance from "@lib/axiosInstance"

type Post = {
  id: number;
  title: string;
  writer: string;
  createdAt: string;
};

const BoardList = () => {
  const [posts, setPosts] = useState<Post[]>([]);

  useEffect(() => {
    const fetchPosts = async () => {
      try {
        const res = await axiosInstance.get("/public/posts", {
          params: {
            page: 0,
            size: 20,
          },
        });

        console.log("✅ 응답 확인:", res.data); // 디버깅용
        setPosts(res.data.data.content);
      } catch (err) {
        console.error("💥 게시글 조회 실패:", err);
      }
    };

    fetchPosts();
  }, []);

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-4">게시판</h1>

      <Link to="/board/write" className="bg-blue-500 text-white px-4 py-2 rounded">
        글쓰기
      </Link>

      <div className="mt-6 space-y-4">
        {posts.length === 0 ? (
          <p className="text-gray-500">게시글이 없습니다.</p>
        ) : (
          posts.map((post) => (
            <Link key={post.id} to={`/board/${post.id}`} className="block border-b pb-2">
              <div className="font-semibold">{post.title}</div>
              <div className="text-sm text-gray-500">
                {post.writer} · {new Date(post.createdAt).toLocaleDateString()}
              </div>
            </Link>
          ))
        )}
      </div>
    </div>
  );
};

export default BoardList;
