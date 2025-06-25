import { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { useAuth } from '../contexts/AuthContext';
import { Link } from 'react-router-dom';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  blinded: boolean;
  likeCount: number;
}

export default function MyPage() {
  const { user } = useAuth();
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchMyPosts = async () => {
    try {
      const res = await axios.get('/member/posts/mine');
      setPosts(res.data.data.content || res.data.data); // Page or List
    } catch (err) {
      console.error('내 게시글 조회 실패', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMyPosts();
  }, []);

  if (!user) return <div className="text-white p-8">로그인이 필요합니다.</div>;

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6">
      <div className="max-w-5xl mx-auto">
        <h2 className="text-3xl font-bold mb-8 drop-shadow-glow text-center">🌠 내 활동</h2>

        {loading ? (
          <p className="text-center text-gray-400">불러오는 중...</p>
        ) : posts.length === 0 ? (
          <p className="text-center text-gray-400">작성한 게시글이 없습니다.</p>
        ) : (
          <ul className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            {posts.map((post) => (
              <li
                key={post.id}
                className="bg-[#1f2336]/80 backdrop-blur-md p-5 rounded-xl shadow hover:shadow-purple-700 transition-shadow"
              >
                <Link to={`/posts/${post.id}`} className="block h-full">
                  <h3 className="text-lg font-semibold text-white mb-2">
                    {post.title}{' '}
                    {post.blinded && <span className="text-red-400 text-sm">(블라인드)</span>}
                  </h3>
                  <p className="text-sm text-gray-300 line-clamp-3 mb-2">{post.content}</p>
                  <div className="text-sm text-gray-400">
                    🗂 {post.category} · ❤️ {post.likeCount}
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
