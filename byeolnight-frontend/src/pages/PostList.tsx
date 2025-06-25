import { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { Link, useSearchParams } from 'react-router-dom';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  blinded: boolean;
  likeCount: number;
  likedByMe: boolean;
}

const CATEGORY_LABELS: Record<string, string> = {
  NEWS: '뉴스',
  DISCUSSION: '토론',
  IMAGE: '사진',
  EVENT: '행사',
  REVIEW: '후기',
};

export default function PostList() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchParams, setSearchParams] = useSearchParams();
  const category = searchParams.get('category') || 'NEWS';
  const sort = searchParams.get('sort') || 'recent';

  useEffect(() => {
    const fetchPosts = async () => {
      setLoading(true);
      try {
        const res = await axios.get('/public/posts', {
          params: { category, sort },
        });
        setPosts(res.data.data.content);
      } catch (err) {
        console.error('게시글 목록 조회 실패', err);
      } finally {
        setLoading(false);
      }
    };

    fetchPosts();
  }, [category, sort]);

  const handleCategoryChange = (cat: string) => {
    setSearchParams({ category: cat, sort });
  };

  const handleSortChange = (s: string) => {
    setSearchParams({ category, sort: s });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6 font-sans">
      <div className="max-w-5xl mx-auto">
        <h2 className="text-4xl font-bold mb-8 text-center text-white drop-shadow-glow">🌌 게시판</h2>

        {/* 카테고리 필터 */}
        <div className="flex flex-wrap gap-2 justify-center mb-6">
          {Object.keys(CATEGORY_LABELS).map((cat) => (
            <button
              key={cat}
              onClick={() => handleCategoryChange(cat)}
              className={`px-4 py-2 rounded-full text-sm font-medium transition ${
                category === cat
                  ? 'bg-purple-600 text-white shadow-lg'
                  : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
              }`}
            >
              {CATEGORY_LABELS[cat]}
            </button>
          ))}
        </div>

        {/* 정렬 */}
        <div className="flex justify-end mb-4">
          <label className="mr-2 text-sm text-gray-400">정렬:</label>
          <select
            value={sort}
            onChange={(e) => handleSortChange(e.target.value)}
            className="bg-[#2a2e45] text-sm rounded px-3 py-1"
          >
            <option value="recent">최신순</option>
            <option value="popular">추천순</option>
          </select>
        </div>

        {/* 게시글 목록 */}
        {loading ? (
          <p className="text-center text-gray-400">🌠 로딩 중...</p>
        ) : posts.length === 0 ? (
          <p className="text-center text-gray-400">게시글이 없습니다.</p>
        ) : (
          <ul className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            {posts.map((post) => (
              <li
                key={post.id}
                className="bg-[#1f2336]/80 backdrop-blur-md border border-gray-700 rounded-xl p-5 shadow-xl hover:shadow-purple-700 transition-shadow"
              >
                <Link to={`/posts/${post.id}`} className="block h-full">
                  <h3 className="text-xl font-semibold mb-2 text-white">
                    {post.title}{' '}
                    {post.blinded && <span className="text-red-400 text-sm">(블라인드)</span>}
                  </h3>
                  <p className="text-sm text-gray-300 mb-3 line-clamp-3">{post.content}</p>
                  <div className="flex justify-between items-center text-sm text-gray-400 mt-auto">
                    <span>✍ {post.writer}</span>
                    <span>❤️ {post.likeCount}</span>
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
