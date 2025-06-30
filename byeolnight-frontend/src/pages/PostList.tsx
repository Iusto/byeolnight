// PostList.tsx
import { useEffect, useState } from 'react';
import axios from '../lib/axios';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

interface Post {
  id: number;
  title: string;
  content: string;
  category: string;
  writer: string;
  blinded: boolean;
  likeCount: number;
  likedByMe: boolean;
  hot: boolean;
  dDay?: string;
}

const CATEGORY_LABELS: Record<string, string> = {
  NEWS: '뉴스',
  DISCUSSION: '토론',
  IMAGE: '사진',
  EVENT: '우주전시회',
  REVIEW: '후기',
  FREE: '자유',
  NOTICE: '공지',
};

const RESTRICTED_CATEGORIES = ['NEWS', 'EVENT', 'NOTICE'];

export default function PostList() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchParams, setSearchParams] = useSearchParams();
  const { user } = useAuth();

  const category = searchParams.get('category') || 'NEWS';
  const sort = searchParams.get('sort') || 'recent';
  const page = Number(searchParams.get('page')) || 0;

  useEffect(() => {
    const fetchPosts = async () => {
      setLoading(true);
      try {
        const res = await axios.get('/public/posts', {
          params: { category, sort, page, size: 30 },
        });
        // 응답 구조 안전하게 처리
        const responseData = res.data?.data || res.data;
        const content = responseData?.content || [];
        setPosts(content);
      } catch (err) {
        console.error('게시글 목록 조회 실패', err);
        setPosts([]); // 오류 시 빈 배열로 설정
      } finally {
        setLoading(false);
      }
    };

    fetchPosts();
  }, [category, sort, page]);

  const handleCategoryChange = (cat: string) => {
    setSearchParams({ category: cat, sort, page: '0' });
  };

  const handleSortChange = (s: string) => {
    setSearchParams({ category, sort: s, page: '0' });
  };

  const handlePageChange = (nextPage: number) => {
    setSearchParams({ category, sort, page: String(nextPage) });
  };

  const canWrite = user && (!RESTRICTED_CATEGORIES.includes(category) || user.role === 'ADMIN');
  const hotPosts = posts.filter((p) => p.hot).slice(0, 4);
  const normalPosts = sort === 'popular' ? posts.slice(0, 30) : posts.filter((p) => !p.hot).slice(0, 25);

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6 font-sans">
      <div className="max-w-5xl mx-auto">
        <h2 className="text-4xl font-bold mb-8 text-center text-white drop-shadow-glow">🪐 게시판</h2>

        {/* 카테고리 선택 */}
        <div className="flex flex-wrap gap-2 justify-center mb-6">
          {Object.keys(CATEGORY_LABELS).map((cat) => (
            <button
              key={cat}
              onClick={() => handleCategoryChange(cat)}
              className={`px-4 py-2 rounded-full text-base font-medium transition ${
                category === cat ? 'bg-purple-600 text-white shadow-lg' : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
              }`}
            >
              {CATEGORY_LABELS[cat]}
            </button>
          ))}
        </div>

        {/* 뉴스 카테고리 자동 업데이트 안내 */}
        {category === 'NEWS' && (
          <div className="mb-6 p-4 bg-blue-900/30 rounded-lg border border-blue-600/30">
            <div className="flex items-center gap-3 mb-2">
              <span className="text-2xl">🤖</span>
              <h3 className="text-lg font-semibold text-blue-200">자동 뉴스 업데이트</h3>
            </div>
            <p className="text-blue-200 text-sm leading-relaxed">
              <strong>뉴스봇</strong>이 매일 <strong>오전 6시</strong>와 <strong>오후 12시</strong>에 우주 뉴스를 자동 수집합니다.
              <br />
              <strong>대상 출처:</strong> 사이언스타임즈, 한국천문연구원, 동아사이언스, 국립과천과학관
              <br />
              매번 새 게시글로 등록되며, 중복 가능성은 낮습니다.
            </p>
          </div>
        )}
        
        {/* 우주 전시회 카테고리 자동 업데이트 안내 */}
        {category === 'EVENT' && (
          <div className="mb-6 p-4 bg-orange-900/30 rounded-lg border border-orange-600/30">
            <div className="flex items-center gap-3 mb-2">
              <span className="text-2xl">🤖</span>
              <h3 className="text-lg font-semibold text-orange-200">자동 우주전시회 업데이트</h3>
            </div>
            <p className="text-orange-200 text-sm leading-relaxed">
              <strong>우주전시회봇</strong>이 매일 <strong>오전 7시</strong>에 전국 우주 관련 전시회 정보를 자동 수집합니다.
              <br />
              <strong>대상 기관:</strong> 국립과천과학관, 국립중앙과학관, 서울시립과학관, 과학기술정보통신부 등
              <br />
              동일 제목+기간이면 중복 등록 방지, 전체 삭제 후 재등록 가능
            </p>
          </div>
        )}

        {/* 정렬 및 글쓰기 */}
        <div className="flex justify-between items-center mb-6">
          <div className="flex items-center gap-2">
            <label className="text-base text-gray-300">정렬:</label>
            <select
              value={sort}
              onChange={(e) => handleSortChange(e.target.value)}
              className="bg-[#2a2e45] text-sm rounded px-3 py-1"
            >
              <option value="recent">최신순</option>
              <option value="popular">추천순</option>
            </select>
          </div>
          {canWrite && (
            <Link
              to={`/posts/write?category=${category}`}
              className="px-6 py-2 bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white font-semibold rounded-lg shadow-lg hover:shadow-xl transition-all duration-200 transform hover:scale-105"
            >
              ✍️ 글쓰기
            </Link>
          )}
        </div>

        {loading ? (
          <p className="text-center text-gray-400">🌠 로딩 중...</p>
        ) : posts.length === 0 ? (
          <p className="text-center text-gray-400">게시글이 없습니다.</p>
        ) : (
          <>
            {/* 인기 게시글 */}
            {sort === 'recent' && hotPosts.length > 0 && (
              <>
                <h3 className="text-2xl font-semibold mb-4 text-orange-400">🔥 인기 게시글</h3>
                <ul className="grid grid-cols-1 sm:grid-cols-2 gap-6 mb-8">
                  {hotPosts.map((post) => (
                    <li
                      key={post.id}
                      className="bg-[#1f2336]/80 border border-gray-700 rounded-xl p-5 shadow-xl hover:shadow-purple-700 transition-shadow"
                    >
                      <Link to={`/posts/${post.id}`} className="block h-full">
                        <h3 className="text-lg font-bold mb-1 text-white">
                          {post.dDay && <span className="text-orange-300 text-sm mr-2">[{post.dDay}]</span>}
                          🔥 {post.title}{' '}
                          {post.blinded && <span className="text-red-400 text-sm">(블라인드)</span>}
                        </h3>
                        <p className="text-sm text-gray-300 mb-2 line-clamp-2">{post.content}</p>
                        <div className="flex justify-between text-sm text-gray-400">
                          <span>✍ {post.writer}</span>
                          <span>❤️ {post.likeCount}</span>
                        </div>
                      </Link>
                    </li>
                  ))}
                </ul>
              </>
            )}

            {/* 일반 게시글 */}
            <h3 className="text-2xl font-semibold mb-4 text-white">
              {sort === 'popular' ? '📄 게시글 (추천순)' : '📄 일반 게시글'}
            </h3>
            <ul className="space-y-4">
              {normalPosts.map((post) => (
                <li
                  key={post.id}
                  className="bg-[#1f2336]/80 border border-gray-700 rounded-lg px-4 py-3 hover:shadow-lg transition"
                >
                  <Link to={`/posts/${post.id}`} className="block">
                    <div className="flex justify-between items-center">
                      <h4 className="text-base font-semibold text-white">
                        {post.dDay && <span className="text-orange-300 text-sm mr-2">[{post.dDay}]</span>}
                        {post.title}{' '}
                        {post.blinded && <span className="text-red-400 text-sm">(블라인드)</span>}
                      </h4>
                      <span className="text-sm text-gray-400">❤️ {post.likeCount}</span>
                    </div>
                    <p className="text-sm text-gray-400 mt-1">✍ {post.writer}</p>
                  </Link>
                </li>
              ))}
            </ul>

            {/* 페이지네이션 */}
            <div className="mt-10 flex justify-center gap-2">
              {page > 0 && (
                <button
                  onClick={() => handlePageChange(page - 1)}
                  className="px-3 py-1 bg-gray-600 rounded hover:bg-gray-500"
                >
                  이전
                </button>
              )}
              <span className="px-3 py-1 bg-gray-800 rounded text-white">Page {page + 1}</span>
              {posts.length >= 30 && (
                <button
                  onClick={() => handlePageChange(page + 1)}
                  className="px-3 py-1 bg-gray-600 rounded hover:bg-gray-500"
                >
                  다음
                </button>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
