import { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { Link } from 'react-router-dom';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  likeCount: number;
  blinded: boolean;
}

export default function AdminBlindedPostsPage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchBlinded = async () => {
    try {
      const res = await axios.get('/admin/posts/blinded');
      setPosts(res.data.data);
    } catch (err) {
      console.error('블라인드 게시글 조회 실패', err);
    } finally {
      setLoading(false);
    }
  };

  const handleUnblind = async (id: number) => {
    if (!confirm('블라인드를 해제하시겠습니까?')) return;
    try {
      await axios.patch(`/admin/posts/${id}/blind`);
      fetchBlinded();
    } catch {
      alert('해제 실패');
    }
  };

  useEffect(() => {
    fetchBlinded();
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-5xl mx-auto">
        <h2 className="text-3xl font-bold mb-8 drop-shadow-glow text-center">🚫 블라인드 게시글 관리</h2>

        {loading ? (
          <p className="text-center text-gray-400">로딩 중...</p>
        ) : posts.length === 0 ? (
          <p className="text-center text-gray-400">블라인드된 게시글이 없습니다.</p>
        ) : (
          <ul className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            {posts.map((post) => (
              <li
                key={post.id}
                className="bg-[#1f2336]/80 backdrop-blur-md p-5 rounded-xl shadow hover:shadow-red-600 transition-shadow"
              >
                <Link to={`/posts/${post.id}`}>
                  <h3 className="text-lg font-semibold mb-2 text-white">
                    {post.title} <span className="text-red-400 text-sm">(블라인드)</span>
                  </h3>
                  <p className="text-sm text-gray-300 mb-2 line-clamp-3">{post.content}</p>
                  <div className="text-sm text-gray-400 mb-3">
                    ✍ {post.writer} · 🗂 {post.category} · ❤️ {post.likeCount}
                  </div>
                </Link>
                <button
                  onClick={() => handleUnblind(post.id)}
                  className="w-full mt-2 bg-green-500 hover:bg-green-600 text-black py-1 rounded text-sm"
                >
                  ✅ 블라인드 해제
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
