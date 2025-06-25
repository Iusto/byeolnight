import { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { Link } from 'react-router-dom';
import ChatSidebar from '../components/ChatSidebar';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  likeCount: number;
  blinded: boolean;
}

export default function Home() {
  const [posts, setPosts] = useState<Post[]>([]);

  useEffect(() => {
    axios
      .get('/api/public/posts', { params: { category: 'NEWS', sort: 'popular' } })
      .then((res) => setPosts(res.data.data.content))
      .catch((err) => console.error('인기 게시글 불러오기 실패', err));
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-10 px-4">
      <div className="max-w-7xl mx-auto flex flex-col lg:flex-row gap-6">
        {/* 메인 콘텐츠 */}
        <div className="flex-1">
          <h2 className="text-2xl font-bold mb-6 drop-shadow-glow">🔥 인기 게시글</h2>
          <ul className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {posts.map((post) => (
              <li
                key={post.id}
                className="bg-[#1f2336]/80 backdrop-blur-md p-4 rounded-xl shadow hover:shadow-purple-600 transition"
              >
                <Link to={`/posts/${post.id}`}>
                  <h3 className="text-lg font-semibold mb-1">{post.title}</h3>
                  <p className="text-sm text-gray-300 line-clamp-2">{post.content}</p>
                  <div className="text-xs text-gray-400 mt-2">
                    ✍ {post.writer} · ❤️ {post.likeCount}
                  </div>
                </Link>
              </li>
            ))}
          </ul>

          {/* 추후 기능 Placeholder */}
          <div className="mt-12 space-y-8">
            <div className="text-xl font-semibold text-starlight">🌌 밤하늘 별 사진</div>
            <div className="text-gray-400">[ 향후 이미지 게시판 연동 예정 ]</div>

            <div className="text-xl font-semibold text-starlight">🪐 천문대 견학 일정 안내</div>
            <div className="text-gray-400">[ 향후 크롤링 게시판 연동 예정 ]</div>

            <div className="text-xl font-semibold text-starlight">🚀 우주 뉴스</div>
            <div className="text-gray-400">[ 향후 뉴스 API 또는 크롤링 연동 예정 ]</div>
          </div>
        </div>

        {/* 채팅 사이드바 */}
        <div className="w-full lg:w-72">
          <ChatSidebar />
        </div>
      </div>
    </div>
  );
}
