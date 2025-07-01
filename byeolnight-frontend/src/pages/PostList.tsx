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
  commentCount: number;
  createdAt: string;
  updatedAt: string;
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
const USER_WRITABLE_CATEGORIES = ['DISCUSSION', 'IMAGE', 'REVIEW', 'FREE'];

export default function PostList() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchParams, setSearchParams] = useSearchParams();
  const { user } = useAuth();

  const category = searchParams.get('category') || 'NEWS';
  const sort = searchParams.get('sort') || 'recent';
  const page = Number(searchParams.get('page')) || 0;
  const searchType = searchParams.get('searchType') || 'title';
  const searchKeyword = searchParams.get('search') || '';
  
  const [searchInput, setSearchInput] = useState(searchKeyword);
  const [searchTypeInput, setSearchTypeInput] = useState(searchType);

  useEffect(() => {
    const fetchPosts = async () => {
      setLoading(true);
      try {
        const params: any = { category, sort, page, size: 30 };
        if (searchKeyword && searchKeyword.trim()) {
          params.searchType = searchType;
          params.search = searchKeyword.trim();
        }
        
        console.log('검색 요청 파라미터:', params);
        console.log('URL:', '/public/posts?' + new URLSearchParams(params).toString());
        const res = await axios.get('/public/posts', { params });
        // 응답 구조 안전하게 처리
        console.log('전체 응답:', res.data);
        const responseData = res.data?.data || res.data;
        const content = responseData?.content || [];
        console.log('최종 게시글 데이터:', content);
        console.log('게시글 수:', content.length);
        setPosts(content);
      } catch (err) {
        console.error('게시글 목록 조회 실패', err);
        setPosts([]); // 오류 시 빈 배열로 설정
      } finally {
        setLoading(false);
      }
    };

    fetchPosts();
  }, [category, sort, page, searchKeyword, searchType]);

  const handleCategoryChange = (cat: string) => {
    const params: any = { category: cat, sort, page: '0' };
    // 카테고리 변경 시 검색 상태 유지
    if (searchKeyword) {
      params.searchType = searchType;
      params.search = searchKeyword;
    }
    setSearchParams(params);
  };

  const handleSortChange = (s: string) => {
    const params: any = { category, sort: s, page: '0' };
    // 정렬 변경 시 검색 상태 유지
    if (searchKeyword) {
      params.searchType = searchType;
      params.search = searchKeyword;
    }
    setSearchParams(params);
  };

  const handlePageChange = (nextPage: number) => {
    const params: any = { category, sort, page: String(nextPage) };
    if (searchKeyword) {
      params.searchType = searchType;
      params.search = searchKeyword;
    }
    setSearchParams(params);
  };
  
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const params: any = { category, sort, page: '0' };
    if (searchInput.trim()) {
      params.searchType = searchTypeInput;
      params.search = searchInput.trim();
    }
    setSearchParams(params);
  };
  
  const handleSearchReset = () => {
    setSearchInput('');
    setSearchParams({ category, sort, page: '0' });
  };

  const canWrite = user && (USER_WRITABLE_CATEGORIES.includes(category) || user.role === 'ADMIN');
  const hotPosts = posts.filter((p) => p.hot).slice(0, 4);
  const normalPosts = sort === 'popular' ? posts.slice(0, 30) : posts.filter((p) => !p.hot).slice(0, 25);

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#0c0c1f] via-[#1b1e3d] to-[#0c0c1f] text-white py-12 px-6 font-sans">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-8">
          <h2 className="text-4xl font-bold text-white drop-shadow-glow mb-2">🪐 {CATEGORY_LABELS[category]} 게시판</h2>
          <p className="text-gray-400 text-sm">우주에 대한 다양한 이야기를 나눠보세요</p>
        </div>

        {/* 카테고리 선택 */}
        <div className="mb-8 p-6 bg-[#1f2336]/80 backdrop-blur-md rounded-xl shadow-xl">
          <h3 className="text-lg font-semibold mb-4 text-center">📚 게시판 카테고리</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-3">
            {Object.keys(CATEGORY_LABELS).map((cat) => {
              const icons = {
                NEWS: '🚀',
                DISCUSSION: '💬',
                IMAGE: '🌌',
                EVENT: '🏦',
                REVIEW: '⭐',
                FREE: '🎈',
                NOTICE: '📢'
              };
              return (
                <button
                  key={cat}
                  onClick={() => handleCategoryChange(cat)}
                  className={`p-4 rounded-lg text-center transition-all duration-200 ${
                    category === cat 
                      ? 'bg-purple-600/40 border-2 border-purple-400 text-white shadow-lg transform scale-105' 
                      : 'bg-gray-700/50 border-2 border-gray-600/30 text-gray-300 hover:bg-gray-600/50 hover:border-gray-500/50'
                  }`}
                >
                  <div className="text-2xl mb-1">{icons[cat as keyof typeof icons]}</div>
                  <div className="text-sm font-medium">{CATEGORY_LABELS[cat]}</div>
                </button>
              );
            })}
          </div>
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

        {/* 검색 기능 */}
        <div className="mb-6 p-4 bg-[#1f2336]/80 rounded-lg border border-gray-600">
          <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3">
            <select
              value={searchTypeInput}
              onChange={(e) => setSearchTypeInput(e.target.value)}
              className="bg-[#2a2e45] text-white rounded px-3 py-2 text-sm"
            >
              <option value="title">제목</option>
              <option value="content">내용</option>
              <option value="titleAndContent">제목+내용</option>
              <option value="writer">글작성자</option>
            </select>
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="검색어를 입력하세요..."
              className="flex-1 bg-[#2a2e45] text-white rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
            <div className="flex gap-2">
              <button
                type="submit"
                className="px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded text-sm transition"
              >
                🔍 검색
              </button>
              {searchKeyword && (
                <button
                  type="button"
                  onClick={handleSearchReset}
                  className="px-4 py-2 bg-gray-600 hover:bg-gray-700 text-white rounded text-sm transition"
                >
                  초기화
                </button>
              )}
            </div>
          </form>
          {searchKeyword && (
            <div className="mt-2 text-sm text-gray-300">
              검색 결과: "{searchKeyword}" ({searchType === 'titleAndContent' ? '제목+내용' : searchType === 'title' ? '제목' : searchType === 'content' ? '내용' : '글작성자'})
            </div>
          )}
        </div>

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
              to={`/posts/write?fixedCategory=${category}`}
              className="px-6 py-2 bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 text-white font-semibold rounded-lg shadow-lg hover:shadow-xl transition-all duration-200 transform hover:scale-105"
            >
              ✍️ {CATEGORY_LABELS[category]} 글쓰기
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
                      className="bg-gradient-to-br from-[#1f2336]/90 to-[#252842]/90 border border-orange-500/30 rounded-xl p-6 shadow-xl hover:shadow-orange-500/50 transition-all duration-300 transform hover:scale-[1.02]"
                    >
                      <Link to={`/posts/${post.id}`} className="block h-full">
                        <div className="flex items-start justify-between mb-3">
                          <h3 className="text-lg font-bold text-white flex items-center gap-2">
                            <span className="text-orange-400">🔥</span>
                            {post.dDay && <span className="bg-orange-500 text-white px-2 py-1 rounded text-xs">[{post.dDay}]</span>}
                            {post.title}
                            {post.blinded && <span className="text-red-400 text-xs">(블라인드)</span>}
                          </h3>
                          <span className="bg-orange-500/20 text-orange-300 px-2 py-1 rounded text-xs font-bold">HOT</span>
                        </div>
                        <p className="text-sm text-gray-300 mb-4 line-clamp-2 leading-relaxed">{post.content}</p>
                        <div className="flex justify-between items-center text-sm">
                          <span className="text-gray-400">✍ {post.writer}</span>
                          <div className="flex gap-3 text-gray-400">
                            <span className="flex items-center gap-1">💬 {post.commentCount || 0}</span>
                            <span className="flex items-center gap-1">❤️ {post.likeCount}</span>
                            <span className="flex items-center gap-1">📅 {new Date(post.createdAt).toLocaleDateString()}</span>
                          </div>
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
                  className="bg-[#1f2336]/80 border border-gray-600/50 rounded-lg p-4 hover:bg-[#252842]/80 hover:border-purple-500/30 hover:shadow-lg transition-all duration-200"
                >
                  <Link to={`/posts/${post.id}`} className="block">
                    <div className="flex justify-between items-start mb-2">
                      <h4 className="text-base font-semibold text-white flex-1 mr-4">
                        {post.dDay && <span className="bg-orange-500 text-white px-2 py-1 rounded text-xs mr-2">[{post.dDay}]</span>}
                        {post.title}
                        {post.blinded && <span className="text-red-400 text-xs ml-2">(블라인드)</span>}
                      </h4>
                      <div className="flex gap-3 text-sm text-gray-400 flex-shrink-0">
                        <span className="flex items-center gap-1">💬 {post.commentCount || 0}</span>
                        <span className="flex items-center gap-1">❤️ {post.likeCount}</span>
                      </div>
                    </div>
                    <div className="flex justify-between items-center text-sm text-gray-400">
                      <span className="flex items-center gap-1">✍ {post.writer}</span>
                      <span className="flex items-center gap-1">📅 {new Date(post.createdAt).toLocaleDateString()}</span>
                    </div>
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
